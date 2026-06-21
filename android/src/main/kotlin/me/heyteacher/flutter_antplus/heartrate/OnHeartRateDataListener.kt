package me.heyteacher.flutter_antplus.heartrate

import me.heyteacher.flutter_antplus.heartrate.pigeons.OnHeartRateDataStreamHandler
import me.heyteacher.flutter_antplus.heartrate.pigeons.PigeonEventSink

/**
 * A Pigeon event stream handler that emits real-time heart rate values (bpm)
 * from the ANT+ Heart Rate Monitor to the Flutter event channel.
 */
class OnHeartRateDataListener : OnHeartRateDataStreamHandler() {
    private var eventSink: PigeonEventSink<Long>? = null

    /** Called when the Flutter side starts listening. Stores the [sink] for dispatching events. */
    override fun onListen(p0: Any?, sink: PigeonEventSink<Long>) {
        eventSink = sink
    }

    /**
     * Emits a heart rate reading to the Flutter heart rate data stream.
     * @param bpm the heart rate value in beats per minute (bpm)
     */
    fun add(bpm: Long) {
        eventSink?.success(bpm)
    }

    /** Signals end of stream and releases the event sink. */
    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}