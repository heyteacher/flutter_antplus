package me.heyteacher.flutter_antplus.logging

import io.flutter.Log
import me.heyteacher.flutter_antplus.logging.pigeons.LogData
import me.heyteacher.flutter_antplus.logging.pigeons.LogEvent
import me.heyteacher.flutter_antplus.logging.pigeons.OnLogDataStreamHandler
import me.heyteacher.flutter_antplus.logging.pigeons.PigeonEventSink

class OnLogDataListener : OnLogDataStreamHandler() {
    private var eventSink: PigeonEventSink<LogData>? = null

    override fun onListen(
        p0: Any?,
        sink: PigeonEventSink<LogData>
    ) {
        eventSink = sink
    }

    fun add(logData: LogData) {
        when (logData.level) {
            LogEvent.DEBUG,
            LogEvent.VERBOSE ->
                Log.d(logData.tag, logData.message)
            LogEvent.INFO ->
                Log.i(logData.tag, logData.message)
            LogEvent.WARNING ->
                Log.w(logData.tag, logData.message)
            LogEvent.ERROR ->
                Log.e(logData.tag, logData.message)
        }
        eventSink?.success(logData)
    }

    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}