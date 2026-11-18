package me.heyteacher.flutter_antplus.device

import me.heyteacher.flutter_antplus.device.pigeons.PigeonEventSink
import me.heyteacher.flutter_antplus.device.pigeons.AntplusRequestAccessResult
import me.heyteacher.flutter_antplus.device.pigeons.OnRequestAccessStreamHandler

/**
 * A Pigeon event stream handler that emits [AntplusRequestAccessResult] values
 * to the Flutter side when an ANT+ channel access request completes.
 */
class OnRequestAccessListener : OnRequestAccessStreamHandler() {
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