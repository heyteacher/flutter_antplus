package me.heyteacher.flutter_antplus.heartrate

import me.heyteacher.flutter_antplus.heartrate.pigeons.OnHeartRateDataStreamHandler
import me.heyteacher.flutter_antplus.heartrate.pigeons.PigeonEventSink

class OnHeartRateDataListener : OnHeartRateDataStreamHandler() {
    private var eventSink: PigeonEventSink<Long>? = null

    override fun onListen(p0: Any?, sink: PigeonEventSink<Long>) {
        eventSink = sink
    }

    fun add(bpm: Long) {
        eventSink?.success(bpm)
    }

    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}