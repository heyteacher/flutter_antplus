package me.heyteacher.flutter_antplus.device

import me.heyteacher.flutter_antplus.device.pigeons.AntplusDevice
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDeviceState
import me.heyteacher.flutter_antplus.device.pigeons.OnDeviceStateChangeStreamHandler
import me.heyteacher.flutter_antplus.device.pigeons.OnRequestAccessResultStreamHandler
import me.heyteacher.flutter_antplus.device.pigeons.OnScanResultStreamHandler
import me.heyteacher.flutter_antplus.device.pigeons.PigeonEventSink
import me.heyteacher.flutter_antplus.device.pigeons.AntplusRequestAccessResult

class OnScanResultListener : OnScanResultStreamHandler() {
    private var eventSink: PigeonEventSink<AntplusDevice>? = null

    override fun onListen(
        p0: Any?,
        sink: PigeonEventSink<AntplusDevice>
    ) {
        eventSink = sink
    }

    fun add(heartrateDevice: AntplusDevice) {
        eventSink?.success(heartrateDevice)
    }

    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}

class OnDeviceStateChangeListener : OnDeviceStateChangeStreamHandler() {
    private var eventSink: PigeonEventSink<AntplusDeviceState>? = null

    override fun onListen(
        p0: Any?,
        sink: PigeonEventSink<AntplusDeviceState>
    ) {
        eventSink = sink
    }

    fun add(deviceState: AntplusDeviceState) {
        eventSink?.success(deviceState)
    }

    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}

class OnRequestAccessResultListener : OnRequestAccessResultStreamHandler() {
    private var eventSink: PigeonEventSink<AntplusRequestAccessResult>? = null

    override fun onListen(
        p0: Any?,
        sink: PigeonEventSink<AntplusRequestAccessResult>
    ) {
        eventSink = sink
    }

    fun add(requestAccessResult: AntplusRequestAccessResult) {
        eventSink?.success(requestAccessResult)
    }

    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}