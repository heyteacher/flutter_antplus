package me.heyteacher.flutter_antplus.usb_adapter

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import me.heyteacher.flutter_antplus.usb_adapter.pigeons.AntplusUsbAdapterState
import me.heyteacher.flutter_antplus.usb_adapter.pigeons.OnUsbAdapterStateChangeStreamHandler
import me.heyteacher.flutter_antplus.usb_adapter.pigeons.PigeonEventSink

/**
 * A Pigeon event stream handler that emits [AntplusUsbAdapterState] values
 * to the Flutter side when an ANT+ channel access request completes.
 */
class OnUsbAdapterStateChangeStreamHandlerImpl : OnUsbAdapterStateChangeStreamHandler() {
    private var eventSink: PigeonEventSink<AntplusUsbAdapterState>? = null
    private val _scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _flow = MutableSharedFlow<AntplusUsbAdapterState>(replay = 0)
    val flow: SharedFlow<AntplusUsbAdapterState> = _flow

    /** Called when the Flutter side starts listening. Stores the [sink] for dispatching events. */
    override fun onListen(
        p0: Any?,
        sink: PigeonEventSink<AntplusUsbAdapterState>
    ) {
        eventSink = sink
    }

    /**
     * Emits the [requestAccessResult] to the Flutter access result stream.
     * @param requestAccessResult the result of the access request
     */
    fun add(requestAccessResult: AntplusUsbAdapterState) {
        eventSink?.success(requestAccessResult)
        _scope.launch {
            _flow.emit(requestAccessResult)
        }
    }

    /** Signals end of stream and releases the event sink. */
    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}