package me.heyteacher.flutter_antplus.bikepower

import me.heyteacher.flutter_antplus.bikepower.pigeons.BatteryStatus
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnBalanceDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnBatteryStatusDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnCadenceDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnPedalSmoothnessDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnPowerDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnTorqueEffectivenessDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.PedalSmoothnessData
import me.heyteacher.flutter_antplus.bikepower.pigeons.PigeonEventSink
import me.heyteacher.flutter_antplus.bikepower.pigeons.TorqueEffectivenessData

class OnPowerDataListener : OnPowerDataStreamHandler() {
    private var eventSink: PigeonEventSink<Long>? = null

    override fun onListen(p0: Any?, sink: PigeonEventSink<Long>) {
        eventSink = sink
    }

    fun add(powerData: Long) {
        eventSink?.success(powerData)
    }

    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}

class OnCadenceDataListener : OnCadenceDataStreamHandler() {
    private var eventSink: PigeonEventSink<Long>? = null

    override fun onListen(p0: Any?, sink: PigeonEventSink<Long>) {
        eventSink = sink
    }

    fun add(powerData: Long) {
        eventSink?.success(powerData)
    }

    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}

class OnBalanceDataListener : OnBalanceDataStreamHandler() {
    private var eventSink: PigeonEventSink<Long>? = null

    override fun onListen(p0: Any?, sink: PigeonEventSink<Long>) {
        eventSink = sink
    }

    fun add(powerData: Long) {
        eventSink?.success(powerData)
    }

    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}

class OnPedalSmoothnessDataListener : OnPedalSmoothnessDataStreamHandler() {
    private var eventSink: PigeonEventSink<PedalSmoothnessData>? = null

    override fun onListen(p0: Any?, sink: PigeonEventSink<PedalSmoothnessData>) {
        eventSink = sink
    }

    fun add(pedalSmoothnessData: PedalSmoothnessData) {
        eventSink?.success(pedalSmoothnessData)
    }

    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}

class OnTorqueEffectivenessDataListener : OnTorqueEffectivenessDataStreamHandler() {
    private var eventSink: PigeonEventSink<TorqueEffectivenessData>? = null

    override fun onListen(p0: Any?, sink: PigeonEventSink<TorqueEffectivenessData>) {
        eventSink = sink
    }

    fun add(torqueEffectivenessData: TorqueEffectivenessData) {
        eventSink?.success(torqueEffectivenessData)
    }

    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}

class OnBatteryStatusListener : OnBatteryStatusDataStreamHandler() {
    private var eventSink: PigeonEventSink<BatteryStatus>? = null

    override fun onListen(p0: Any?, sink: PigeonEventSink<BatteryStatus>) {
        eventSink = sink
    }

    fun add(batteryStatus: BatteryStatus) {
        eventSink?.success(batteryStatus)
    }

    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}