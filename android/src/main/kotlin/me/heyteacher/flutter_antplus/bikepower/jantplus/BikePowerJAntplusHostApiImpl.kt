package me.heyteacher.flutter_antplus.bikepower.jantplus

import android.os.Handler
import android.os.Looper
import be.glever.ant.channel.AntChannel
import be.glever.ant.message.AntMessage
import be.glever.ant.message.data.BroadcastDataMessage
import be.glever.antplus.common.datapage.DataPage82BatteryStatus
import be.glever.antplus.power.PowerChannel
import be.glever.antplus.power.datapage.PowerDataPageRegistry
import be.glever.antplus.power.datapage.main.PowerDataPage02GetSetParameters
import be.glever.antplus.power.datapage.main.PowerDataPage10PowerOnly
import be.glever.antplus.power.datapage.main.PowerDataPage13TorqueEffectivenessPedalSmoothness
import io.flutter.plugin.common.BinaryMessenger
import me.heyteacher.flutter_antplus.bikepower.OnBalanceDataListener
import me.heyteacher.flutter_antplus.bikepower.OnBatteryStatusListener
import me.heyteacher.flutter_antplus.bikepower.OnCadenceDataListener
import me.heyteacher.flutter_antplus.bikepower.OnPedalSmoothnessDataListener
import me.heyteacher.flutter_antplus.bikepower.OnPowerDataListener
import me.heyteacher.flutter_antplus.bikepower.OnTorqueEffectivenessDataListener
import me.heyteacher.flutter_antplus.bikepower.pigeons.AntplusBatteryStatus
import me.heyteacher.flutter_antplus.bikepower.pigeons.AntplusPedalSmoothnessData
import me.heyteacher.flutter_antplus.bikepower.pigeons.AntplusTorqueEffectivenessData
import me.heyteacher.flutter_antplus.bikepower.pigeons.BikepowerHostApi
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnBalanceDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnBatteryStatusDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnCadenceDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnPedalSmoothnessDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnPowerDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnTorqueEffectivenessDataStreamHandler
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDeviceState
import me.heyteacher.flutter_antplus.jantplus.AbstractJAntplusHostApiImpl
import me.heyteacher.flutter_antplus.logging.OnLogDataListener
import me.heyteacher.flutter_antplus.logging.pigeons.AntplusLogData
import me.heyteacher.flutter_antplus.logging.pigeons.AntplusLogEvent

/**
 * Android implementation of the [BikepowerHostApi] Pigeon interface with j-antplus Java library
 *
 * Manages scanning, connection, and telemetry subscriptions for an ANT+ Bike Power Meter.
 * Registers Pigeon event channel stream handlers for power, cadence, balance, pedal smoothness,
 * torque effectiveness, and battery status, and forwards all native events to Flutter.
 *
 * @param binaryMessenger the Flutter binary messenger for Pigeon channel registration
 * @param onLogDataListener shared log listener to forward native log events to Flutter
 */
