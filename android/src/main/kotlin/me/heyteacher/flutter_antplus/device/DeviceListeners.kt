package me.heyteacher.flutter_antplus.device

import me.heyteacher.flutter_antplus.device.pigeons.AntplusDevice
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDeviceState
import me.heyteacher.flutter_antplus.device.pigeons.OnDeviceStateChangeStreamHandler
import me.heyteacher.flutter_antplus.device.pigeons.OnRequestAccessResultStreamHandler
import me.heyteacher.flutter_antplus.device.pigeons.OnScanResultStreamHandler
import me.heyteacher.flutter_antplus.device.pigeons.PigeonEventSink
import me.heyteacher.flutter_antplus.device.pigeons.AntplusRequestAccessResult

/**
 * A Pigeon event stream handler that emits [AntplusDevice] objects discovered
 * during an ANT+ device scan to the Flutter side.
 */
class OnScanResultListener : OnScanResultStreamHandler() {
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
class OnDeviceStateChangeListener : OnDeviceStateChangeStreamHandler() {
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
 * A Pigeon event stream handler that emits [AntplusRequestAccessResult] values
 * to the Flutter side when an ANT+ channel access request completes.
 */
class OnRequestAccessResultListener : OnRequestAccessResultStreamHandler() {
    private var eventSink: PigeonEventSink<AntplusRequestAccessResult>? = null

    /** Called when the Flutter side starts listening. Stores the [sink] for dispatching events. */
    override fun onListen(
        p0: Any?,
        sink: PigeonEventSink<AntplusRequestAccessResult>
    ) {
        eventSink = sink
    }

    /**
     * Emits the [requestAccessResult] to the Flutter access result stream.
     * @param requestAccessResult the result of the access request
     */
    fun add(requestAccessResult: AntplusRequestAccessResult) {
        eventSink?.success(requestAccessResult)
    }

    /** Signals end of stream and releases the event sink. */
    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}