package me.heyteacher.flutter_antplus.logging

import me.heyteacher.flutter_antplus.logging.pigeons.AntplusLogData
import me.heyteacher.flutter_antplus.logging.pigeons.OnLogDataStreamHandler
import me.heyteacher.flutter_antplus.logging.pigeons.PigeonEventSink

/**
 * A Pigeon event stream handler that forwards native ANT+ log messages
 * to both the Flutter event channel and the Android [io.flutter.Log].
 *
 * Log levels are mapped to Android logcat methods:
 * DEBUG/VERBOSE -> Log.d, INFO -> Log.i, WARNING -> Log.w, ERROR -> Log.e
 */
class OnLogDataListener : OnLogDataStreamHandler() {
    private var eventSink: PigeonEventSink<AntplusLogData>? = null

    /** Called when the Flutter side starts listening. Stores the [sink] for dispatching events. */
    override fun onListen(
        p0: Any?,
        sink: PigeonEventSink<AntplusLogData>
    ) {
        eventSink = sink
    }

    /**
     * Dispatches a [logData] entry to Android logcat and to the Flutter event sink.
     * @param logData the log data to forward
     */
    fun add(logData: AntplusLogData) {
        eventSink?.success(logData)
    }

    /** Signals end of stream and releases the event sink. */
    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}