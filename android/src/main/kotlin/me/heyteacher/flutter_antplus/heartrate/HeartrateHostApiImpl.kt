package me.heyteacher.flutter_antplus.heartrate

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
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDevice
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDeviceType
import me.heyteacher.flutter_antplus.heartrate.pigeons.HeartrateHostApi
import me.heyteacher.flutter_antplus.heartrate.pigeons.OnHeartRateDataStreamHandler
import me.heyteacher.flutter_antplus.logging.OnLogDataListener
import me.heyteacher.flutter_antplus.logging.pigeons.AntplusLogData
import me.heyteacher.flutter_antplus.logging.pigeons.AntplusLogEvent

/**
 * Android implementation of the [me.heyteacher.flutter_antplus.heartrate.pigeons.HeartrateHostApi] Pigeon interface.
 *
 * Manages scanning, connection, and heart rate data subscriptions for an ANT+ HRM.
 * Registers Pigeon event channel stream handlers for scan results, device state, access
 * results, and heart rate data, forwarding all native events to Flutter.
 *
 * @param binaryMessenger the Flutter binary messenger for Pigeon channel registration
 * @param onLogDataListener shared log listener to forward native log events to Flutter
 */
class HeartrateHostApiImpl(
    binaryMessenger: BinaryMessenger,
    private var onLogDataListener: OnLogDataListener
) : AbstractHostApi(binaryMessenger,onLogDataListener), HeartrateHostApi {

    private val hrmDataPageRegistry: HrmDataPageRegistry = HrmDataPageRegistry()
    private var onHeartRateDataListener: OnHeartRateDataListener = OnHeartRateDataListener()

    companion object {
        private val TAG: String = HeartrateHostApiImpl::class.java.simpleName
    }

    init {
        HeartrateHostApi.setUp(binaryMessenger, this)
        OnHeartRateDataStreamHandler.register(binaryMessenger, onHeartRateDataListener)
    }

    override fun deviceType(): AntplusDeviceType {
        return AntplusDeviceType.HEARTRATE
    }

    override fun dataRegistry(): AbstractDataPageRegistry {
        return hrmDataPageRegistry
    }

    override fun deviceChannel(): AntChannel {
        return HRMChannel(getAntUsbDevice())
    }

    override fun handleScanDataPage(dataPage: AbstractAntPlusDataPage) {
        if (dataPage is HrmDataPage2ManufacturerInformation) {
            Handler(Looper.getMainLooper()).post {
                log(TAG,AntplusLogEvent.INFO, "(handleScanDataPage): " +
                        "found device ${dataPage.deviceNumber} (${dataPage.deviceNumberAsString})"
                )
                onScanResultListener.add(
                    AntplusDevice(dataPage.deviceNumber,dataPage.deviceNumberAsString,deviceType())
                )
            }
        }
        else {
            log(TAG,AntplusLogEvent.DEBUG, "(handleScanDataPage): page number ${dataPage.pageNumber}")
        }
    }

    override fun handleConnectedDataPage(dataPage: AbstractAntPlusDataPage) {
        if (dataPage is HrmDataPage0Default) {
            Handler(Looper.getMainLooper()).post {
                onHeartRateDataListener.add(dataPage.computedHeartRateInBpm.toLong())
            }
        }
        else {
            log(TAG,AntplusLogEvent.DEBUG, "(handleConnectedDataPage): page number ${dataPage.pageNumber}")
        }
    }

    override fun close() {
        super.close()
        // closes heartrate listener
        onHeartRateDataListener.onEventsDone()
    }
}