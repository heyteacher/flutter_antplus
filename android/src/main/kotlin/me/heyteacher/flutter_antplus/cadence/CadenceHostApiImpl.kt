package me.heyteacher.flutter_antplus.cadence

import android.content.Context
import android.os.Handler
import android.os.Looper
import be.glever.ant.channel.AntChannel
import be.glever.antplus.common.datapage.AbstractAntPlusDataPage
import be.glever.antplus.common.datapage.registry.AbstractDataPageRegistry
import be.glever.antplus.speedcadence.CadenceChannel
import be.glever.antplus.speedcadence.datapage.AbstractSpeedCadenceDataPage
import be.glever.antplus.speedcadence.datapage.SpeedAndCadenceDataPageRegistry
import be.glever.antplus.speedcadence.datapage.background.SpeedCadenceDataPage2ManufacturerInformation
import io.flutter.plugin.common.BinaryMessenger
import me.heyteacher.flutter_antplus.cadence.pigeons.CadenceHostApi
import me.heyteacher.flutter_antplus.cadence.pigeons.OnCadenceDataStreamHandler
import me.heyteacher.flutter_antplus.AbstractHostApi
import me.heyteacher.flutter_antplus.usb_adapter.OnUsbAdapterStateChangeStreamHandlerImpl
import me.heyteacher.flutter_antplus.usb_adapter.UsbAntplusAdapterManager
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDevice
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDeviceType
import me.heyteacher.flutter_antplus.device.pigeons.OnDeviceStateChangeStreamHandler
import me.heyteacher.flutter_antplus.device.pigeons.OnScanResultStreamHandler
import me.heyteacher.flutter_antplus.logging.OnLogDataStreamHandlerImpl
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
 */
class CadenceHostApiImpl(
    binaryMessenger: BinaryMessenger,
    context: Context,
    onLogDataStreamHandler: OnLogDataStreamHandlerImpl,
    onUsbAdapterStateChangeHandler: OnUsbAdapterStateChangeStreamHandlerImpl
) : AbstractHostApi(binaryMessenger, context,onLogDataStreamHandler,onUsbAdapterStateChangeHandler), CadenceHostApi {

    companion object {
        private val TAG: String = CadenceHostApiImpl::class.java.simpleName
    }

    private val speedAndCadenceDataPageRegistry: SpeedAndCadenceDataPageRegistry = SpeedAndCadenceDataPageRegistry()
    private var _onCadenceDataStreamHandler: OnCadenceDataStreamHandlerImpl = OnCadenceDataStreamHandlerImpl()
    private var prevRevCount = 0
    private var prevEventTime = 0L
    private var cycles = 0

    init {
        CadenceHostApi.setUp(binaryMessenger, this)
        OnScanResultStreamHandler.register(
            binaryMessenger, onScanResultStreamHandler, AntplusDeviceType.CADENCE.name
        )
        OnDeviceStateChangeStreamHandler.register(
            binaryMessenger, onDeviceStateChangeStreamHandler, AntplusDeviceType.CADENCE.name
        )
        OnCadenceDataStreamHandler.register(binaryMessenger, _onCadenceDataStreamHandler)
    }

    override fun deviceType(): AntplusDeviceType {
        return AntplusDeviceType.CADENCE
    }

    override fun dataRegistry(): AbstractDataPageRegistry {
        return speedAndCadenceDataPageRegistry
    }

    override fun deviceChannel(deviceNumber: Long?): AntChannel {
        return if (deviceNumber != null) {
            CadenceChannel(UsbAntplusAdapterManager.getInstance().getAntplusUsbAdapter(), deviceNumber)
        } else {
            CadenceChannel(UsbAntplusAdapterManager.getInstance().getAntplusUsbAdapter())
        }
    }

    override fun handleScanDataPage(dataPage: AbstractAntPlusDataPage) {
        if (dataPage is SpeedCadenceDataPage2ManufacturerInformation) {
            Handler(Looper.getMainLooper()).post {
                this@CadenceHostApiImpl.onLogDataStreamHandler.add(
                    AntplusLogData(
                        AntplusLogEvent.INFO,
                        TAG,
                        "(handleScanDataPage): found device ${dataPage.deviceNumber} " +
                                "(${dataPage.deviceNumberAsString})"
                    )
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
        if (dataPage is AbstractSpeedCadenceDataPage) {
            Handler(Looper.getMainLooper()).post {
                _onCadenceDataStreamHandler.add(calcCadence(dataPage))
            }
        }
    }

    override fun connect(deviceNumber: Long) {
        prevEventTime = 0
        prevRevCount = 0
        cycles = 0
        super.connect(deviceNumber)
    }

    private fun calcCadence(dataPage: AbstractSpeedCadenceDataPage): Long {
        var currRevCount = dataPage.cumulativeRevolutions
        // no new value, return 0
        if (currRevCount == prevRevCount) return 0
        // cycle end, add 256 and increment cycles count
        if (currRevCount < prevRevCount) {
           cycles++
           currRevCount += (cycles * 256)
        }
        val currEventTime = dataPage.eventTime
        // no new current time, return 0
        if (currEventTime <= prevEventTime) return 0
        if (prevEventTime == 0L)  return 0
        val cadence = (1000 * 60 * ( (currRevCount - prevRevCount) / (currEventTime - prevEventTime).toDouble())).toLong()
        prevRevCount = currRevCount
        prevEventTime = currEventTime
        return cadence
    }

    override fun close() {
        super.close()
        // closes heartrate listener
        _onCadenceDataStreamHandler.onEventsDone()
    }
}