class BikePowerJAntplusHostApiImpl(
    binaryMessenger: BinaryMessenger,
    val onLogDataListener: OnLogDataListener
) : AbstractJAntplusHostApiImpl(binaryMessenger, onLogDataListener), BikepowerHostApi {

    companion object {
        private val TAG: String = BikePowerJAntplusHostApiImpl::class.java.simpleName
    }

    private val powerDataPageRegistry = PowerDataPageRegistry()
    private val onPowerDataListener: OnPowerDataListener = OnPowerDataListener()
    private val onBalanceDataListener: OnBalanceDataListener = OnBalanceDataListener()
    private val onCadenceDataListener: OnCadenceDataListener = OnCadenceDataListener()
    private val onPedalSmoothnessDataListener: OnPedalSmoothnessDataListener =
        OnPedalSmoothnessDataListener()
    private val onTorqueEffectivenessDataListener: OnTorqueEffectivenessDataListener =
        OnTorqueEffectivenessDataListener()
    private val onBatteryStatusListener: OnBatteryStatusListener = OnBatteryStatusListener()
    /**
     * Registers all Pigeon Host APIs and event stream handlers on initialization.
     */
    init {
        BikepowerHostApi.setUp(binaryMessenger, this)
        OnPowerDataStreamHandler.register(binaryMessenger, onPowerDataListener)
        OnPedalSmoothnessDataStreamHandler.register(binaryMessenger, onPedalSmoothnessDataListener)
        OnBatteryStatusDataStreamHandler.register(binaryMessenger, onBatteryStatusListener)
        OnBalanceDataStreamHandler.register(binaryMessenger, onBalanceDataListener)
        OnCadenceDataStreamHandler.register(binaryMessenger, onCadenceDataListener)
        OnTorqueEffectivenessDataStreamHandler.register(
            binaryMessenger, onTorqueEffectivenessDataListener
        )
    }

    override fun deviceChannel(): AntChannel {
        return PowerChannel(getAntUsbDevice())
    }

    override fun handleConnectedMessage(antMessage:AntMessage ) {
        try {
            Handler(Looper.getMainLooper()).post {
                onDeviceStateChangeListener.add(AntplusDeviceState.TRACKING)
            }
            if (antMessage is BroadcastDataMessage) {
                val payLoad = antMessage.payLoad
                removeToggleBit(payLoad)
                val dataPage = powerDataPageRegistry.constructDataPage(payLoad) ?: return
                when (dataPage) {
                    is PowerDataPage10PowerOnly  -> {
                        Handler(Looper.getMainLooper()).post {
                            onPowerDataListener.add(dataPage.instantaneousPower.toLong())
                            onCadenceDataListener.add(dataPage.instantaneousCadence.toLong())
                            onBalanceDataListener.add(dataPage.pedalPowerDistribution.percentage.toLong())
                        }
                    }
                    is PowerDataPage13TorqueEffectivenessPedalSmoothness -> {
                        Handler(Looper.getMainLooper()).post {
                            onTorqueEffectivenessDataListener.add(
                                AntplusTorqueEffectivenessData(
                                    dataPage.leftTorqueEffectiveness.toDouble(),
                                    dataPage.rightTorqueEffectiveness.toDouble()
                                )
                            )
                            onPedalSmoothnessDataListener.add(
                                AntplusPedalSmoothnessData(
                                    dataPage.combinedPedalSmoothness == 0,
                                    dataPage.leftPedalSmoothness.toDouble(),
                                    dataPage.rightPedalSmoothness.toDouble()
                                )
                            )
                        }
                    }
                    is DataPage82BatteryStatus -> {
                        Handler(Looper.getMainLooper()).post {
                            onBatteryStatusListener.add(AntplusBatteryStatus.ofRaw(dataPage.batteryStatus.value ) ?: AntplusBatteryStatus.INVALID)
                        }
                    }
                    is PowerDataPage02GetSetParameters -> {
                        Handler(Looper.getMainLooper()).post {
                            onLogDataListener.add(
                                AntplusLogData(
                                    AntplusLogEvent.VERBOSE,
                                    TAG,
                                    "(handle): PowerDataPage02GetSetParameters ${dataPage.subPage}"
                                )
                            )
                        }
                    }
                    else -> {
                        /*
                        Handler(Looper.getMainLooper()).post {
                            onLogDataListener.add(
                                AntplusLogData(
                                    AntplusLogEvent.VERBOSE,
                                    TAG,
                                    "(handle): not handled pageNumber ${dataPage.pageNumber} " +
                                            "payload ${ByteUtils.hexString(dataPage.dataPageBytes)}"
                                )
                            )
                        }
                         */
                    }
                }
            }
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                onLogDataListener.add(
                    AntplusLogData(
                        AntplusLogEvent.ERROR,
                        TAG,
                        "(handleAntDevice): error ${e.message} "
                    )
                )
            }
        }
    }
    /** Closes all active event stream listeners. Called when the plugin is detached from the engine. */
    override fun close() {
        super.close()
        // close bikepower listeners
        onPowerDataListener.onEventsDone()
        onBalanceDataListener.onEventsDone()
        onBatteryStatusListener.onEventsDone()
        onCadenceDataListener.onEventsDone()
        onTorqueEffectivenessDataListener.onEventsDone()
        onPedalSmoothnessDataListener.onEventsDone()
    }
}