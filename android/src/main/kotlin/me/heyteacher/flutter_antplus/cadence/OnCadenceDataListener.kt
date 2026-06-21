package me.heyteacher.flutter_antplus.cadence

import me.heyteacher.flutter_antplus.cadence.pigeons.OnCadenceDataStreamHandler
import me.heyteacher.flutter_antplus.cadence.pigeons.PigeonEventSink

/**
 * A Pigeon event stream handler that emits real-time cadence values (rpm)
 * from the ANT+ Cadence sensor to the Flutter event channel.
 */
class OnCadenceDataListener : OnCadenceDataStreamHandler() {
    private var eventSink: PigeonEventSink<Long>? = null

    /** Called when the Flutter side starts listening. Stores the [sink] for dispatching events. */
    override fun onListen(p0: Any?, sink: PigeonEventSink<Long>) {
        eventSink = sink
    }

    /**
     * Emits a cadence reading to the Flutter cadence data stream.
     * @param bpm the cadence value in revolutions per minute (rpm)
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