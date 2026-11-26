package me.heyteacher.flutter_antplus.bikepower

import android.content.Context
import android.os.Handler
import android.os.Looper
import be.glever.ant.channel.AntChannel
import be.glever.antplus.common.datapage.AbstractAntPlusDataPage
import be.glever.antplus.common.datapage.DataPage70Request
import be.glever.antplus.common.datapage.DataPage81ProductInformation
import be.glever.antplus.common.datapage.DataPage82BatteryStatus
import be.glever.antplus.common.datapage.registry.AbstractDataPageRegistry
import be.glever.antplus.power.PowerChannel
import be.glever.antplus.power.datapage.PowerDataPageRegistry
import be.glever.antplus.power.datapage.main.PowerDataPage02GetSetParameters
import be.glever.antplus.power.datapage.main.PowerDataPage10PowerOnly
import be.glever.antplus.power.datapage.main.PowerDataPage13TorqueEffectivenessPedalSmoothness
import be.glever.antplus.power.datapage.main.PowerDataSubPage01CrankParameters
import be.glever.antplus.power.datapage.main.PowerDataSubPage254AdvancedCapabilities2
import io.flutter.plugin.common.BinaryMessenger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.heyteacher.flutter_antplus.AbstractHostApi
import me.heyteacher.flutter_antplus.bikepower.pigeons.AntplusAdvancedCapabilitiesData
import me.heyteacher.flutter_antplus.bikepower.pigeons.AntplusPedalSmoothnessData
import me.heyteacher.flutter_antplus.bikepower.pigeons.AntplusTorqueEffectivenessData
import me.heyteacher.flutter_antplus.bikepower.pigeons.BikepowerHostApi
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnAdvancedCapabilitiesDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnBalanceDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnCadenceDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnCrankLengthInMmStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnPedalSmoothnessDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnPowerDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnTorqueEffectivenessDataStreamHandler
import me.heyteacher.flutter_antplus.device.pigeons.AntplusBatteryStatus
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDevice
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDeviceType
import me.heyteacher.flutter_antplus.logging.OnLogDataStreamHandlerImpl
import me.heyteacher.flutter_antplus.logging.pigeons.AntplusLogLevel
import me.heyteacher.flutter_antplus.usb_adapter.OnUsbAdapterStateChangeStreamHandlerImpl
import me.heyteacher.flutter_antplus.usb_adapter.UsbAntplusAdapterManager

/**
 * Android implementation of the [me.heyteacher.flutter_antplus.bikepower.pigeons.BikepowerHostApi]
 * Pigeon interface with j-antplus Java library
 *
 * Manages scanning, connection, and bike power subscriptions for an ANT+ Bike Power Meter.

 * Listen and forward to Flutter of:
 * - scan results
 * - device state
 * - battery status
 * - power in watt
 * - cadence in rpm
 * - balance in percentage
 * - pedal smoothness
 * - torque effectiveness
 *
 * Requests, forwarding to Flutter and sets of:
 * - crank length in mm
 * - advanced capabilities
 *   - 4Hz
 *   - 8Hz
 *   - PCO
 *   - Power Phase
 *   - Rider Position
 *   - Torque Barycenter
 *
 * @param binaryMessenger the Flutter binary messenger for Pigeon channel registration
 */
