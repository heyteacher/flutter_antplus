package me.heyteacher.flutter_antplus.device

import me.heyteacher.flutter_antplus.device.pigeons.AntplusBatteryStatus
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDevice
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDeviceState
import me.heyteacher.flutter_antplus.device.pigeons.OnBatteryStatusDataStreamHandler
import me.heyteacher.flutter_antplus.device.pigeons.OnDeviceStateChangeStreamHandler
import me.heyteacher.flutter_antplus.device.pigeons.OnScanResultStreamHandler
import me.heyteacher.flutter_antplus.device.pigeons.PigeonEventSink

/**
 * A Pigeon event stream handler that emits [AntplusDevice] objects discovered
 * during an ANT+ device scan to the Flutter side.
 */
class OnScanResultStreamHandlerImpl : OnScanResultStreamHandler() {
    private var eventSink: PigeonEventSink<AntplusDevice>? = null

    /** Called when the Flutter side starts listening. Stores the [sink] for dispatching events. */
    override fun onListen(
        p0: Any?,
        sink: PigeonEventSink<AntplusDevice>
    ) {
        eventSink = sink
    }

    /**
     * Emits a discovered [heartrateDevice] to the Flutter scan result stream.
     * @param heartrateDevice the discovered device to emit
     */
    fun add(heartrateDevice: AntplusDevice) {
        eventSink?.success(heartrateDevice)
    }

    /** Signals end of stream and releases the event sink. */
    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}

/**
 * A Pigeon event stream handler that emits [AntplusDeviceState] changes
 * to the Flutter side whenever the connected ANT+ device changes state.
 */
class OnDeviceStateChangeStreamHandlerImpl : OnDeviceStateChangeStreamHandler() {
    private var eventSink: PigeonEventSink<AntplusDeviceState>? = null

    /** Called when the Flutter side starts listening. Stores the [sink] for dispatching events. */
    override fun onListen(
        p0: Any?,
        sink: PigeonEventSink<AntplusDeviceState>
    ) {
        eventSink = sink
    }

    /**
     * Emits the updated [deviceState] to the Flutter device state stream.
     * @param deviceState the new device state to emit
     */
    fun add(deviceState: AntplusDeviceState) {
        eventSink?.success(deviceState)
    }

    /** Signals end of stream and releases the event sink. */
    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}

/**
 * A Pigeon event stream handler that emits [AntplusBatteryStatus] values
 * from the ANT+ Bike Power Meter sensor to the Flutter event channel.
 */
class OnBatteryStatusDataStreamHandlerImpl : OnBatteryStatusDataStreamHandler() {
    private var eventSink: PigeonEventSink<AntplusBatteryStatus>? = null

    /** Called when the Flutter side starts listening. Stores the [sink] for dispatching events. */
    override fun onListen(p0: Any?, sink: PigeonEventSink<AntplusBatteryStatus>) {
        eventSink = sink
    }

    /**
     * Emits the sensor [batteryStatus] to the Flutter battery status stream.
     * @param batteryStatus the current battery status to emit
     */
    fun add(batteryStatus: AntplusBatteryStatus) {
        eventSink?.success(batteryStatus)
    }

    /** Signals end of stream and releases the event sink. */
    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}