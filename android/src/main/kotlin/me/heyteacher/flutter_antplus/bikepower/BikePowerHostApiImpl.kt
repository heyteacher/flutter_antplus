package me.heyteacher.flutter_antplus.bikepower

import android.content.Context
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
import me.heyteacher.flutter_antplus.bikepower.pigeons.AntplusPedalSmoothnessData
import me.heyteacher.flutter_antplus.bikepower.pigeons.AntplusTorqueEffectivenessData
import me.heyteacher.flutter_antplus.bikepower.pigeons.BikepowerHostApi
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnBalanceDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnCadenceDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnPedalSmoothnessDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnPowerDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnTorqueEffectivenessDataStreamHandler
import me.heyteacher.flutter_antplus.AbstractHostApi
import me.heyteacher.flutter_antplus.device.pigeons.AntplusBatteryStatus
import me.heyteacher.flutter_antplus.usb_adapter.OnUsbAdapterStateChangeStreamHandlerImpl
import me.heyteacher.flutter_antplus.usb_adapter.UsbAntplusAdapterManager
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDevice
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDeviceType
import me.heyteacher.flutter_antplus.device.pigeons.OnDeviceStateChangeStreamHandler
import me.heyteacher.flutter_antplus.device.pigeons.OnScanResultStreamHandler
import me.heyteacher.flutter_antplus.logging.OnLogDataStreamHandlerImpl
import me.heyteacher.flutter_antplus.logging.pigeons.AntplusLogLevel

/**
 * Android implementation of the [me.heyteacher.flutter_antplus.bikepower.pigeons.BikepowerHostApi] Pigeon interface with j-antplus Java library
 *
 * Manages scanning, connection, and telemetry subscriptions for an ANT+ Bike Power Meter.
 * Registers Pigeon event channel stream handlers for power, cadence, balance, pedal smoothness,
 * torque effectiveness, and battery status, and forwards all native events to Flutter.
 *
 * @param binaryMessenger the Flutter binary messenger for Pigeon channel registration
 */
class BikePowerHostApiImpl(
    binaryMessenger: BinaryMessenger,
    context: Context,
    onLogDataStreamHandler: OnLogDataStreamHandlerImpl,
    onUsbAdapterStateChangeHandler: OnUsbAdapterStateChangeStreamHandlerImpl
) : AbstractHostApi(binaryMessenger, context, onLogDataStreamHandler, onUsbAdapterStateChangeHandler), BikepowerHostApi {

    companion object {
        private val TAG: String = BikePowerHostApiImpl::class.java.simpleName
    }

    private val _powerDataPageRegistry = PowerDataPageRegistry()
    private val _onPowerDataStreamHandler: OnPowerDataStreamHandlerImpl = OnPowerDataStreamHandlerImpl()
    private val _onBalanceDataStreamHandler: OnBalanceDataStreamHandlerImpl = OnBalanceDataStreamHandlerImpl()
    private val _onCadenceDataStreamHandler: OnCadenceDataStreamHandlerImpl = OnCadenceDataStreamHandlerImpl()
    private val _onPedalSmoothnessDataStreamHandler: OnPedalSmoothnessDataStreamHandlerImpl =
        OnPedalSmoothnessDataStreamHandlerImpl()
    private val _onTorqueEffectivenessDataStreamHandler: OnTorqueEffectivenessDataStreamHandlerImpl =
        OnTorqueEffectivenessDataStreamHandlerImpl()
    /**
     * Registers all Pigeon Host APIs and event stream handlers on initialization.
     */
    init {
        BikepowerHostApi.setUp(binaryMessenger, this)
        OnScanResultStreamHandler.register(
            binaryMessenger, onScanResultStreamHandler, AntplusDeviceType.BIKEPOWER.name
        )
        OnDeviceStateChangeStreamHandler.register(
            binaryMessenger, onDeviceStateChangeStreamHandler, AntplusDeviceType.BIKEPOWER.name
        )
        OnPowerDataStreamHandler.register(binaryMessenger, _onPowerDataStreamHandler)
        OnPedalSmoothnessDataStreamHandler.register(binaryMessenger, _onPedalSmoothnessDataStreamHandler)
        OnBalanceDataStreamHandler.register(binaryMessenger, _onBalanceDataStreamHandler)
        OnCadenceDataStreamHandler.register(binaryMessenger, _onCadenceDataStreamHandler)
        OnTorqueEffectivenessDataStreamHandler.register(
            binaryMessenger, _onTorqueEffectivenessDataStreamHandler
        )
    }

    override fun deviceType(): AntplusDeviceType {
        return AntplusDeviceType.BIKEPOWER
    }

    override fun deviceChannel(deviceNumber: Long? ): AntChannel {
        return PowerChannel(UsbAntplusAdapterManager.getInstance().getAntplusUsbAdapter(), deviceNumber)
    }

    override fun dataRegistry(): AbstractDataPageRegistry {
        return _powerDataPageRegistry
    }

    override fun handleScanDataPage(dataPage: AbstractAntPlusDataPage) {
        if (dataPage is DataPage81ProductInformation) {
            Handler(Looper.getMainLooper()).post {
                onScanResultStreamHandler.add(
                    AntplusDevice(
                        dataPage.deviceNumber,
                        dataPage.deviceNumberAsString,
                        deviceType()
                    )
                )
            }
        }
    }

    override fun handleConnectedDataPage(dataPage: AbstractAntPlusDataPage) {
        when (dataPage) {
            is PowerDataPage10PowerOnly -> {
                Handler(Looper.getMainLooper()).post {
                    _onPowerDataStreamHandler.add(dataPage.instantaneousPower.toLong())
                    _onCadenceDataStreamHandler.add(dataPage.instantaneousCadence.toLong())
                    _onBalanceDataStreamHandler.add(dataPage.pedalPowerDistribution.percentage.toLong())
                }
            }
            is PowerDataPage13TorqueEffectivenessPedalSmoothness -> {
                Handler(Looper.getMainLooper()).post {
                    _onTorqueEffectivenessDataStreamHandler.add(
                        AntplusTorqueEffectivenessData(
                            dataPage.leftTorqueEffectiveness.toDouble(),
                            dataPage.rightTorqueEffectiveness.toDouble()
                        )
                    )
                    _onPedalSmoothnessDataStreamHandler.add(
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
                    onBatteryStatusDataStreamHandler.add(
                        AntplusBatteryStatus.ofRaw(dataPage.batteryStatus.value)
                            ?: AntplusBatteryStatus.INVALID
                    )
                }
            }
            is PowerDataPage02GetSetParameters -> {
               log(
                   TAG,
                   AntplusLogLevel.DEBUG,
                   "(handleDataPage): PowerDataPage02GetSetParameters ${dataPage.subPage}")
            }
        }
    }

    /** Closes all active event stream listeners. Called when the plugin is detached from the engine. */
    override fun close() {
        super.close()
        // close bikepower listeners
        _onPowerDataStreamHandler.onEventsDone()
        _onBalanceDataStreamHandler.onEventsDone()
        _onCadenceDataStreamHandler.onEventsDone()
        _onTorqueEffectivenessDataStreamHandler.onEventsDone()
        _onPedalSmoothnessDataStreamHandler.onEventsDone()
    }
}