class BikePowerHostApiImpl(
    binaryMessenger: BinaryMessenger,
    context: Context,
    onLogDataStreamHandler: OnLogDataStreamHandlerImpl,
    onUsbAdapterStateChangeHandler: OnUsbAdapterStateChangeStreamHandlerImpl
) : AbstractHostApi(
    binaryMessenger, context, onLogDataStreamHandler, onUsbAdapterStateChangeHandler
), BikepowerHostApi {

    companion object {
        private val _TAG: String = BikePowerHostApiImpl::class.java.simpleName
    }

    private val _powerDataPageRegistry = PowerDataPageRegistry()
    private val _onPowerDataStreamHandler: OnPowerDataStreamHandlerImpl =
        OnPowerDataStreamHandlerImpl()
    private val _onBalanceDataStreamHandler: OnBalanceDataStreamHandlerImpl =
        OnBalanceDataStreamHandlerImpl()
    private val _onCadenceDataStreamHandler: OnCadenceDataStreamHandlerImpl =
        OnCadenceDataStreamHandlerImpl()
    private val _onPedalSmoothnessDataStreamHandler: OnPedalSmoothnessDataStreamHandlerImpl =
        OnPedalSmoothnessDataStreamHandlerImpl()
    private val _onTorqueEffectivenessDataStreamHandler: OnTorqueEffectivenessDataStreamHandlerImpl =
        OnTorqueEffectivenessDataStreamHandlerImpl()

    private val _onCrankLengthInMmStreamHandler: OnCrankLengthInMmStreamHandlerImpl =
        OnCrankLengthInMmStreamHandlerImpl()

    private val _onAdvancedCapabilitiesDataStreamHandler: OnAdvancedCapabilitiesDataStreamHandlerImpl =
        OnAdvancedCapabilitiesDataStreamHandlerImpl()

    private val _scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Registers all Pigeon Host APIs and event stream handlers on initialization.
     */
    init {
        BikepowerHostApi.setUp(binaryMessenger, this)
        OnPowerDataStreamHandler.register(binaryMessenger, _onPowerDataStreamHandler)
        OnPedalSmoothnessDataStreamHandler.register(
            binaryMessenger, _onPedalSmoothnessDataStreamHandler
        )
        OnBalanceDataStreamHandler.register(binaryMessenger, _onBalanceDataStreamHandler)
        OnCadenceDataStreamHandler.register(binaryMessenger, _onCadenceDataStreamHandler)
        OnTorqueEffectivenessDataStreamHandler.register(
            binaryMessenger, _onTorqueEffectivenessDataStreamHandler
        )
        OnCrankLengthInMmStreamHandler.register(
            binaryMessenger, _onCrankLengthInMmStreamHandler
        )
        OnAdvancedCapabilitiesDataStreamHandler.register(
            binaryMessenger, _onAdvancedCapabilitiesDataStreamHandler
        )
    }

    override fun deviceType(): AntplusDeviceType {
        return AntplusDeviceType.BIKEPOWER
    }

    override fun dataRegistry(): AbstractDataPageRegistry {
        return _powerDataPageRegistry
    }

    override fun deviceChannel(deviceNumber: Long?): AntChannel {
        return PowerChannel(
            UsbAntplusAdapterManager.getInstance().getAntplusUsbAdapter(), deviceNumber
        )
    }

    override fun handleScanDataPage(dataPage: AbstractAntPlusDataPage) {
        if (dataPage is DataPage81ProductInformation) {
            Handler(Looper.getMainLooper()).post {
                onScanResultStreamHandler.add(
                    AntplusDevice(
                        dataPage.deviceNumber, dataPage.deviceNumberAsString, deviceType()
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
                    _TAG,
                    AntplusLogLevel.DEBUG,
                    "(handleConnectedDataPage): PowerDataPage02GetSetParameters sub page ${dataPage.subPage}"
                )
                when (val subPage = dataPage.subPage) {
                    is PowerDataSubPage01CrankParameters -> {
                        Handler(Looper.getMainLooper()).post {
                            _onCrankLengthInMmStreamHandler.add(subPage.crankLengthInMillimeters)
                        }
                    }

                    is PowerDataSubPage254AdvancedCapabilities2 -> {
                        Handler(Looper.getMainLooper()).post {
                            _onAdvancedCapabilitiesDataStreamHandler.add(
                                AntplusAdvancedCapabilitiesData(
                                    is4HertzEnabled = subPage.is4HertzEnabled,
                                    is4HertzSupported = subPage.is4HertzSupported,
                                    is8HertzEnabled = subPage.is8HertzEnabled,
                                    is8HertzSupported = subPage.is8HertzSupported,
                                    powerPhaseEnabled = subPage.isPowerPhaseEnabled,
                                    powerPhaseSupported = subPage.isPowerPhaseSupported,
                                    pcoEnabled = subPage.isPCOEnabled,
                                    pcoSupported = subPage.isPCOSupported,
                                    riderPositionEnabled = subPage.isRiderPositionEnabled,
                                    riderPositionSupported = subPage.isRiderPositionSupported,
                                    torqueBarycenterEnabled = subPage.isTorqueBarycenterEnabled,
                                    torqueBarycenterSupported = subPage.isTorqueBarycenterSupported
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    override fun requestCrankLengthInMm() {
        log(_TAG, AntplusLogLevel.INFO,"<requestCrankLengthInMm>")
        sendDataPage(
            DataPage70Request(
                2,
                false,
                false,
                0x02,
                0x01.toByte()
            )
        )
    }

    //private val _setRequestDelay = 1000.milliseconds

    override fun setCrankLengthInMm(crankLengthInMm: Double) {
        log(_TAG, AntplusLogLevel.INFO,"<setCrankLengthInMm>: $crankLengthInMm")
        sendDataPage(
            PowerDataPage02GetSetParameters(
                PowerDataSubPage01CrankParameters(crankLengthInMm)
            )
        )
        _scope.launch {
            //delay(_setRequestDelay)
            requestCrankLengthInMm()
        }
    }

    override fun requestAdvancedCapabilities() {
        log(_TAG, AntplusLogLevel.INFO,"<requestAdvancedCapabilities>")
        sendDataPage(
            DataPage70Request(
                2,
                false,
                false,
                0x02,
                0xFE.toByte()
            )
        )
    }

    override fun set8Hertz(enable: Boolean) {
        log(_TAG, AntplusLogLevel.INFO,"<set8Hertz>: $enable")
        sendDataPage(
            PowerDataPage02GetSetParameters(
                PowerDataSubPage254AdvancedCapabilities2.createHertzSetter(enable)
            )
        )
        _scope.launch {
            //delay(_setRequestDelay)
            requestAdvancedCapabilities()
        }
    }

    override fun setPowerPhase(enable: Boolean) {
        log(_TAG, AntplusLogLevel.INFO,"<setPowerPhase>: $enable")
        sendDataPage(
            PowerDataPage02GetSetParameters(
                PowerDataSubPage254AdvancedCapabilities2.createPowerPhaseSetter(enable)
            )
        )
        _scope.launch {
            //delay(_setRequestDelay)
            requestAdvancedCapabilities()
        }
    }

    override fun setPco(enable: Boolean) {
        log(_TAG, AntplusLogLevel.INFO,"<setPco>: $enable")
        sendDataPage(
            PowerDataPage02GetSetParameters(
                PowerDataSubPage254AdvancedCapabilities2.createPCOSetter(enable)
            )
        )
        _scope.launch {
            //delay(_setRequestDelay)
            requestAdvancedCapabilities()
        }
    }

    override fun setRiderPosition(enable: Boolean) {
        log(_TAG, AntplusLogLevel.INFO,"<setRiderPosition>: $enable")
        sendDataPage(
            PowerDataPage02GetSetParameters(
                PowerDataSubPage254AdvancedCapabilities2.createRiderPositionSetter(enable)
            )
        )
        _scope.launch {
            //delay(_setRequestDelay)
            requestAdvancedCapabilities()
        }
    }

    override fun setTorqueBarycenter(enable: Boolean) {
        log(_TAG, AntplusLogLevel.INFO,"<setTorqueBarycenter>: $enable")
        sendDataPage(
            PowerDataPage02GetSetParameters(
                PowerDataSubPage254AdvancedCapabilities2.createTorqueBarycenterSetter(enable)
            )
        )
        _scope.launch {
            //delay(_setRequestDelay)
            requestAdvancedCapabilities()
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
        _onAdvancedCapabilitiesDataStreamHandler.onEventsDone()
        _onCrankLengthInMmStreamHandler.onEventsDone()
    }
}