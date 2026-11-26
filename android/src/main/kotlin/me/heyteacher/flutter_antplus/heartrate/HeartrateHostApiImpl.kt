package me.heyteacher.flutter_antplus.heartrate

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import be.glever.ant.channel.AntChannel
import be.glever.antplus.common.datapage.AbstractAntPlusDataPage
import be.glever.antplus.common.datapage.registry.AbstractDataPageRegistry
import be.glever.antplus.hrm.HRMChannel
import be.glever.antplus.hrm.datapage.HrmDataPageRegistry
import be.glever.antplus.hrm.datapage.background.HrmDataPage2ManufacturerInformation
import be.glever.antplus.hrm.datapage.background.HrmDataPage7BatteryStatus
import be.glever.antplus.hrm.datapage.main.HrmDataPage0Default
import io.flutter.plugin.common.BinaryMessenger
import me.heyteacher.flutter_antplus.AbstractHostApi
import me.heyteacher.flutter_antplus.device.pigeons.AntplusBatteryStatus
import me.heyteacher.flutter_antplus.usb_adapter.OnUsbAdapterStateChangeStreamHandlerImpl
import me.heyteacher.flutter_antplus.usb_adapter.UsbAntplusAdapterManager
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDevice
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDeviceType
import me.heyteacher.flutter_antplus.heartrate.pigeons.HeartrateHostApi
import me.heyteacher.flutter_antplus.heartrate.pigeons.OnHeartRateDataStreamHandler
import me.heyteacher.flutter_antplus.logging.OnLogDataStreamHandlerImpl

/**
 * Android implementation of the [me.heyteacher.flutter_antplus.heartrate.pigeons.HeartrateHostApi] Pigeon interface.
 *
 * Manages scanning, connection, and heart rate data subscriptions for an ANT+ HRM devices.
 *
 * Listen and forward to Flutter of:
 * - scan result,
 * - device state
 * - battery status
 * - heart rate in bpm.
 *
 * @param binaryMessenger the Flutter binary messenger for Pigeon channel registration
 */
class HeartrateHostApiImpl(
    binaryMessenger: BinaryMessenger,
    context: Context,
    onLogDataStreamHandler: OnLogDataStreamHandlerImpl,
    onUsbAdapterStateChangeHandler: OnUsbAdapterStateChangeStreamHandlerImpl
) : AbstractHostApi(
    binaryMessenger,
    context,
    onLogDataStreamHandler,
    onUsbAdapterStateChangeHandler
), HeartrateHostApi {

    private val _hrmDataPageRegistry: HrmDataPageRegistry = HrmDataPageRegistry()
    private var _onHeartRateDataStreamHandlerImpl: OnHeartRateDataStreamHandlerImpl =
        OnHeartRateDataStreamHandlerImpl()

    companion object {
        private val _TAG: String = HeartrateHostApiImpl::class.java.simpleName
    }

    init {
        HeartrateHostApi.setUp(binaryMessenger, this)
        OnHeartRateDataStreamHandler.register(binaryMessenger, _onHeartRateDataStreamHandlerImpl)
    }

    override fun deviceType(): AntplusDeviceType { return AntplusDeviceType.HEARTRATE }

    override fun dataRegistry(): AbstractDataPageRegistry { return _hrmDataPageRegistry }

    override fun deviceChannel(deviceNumber: Long?): AntChannel {
        return HRMChannel(
            UsbAntplusAdapterManager.getInstance().getAntplusUsbAdapter(),
            deviceNumber
        )
    }

    override fun handleScanDataPage(dataPage: AbstractAntPlusDataPage) {
        if (dataPage is HrmDataPage2ManufacturerInformation) {
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
            is HrmDataPage0Default -> {
                Handler(Looper.getMainLooper()).post {
                    _onHeartRateDataStreamHandlerImpl.add(dataPage.computedHeartRateInBpm.toLong())
                }
            }
            is HrmDataPage7BatteryStatus -> {
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

    override fun close() {
        super.close()
        // closes heartrate listener
        _onHeartRateDataStreamHandlerImpl.onEventsDone()
    }
}