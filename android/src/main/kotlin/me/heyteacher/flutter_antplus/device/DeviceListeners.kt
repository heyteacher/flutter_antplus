package me.heyteacher.flutter_antplus.device

import me.heyteacher.flutter_antplus.device.pigeons.Device
import me.heyteacher.flutter_antplus.device.pigeons.DeviceState
import me.heyteacher.flutter_antplus.device.pigeons.OnDeviceStateChangeStreamHandler
import me.heyteacher.flutter_antplus.device.pigeons.OnRequestAccessResultStreamHandler
import me.heyteacher.flutter_antplus.device.pigeons.OnScanResultStreamHandler
import me.heyteacher.flutter_antplus.device.pigeons.PigeonEventSink
import me.heyteacher.flutter_antplus.device.pigeons.RequestAccessResult

class OnScanResultListener : OnScanResultStreamHandler() {
    private var eventSink: PigeonEventSink<Device>? = null

    override fun onListen(
        p0: Any?,
        sink: PigeonEventSink<Device>
    ) {
        eventSink = sink
    }

    fun add(heartrateDevice: Device) {
        eventSink?.success(heartrateDevice)
    }

    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}

class OnDeviceStateChangeListener : OnDeviceStateChangeStreamHandler() {
    private var eventSink: PigeonEventSink<DeviceState>? = null

    override fun onListen(
        p0: Any?,
        sink: PigeonEventSink<DeviceState>
    ) {
        eventSink = sink
    }

    fun add(deviceState: DeviceState) {
        eventSink?.success(deviceState)
    }

    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}

class OnRequestAccessResultListener : OnRequestAccessResultStreamHandler() {
    private var eventSink: PigeonEventSink<RequestAccessResult>? = null

    override fun onListen(
        p0: Any?,
        sink: PigeonEventSink<RequestAccessResult>
    ) {
        eventSink = sink
    }

    fun add(requestAccessResult: RequestAccessResult) {
        eventSink?.success(requestAccessResult)
    }

    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}