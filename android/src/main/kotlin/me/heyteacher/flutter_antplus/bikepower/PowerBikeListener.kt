package me.heyteacher.flutter_antplus.bikepower

import me.heyteacher.flutter_antplus.bikepower.pigeons.AntplusBatteryStatus
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnBalanceDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnBatteryStatusDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnCadenceDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnPedalSmoothnessDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnPowerDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnTorqueEffectivenessDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.AntplusPedalSmoothnessData
import me.heyteacher.flutter_antplus.bikepower.pigeons.PigeonEventSink
import me.heyteacher.flutter_antplus.bikepower.pigeons.AntplusTorqueEffectivenessData

/**
 * A Pigeon event stream handler that emits real-time power values (watts)
 * from the ANT+ Bike Power Meter to the Flutter event channel.
 */
class OnPowerDataListener : OnPowerDataStreamHandler() {
    private var eventSink: PigeonEventSink<Long>? = null

    /** Called when the Flutter side starts listening. Stores the [sink] for dispatching events. */
    override fun onListen(p0: Any?, sink: PigeonEventSink<Long>) {
        eventSink = sink
    }

    /**
     * Emits a power reading to the Flutter power data stream.
     * @param powerData the power value in watts (W)
     */
    fun add(powerData: Long) {
        eventSink?.success(powerData)
    }

    /** Signals end of stream and releases the event sink. */
    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}

/**
 * A Pigeon event stream handler that emits real-time crank cadence values (rpm)
 * from the ANT+ Bike Power Meter to the Flutter event channel.
 */
class OnCadenceDataListener : OnCadenceDataStreamHandler() {
    private var eventSink: PigeonEventSink<Long>? = null

    /** Called when the Flutter side starts listening. Stores the [sink] for dispatching events. */
    override fun onListen(p0: Any?, sink: PigeonEventSink<Long>) {
        eventSink = sink
    }

    /**
     * Emits a cadence reading to the Flutter cadence data stream.
     * @param powerData the cadence value in revolutions per minute (rpm)
     */
    fun add(powerData: Long) {
        eventSink?.success(powerData)
    }

    /** Signals end of stream and releases the event sink. */
    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}

/**
 * A Pigeon event stream handler that emits real-time pedal power balance (%)
 * from the ANT+ Bike Power Meter to the Flutter event channel.
 */
class OnBalanceDataListener : OnBalanceDataStreamHandler() {
    private var eventSink: PigeonEventSink<Long>? = null

    /** Called when the Flutter side starts listening. Stores the [sink] for dispatching events. */
    override fun onListen(p0: Any?, sink: PigeonEventSink<Long>) {
        eventSink = sink
    }

    /**
     * Emits a pedal balance reading to the Flutter balance data stream.
     * @param powerData the balance value as a percentage (%)
     */
    fun add(powerData: Long) {
        eventSink?.success(powerData)
    }

    /** Signals end of stream and releases the event sink. */
    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}

/**
 * A Pigeon event stream handler that emits [AntplusPedalSmoothnessData] from
 * the ANT+ Bike Power Meter to the Flutter event channel.
 *
 * Pedal smoothness represents the percentage of time within a pedal stroke that
 * force is applied effectively.
 */
class OnPedalSmoothnessDataListener : OnPedalSmoothnessDataStreamHandler() {
    private var eventSink: PigeonEventSink<AntplusPedalSmoothnessData>? = null

    /** Called when the Flutter side starts listening. Stores the [sink] for dispatching events. */
    override fun onListen(p0: Any?, sink: PigeonEventSink<AntplusPedalSmoothnessData>) {
        eventSink = sink
    }

    /**
     * Emits a [pedalSmoothnessData] reading to the Flutter pedal smoothness stream.
     * @param pedalSmoothnessData the pedal smoothness measurement to emit
     */
    fun add(pedalSmoothnessData: AntplusPedalSmoothnessData) {
        eventSink?.success(pedalSmoothnessData)
    }

    /** Signals end of stream and releases the event sink. */
    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}

/**
 * A Pigeon event stream handler that emits [AntplusTorqueEffectivenessData] from
 * the ANT+ Bike Power Meter to the Flutter event channel.
 *
 * Torque effectiveness represents how efficiently force is applied during a pedal stroke.
 */
class OnTorqueEffectivenessDataListener : OnTorqueEffectivenessDataStreamHandler() {
    private var eventSink: PigeonEventSink<AntplusTorqueEffectivenessData>? = null

    /** Called when the Flutter side starts listening. Stores the [sink] for dispatching events. */
    override fun onListen(p0: Any?, sink: PigeonEventSink<AntplusTorqueEffectivenessData>) {
        eventSink = sink
    }

    /**
     * Emits a [torqueEffectivenessData] reading to the Flutter torque effectiveness stream.
     * @param torqueEffectivenessData the torque effectiveness measurement to emit
     */
    fun add(torqueEffectivenessData: AntplusTorqueEffectivenessData) {
        eventSink?.success(torqueEffectivenessData)
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
class OnBatteryStatusListener : OnBatteryStatusDataStreamHandler() {
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