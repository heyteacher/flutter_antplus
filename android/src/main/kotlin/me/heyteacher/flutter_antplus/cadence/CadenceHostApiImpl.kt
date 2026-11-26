package me.heyteacher.flutter_antplus.cadence

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import be.glever.ant.channel.AntChannel
import be.glever.antplus.common.datapage.AbstractAntPlusDataPage
import be.glever.antplus.common.datapage.registry.AbstractDataPageRegistry
import be.glever.antplus.speedcadence.CadenceChannel
import be.glever.antplus.speedcadence.datapage.AbstractSpeedCadenceDataPage
import be.glever.antplus.speedcadence.datapage.SpeedAndCadenceDataPageRegistry
import be.glever.antplus.speedcadence.datapage.background.SpeedCadenceDataPage2ManufacturerInformation
import be.glever.antplus.speedcadence.datapage.background.SpeedCadenceDataPage4BatteryStatus
import be.glever.antplus.speedcadence.datapage.main.SpeedCadenceDataPage0Default
import io.flutter.plugin.common.BinaryMessenger
import me.heyteacher.flutter_antplus.AbstractHostApi
import me.heyteacher.flutter_antplus.cadence.pigeons.CadenceHostApi
import me.heyteacher.flutter_antplus.cadence.pigeons.OnCadenceDataStreamHandler
import me.heyteacher.flutter_antplus.device.pigeons.AntplusBatteryStatus
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDevice
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDeviceType
import me.heyteacher.flutter_antplus.logging.OnLogDataStreamHandlerImpl
import me.heyteacher.flutter_antplus.logging.pigeons.AntplusLogLevel
import me.heyteacher.flutter_antplus.usb_adapter.OnUsbAdapterStateChangeStreamHandlerImpl
import me.heyteacher.flutter_antplus.usb_adapter.UsbAntplusAdapterManager

/**
 * Android implementation of the [me.heyteacher.flutter_antplus.heartrate.pigeons.HeartrateHostApi] Pigeon interface.
 *
 * Manages scanning, connection, and cadence data subscriptions for an ANT+ Cadence and Speed devices.
 *
 * Listen and forward to Flutter of:
 * - scan results
 * - device state
 * - battery status
 * - cadence in rpm
 *
 * @param binaryMessenger the Flutter binary messenger for Pigeon channel registration
 */
class CadenceHostApiImpl(
    binaryMessenger: BinaryMessenger,
    context: Context,
    onLogDataStreamHandler: OnLogDataStreamHandlerImpl,
    onUsbAdapterStateChangeHandler: OnUsbAdapterStateChangeStreamHandlerImpl
) : AbstractHostApi(
    binaryMessenger,
    context,
    onLogDataStreamHandler,
    onUsbAdapterStateChangeHandler
), CadenceHostApi {

    companion object {
        private val _TAG: String = CadenceHostApiImpl::class.java.simpleName
    }
    private val _speedAndCadenceDataPageRegistry: SpeedAndCadenceDataPageRegistry =
        SpeedAndCadenceDataPageRegistry()
    private var _onCadenceDataStreamHandler: OnCadenceDataStreamHandlerImpl =
        OnCadenceDataStreamHandlerImpl()
    private var _prevRevCount = 0
    private var _prevEventTime = 0L
    private var _cycles = 0

    init {
        CadenceHostApi.setUp(binaryMessenger, this)
        OnCadenceDataStreamHandler.register(binaryMessenger, _onCadenceDataStreamHandler)
    }

    override fun deviceType(): AntplusDeviceType { return AntplusDeviceType.CADENCE }

    override fun dataRegistry(): AbstractDataPageRegistry { return _speedAndCadenceDataPageRegistry }

    override fun deviceChannel(deviceNumber: Long?): AntChannel {
        return CadenceChannel(
            UsbAntplusAdapterManager.getInstance().getAntplusUsbAdapter(),
            deviceNumber
        )
    }

    override fun handleScanDataPage(dataPage: AbstractAntPlusDataPage) {
        if (dataPage is SpeedCadenceDataPage2ManufacturerInformation) {
            Handler(Looper.getMainLooper()).post {
                log(
                    _TAG,
                    AntplusLogLevel.INFO,
                    "(handleScanDataPage): found device ${dataPage.deviceNumber} " +
                            "(${dataPage.deviceNumberAsString})"
                )
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
        when(dataPage) {
            is SpeedCadenceDataPage0Default -> {
                Handler(Looper.getMainLooper()).post {
                    _onCadenceDataStreamHandler.add(calcCadence(dataPage))
                }
            }
            is SpeedCadenceDataPage4BatteryStatus -> {
                Handler(Looper.getMainLooper()).post {
                    onBatteryStatusDataStreamHandler.add(
                        AntplusBatteryStatus.ofRaw(dataPage.batteryVoltageDescription.value)
                            ?: AntplusBatteryStatus.INVALID
                    )
                    Log.d(_TAG, "batteryVoltageDescription ${dataPage.batteryVoltageDescription}")
                }
            }
        }
    }

    override fun connect(deviceNumber: Long) {
        _prevEventTime = 0
        _prevRevCount = 0
        _cycles = 0
        super.connect(deviceNumber)
    }

    private fun calcCadence(dataPage: AbstractSpeedCadenceDataPage): Long {
        var currRevCount = dataPage.cumulativeRevolutions
        // no new value, return 0
        if (currRevCount == _prevRevCount) return 0
        // cycle end, add 256 and increment cycles count
        if (currRevCount < _prevRevCount) {
            _cycles++
            currRevCount += (_cycles * 256)
        }
        val currEventTime = dataPage.eventTime
        // no new current time, return 0
        if (currEventTime <= _prevEventTime) return 0
        if (_prevEventTime == 0L) return 0
        val cadence =
            (1000 * 60 * ((currRevCount - _prevRevCount) / (currEventTime - _prevEventTime).toDouble())).toLong()
        _prevRevCount = currRevCount
        _prevEventTime = currEventTime
        return cadence
    }

    override fun close() {
        super.close()
        // closes heartrate listener
        _onCadenceDataStreamHandler.onEventsDone()
    }
}