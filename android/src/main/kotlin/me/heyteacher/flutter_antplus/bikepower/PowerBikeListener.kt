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
    private var eventSink: PigeonEventSink<AntplusPedalSmoothnessData>? = null

    override fun onListen(p0: Any?, sink: PigeonEventSink<AntplusPedalSmoothnessData>) {
        eventSink = sink
    }

    fun add(pedalSmoothnessData: AntplusPedalSmoothnessData) {
        eventSink?.success(pedalSmoothnessData)
    }

    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}

class OnTorqueEffectivenessDataListener : OnTorqueEffectivenessDataStreamHandler() {
    private var eventSink: PigeonEventSink<AntplusTorqueEffectivenessData>? = null

    override fun onListen(p0: Any?, sink: PigeonEventSink<AntplusTorqueEffectivenessData>) {
        eventSink = sink
    }

    fun add(torqueEffectivenessData: AntplusTorqueEffectivenessData) {
        eventSink?.success(torqueEffectivenessData)
    }

    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}

class OnBatteryStatusListener : OnBatteryStatusDataStreamHandler() {
    private var eventSink: PigeonEventSink<AntplusBatteryStatus>? = null

    override fun onListen(p0: Any?, sink: PigeonEventSink<AntplusBatteryStatus>) {
        eventSink = sink
    }

    fun add(batteryStatus: AntplusBatteryStatus) {
        eventSink?.success(batteryStatus)
    }

    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}