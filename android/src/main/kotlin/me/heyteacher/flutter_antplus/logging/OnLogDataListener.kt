package me.heyteacher.flutter_antplus.logging

import io.flutter.Log
import me.heyteacher.flutter_antplus.logging.pigeons.AntplusLogData
import me.heyteacher.flutter_antplus.logging.pigeons.AntplusLogEvent
import me.heyteacher.flutter_antplus.logging.pigeons.OnLogDataStreamHandler
import me.heyteacher.flutter_antplus.logging.pigeons.PigeonEventSink

class OnLogDataListener : OnLogDataStreamHandler() {
    private var eventSink: PigeonEventSink<AntplusLogData>? = null

    override fun onListen(
        p0: Any?,
        sink: PigeonEventSink<AntplusLogData>
    ) {
        eventSink = sink
    }

    fun add(logData: AntplusLogData) {
        when (logData.level) {
            AntplusLogEvent.DEBUG,
            AntplusLogEvent.VERBOSE ->
                Log.d(logData.tag, logData.message)
            AntplusLogEvent.INFO ->
                Log.i(logData.tag, logData.message)
            AntplusLogEvent.WARNING ->
                Log.w(logData.tag, logData.message)
            AntplusLogEvent.ERROR ->
                Log.e(logData.tag, logData.message)
        }
        eventSink?.success(logData)
    }

    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}