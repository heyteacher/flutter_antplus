package me.heyteacher.flutter_antplus.heartrate

import android.content.Context
import android.os.Handler
import android.os.Looper
import be.glever.ant.channel.AntChannel
import be.glever.antplus.common.datapage.AbstractAntPlusDataPage
import be.glever.antplus.common.datapage.registry.AbstractDataPageRegistry
import be.glever.antplus.hrm.HRMChannel
import be.glever.antplus.hrm.datapage.HrmDataPageRegistry
import be.glever.antplus.hrm.datapage.background.HrmDataPage2ManufacturerInformation
import be.glever.antplus.hrm.datapage.main.HrmDataPage0Default
import io.flutter.plugin.common.BinaryMessenger
import me.heyteacher.flutter_antplus.AbstractHostApi
import me.heyteacher.flutter_antplus.usb_adapter.OnUsbAdapterStateChangeStreamHandlerImpl
import me.heyteacher.flutter_antplus.usb_adapter.UsbAntplusAdapterManager
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDevice
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDeviceType
import me.heyteacher.flutter_antplus.heartrate.pigeons.HeartrateHostApi
import me.heyteacher.flutter_antplus.heartrate.pigeons.OnHeartRateDataStreamHandler
import me.heyteacher.flutter_antplus.logging.OnLogDataStreamHandlerImpl
import me.heyteacher.flutter_antplus.logging.pigeons.AntplusLogEvent

/**
 * Android implementation of the [me.heyteacher.flutter_antplus.heartrate.pigeons.HeartrateHostApi] Pigeon interface.
 *
 * Manages scanning, connection, and heart rate data subscriptions for an ANT+ HRM.
 * Registers Pigeon event channel stream handlers for scan results, device state, access
 * results, and heart rate data, forwarding all native events to Flutter.
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
        private val TAG: String = HeartrateHostApiImpl::class.java.simpleName
    }

    init {
        HeartrateHostApi.setUp(binaryMessenger, this)
        OnHeartRateDataStreamHandler.register(binaryMessenger, _onHeartRateDataStreamHandlerImpl)
    }

    override fun deviceType(): AntplusDeviceType {
        return AntplusDeviceType.HEARTRATE
    }

    override fun dataRegistry(): AbstractDataPageRegistry {
        return _hrmDataPageRegistry
    }

    override fun deviceChannel(deviceNumber: Long?): AntChannel {
        return if (deviceNumber != null) {
            HRMChannel(UsbAntplusAdapterManager.getInstance().getAntplusUsbAdapter(), deviceNumber)
        } else {
            HRMChannel(UsbAntplusAdapterManager.getInstance().getAntplusUsbAdapter())
        }
    }

    override fun handleScanDataPage(dataPage: AbstractAntPlusDataPage) {
        if (dataPage is HrmDataPage2ManufacturerInformation) {
            Handler(Looper.getMainLooper()).post {
                log(
                    TAG, AntplusLogEvent.INFO, "(handleScanDataPage): " +
                            "found device ${dataPage.deviceNumber} (${dataPage.deviceNumberAsString})"
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
        if (dataPage is HrmDataPage0Default) {
            Handler(Looper.getMainLooper()).post {
                _onHeartRateDataStreamHandlerImpl.add(dataPage.computedHeartRateInBpm.toLong())
            }
        }
    }

    override fun close() {
        super.close()
        // closes heartrate listener
        _onHeartRateDataStreamHandlerImpl.onEventsDone()
    }
}