package me.heyteacher.flutter_antplus.cadence

import me.heyteacher.flutter_antplus.cadence.pigeons.OnCadenceDataStreamHandler
import me.heyteacher.flutter_antplus.cadence.pigeons.PigeonEventSink

class OnCadenceDataListener : OnCadenceDataStreamHandler() {
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