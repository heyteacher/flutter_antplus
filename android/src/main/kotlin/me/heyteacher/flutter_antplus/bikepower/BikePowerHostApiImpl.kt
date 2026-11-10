package me.heyteacher.flutter_antplus.bikepower

import android.os.Handler
import android.os.Looper
import be.glever.ant.channel.AntChannel
import be.glever.antplus.common.datapage.AbstractAntPlusDataPage
import be.glever.antplus.common.datapage.DataPage81ProductInformation
import be.glever.antplus.common.datapage.DataPage82BatteryStatus
import be.glever.antplus.common.datapage.registry.AbstractDataPageRegistry
import be.glever.antplus.power.PowerChannel
import be.glever.antplus.power.datapage.PowerDataPageRegistry
import be.glever.antplus.power.datapage.main.PowerDataPage02GetSetParameters
import be.glever.antplus.power.datapage.main.PowerDataPage10PowerOnly
import be.glever.antplus.power.datapage.main.PowerDataPage13TorqueEffectivenessPedalSmoothness
import io.flutter.plugin.common.BinaryMessenger
import me.heyteacher.flutter_antplus.AbstractHostApi
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
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDevice
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDeviceType
import me.heyteacher.flutter_antplus.device.pigeons.OnDeviceStateChangeStreamHandler
import me.heyteacher.flutter_antplus.device.pigeons.OnRequestAccessResultStreamHandler
import me.heyteacher.flutter_antplus.device.pigeons.OnScanResultStreamHandler
import me.heyteacher.flutter_antplus.heartrate.HeartrateHostApiImpl
import me.heyteacher.flutter_antplus.logging.OnLogDataListener
import me.heyteacher.flutter_antplus.logging.pigeons.AntplusLogData
import me.heyteacher.flutter_antplus.logging.pigeons.AntplusLogEvent

/**
 * Android implementation of the [me.heyteacher.flutter_antplus.bikepower.pigeons.BikepowerHostApi] Pigeon interface with j-antplus Java library
 *
 * Manages scanning, connection, and telemetry subscriptions for an ANT+ Bike Power Meter.
 * Registers Pigeon event channel stream handlers for power, cadence, balance, pedal smoothness,
 * torque effectiveness, and battery status, and forwards all native events to Flutter.
 *
 * @param binaryMessenger the Flutter binary messenger for Pigeon channel registration
 * @param onLogDataListener shared log listener to forward native log events to Flutter
 */
class BikePowerHostApiImpl(
    binaryMessenger: BinaryMessenger,
    val onLogDataListener: OnLogDataListener
) : AbstractHostApi(binaryMessenger, onLogDataListener), BikepowerHostApi {

    companion object {
        private val TAG: String = BikePowerHostApiImpl::class.java.simpleName
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
        OnScanResultStreamHandler.register(
            binaryMessenger, onScanResultListener, AntplusDeviceType.BIKEPOWER.name
        )
        OnDeviceStateChangeStreamHandler.register(
            binaryMessenger, onDeviceStateChangeListener, AntplusDeviceType.BIKEPOWER.name
        )
        OnRequestAccessResultStreamHandler.register(
            binaryMessenger, onRequestAccessResultListener, AntplusDeviceType.BIKEPOWER.name
        )
        OnPowerDataStreamHandler.register(binaryMessenger, onPowerDataListener)
        OnPedalSmoothnessDataStreamHandler.register(binaryMessenger, onPedalSmoothnessDataListener)
        OnBatteryStatusDataStreamHandler.register(binaryMessenger, onBatteryStatusListener)
        OnBalanceDataStreamHandler.register(binaryMessenger, onBalanceDataListener)
        OnCadenceDataStreamHandler.register(binaryMessenger, onCadenceDataListener)
        OnTorqueEffectivenessDataStreamHandler.register(
            binaryMessenger, onTorqueEffectivenessDataListener
        )
    }

    override fun deviceType(): AntplusDeviceType {
        return AntplusDeviceType.BIKEPOWER
    }

    override fun deviceChannel(): AntChannel {
        return PowerChannel(getAntUsbDevice())
    }

    override fun dataRegistry(): AbstractDataPageRegistry {
        return powerDataPageRegistry
    }

    override fun handleScanDataPage(dataPage: AbstractAntPlusDataPage) {
        if (dataPage is DataPage81ProductInformation) {
            Handler(Looper.getMainLooper()).post {
                onScanResultListener.add(
                    AntplusDevice(
                        dataPage.deviceNumber,
                        dataPage.deviceNumberAsString,
                        deviceType()
                    )
                )
            }
        }
        else {
            log(TAG,AntplusLogEvent.DEBUG, "(handleScanDataPage): page number ${dataPage.pageNumber}")
        }
    }

    override fun handleConnectedDataPage(dataPage: AbstractAntPlusDataPage) {
        when (dataPage) {
            is PowerDataPage10PowerOnly -> {
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
                    onBatteryStatusListener.add(
                        AntplusBatteryStatus.ofRaw(dataPage.batteryStatus.value)
                            ?: AntplusBatteryStatus.INVALID
                    )
                }
            }
            is PowerDataPage02GetSetParameters -> {
                Handler(Looper.getMainLooper()).post {
                    onLogDataListener.add(
                        AntplusLogData(
                            AntplusLogEvent.VERBOSE,
                            TAG,
                            "(handleDataPage): PowerDataPage02GetSetParameters ${dataPage.subPage}"
                        )
                    )
                }
            }
            else -> {
                log(TAG,AntplusLogEvent.DEBUG, "(handleConnectedDataPage): page number ${dataPage.pageNumber}")
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