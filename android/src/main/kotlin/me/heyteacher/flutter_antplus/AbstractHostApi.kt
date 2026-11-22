package me.heyteacher.flutter_antplus

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import be.glever.ant.channel.AntChannel
import be.glever.ant.message.AntMessage
import be.glever.ant.message.data.BroadcastDataMessage
import be.glever.antplus.common.datapage.AbstractAntPlusDataPage
import be.glever.antplus.common.datapage.registry.AbstractDataPageRegistry
import io.flutter.plugin.common.BinaryMessenger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.heyteacher.flutter_antplus.device.OnBatteryStatusDataStreamHandlerImpl
import me.heyteacher.flutter_antplus.usb_adapter.OnUsbAdapterStateChangeStreamHandlerImpl
import me.heyteacher.flutter_antplus.device.OnDeviceStateChangeStreamHandlerImpl
import me.heyteacher.flutter_antplus.device.OnScanResultStreamHandlerImpl
import me.heyteacher.flutter_antplus.usb_adapter.UsbAntplusAdapterManager
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDeviceState
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDeviceType
import me.heyteacher.flutter_antplus.device.pigeons.OnBatteryStatusDataStreamHandler
import me.heyteacher.flutter_antplus.device.pigeons.OnDeviceStateChangeStreamHandler
import me.heyteacher.flutter_antplus.device.pigeons.OnScanResultStreamHandler
import me.heyteacher.flutter_antplus.logging.OnLogDataStreamHandlerImpl
import me.heyteacher.flutter_antplus.logging.pigeons.AntplusLogData
import me.heyteacher.flutter_antplus.logging.pigeons.AntplusLogLevel
import me.heyteacher.flutter_antplus.usb_adapter.pigeons.AntplusUsbAdapterState
import reactor.core.Disposable
import java.io.PrintWriter
import java.io.StringWriter
import java.util.function.Consumer

/**
 * Abstract implementation for scanning, connection, and telemetry subscriptions for an ANT+ device
 * (Bike Power Meter, Heart Rate Monitor, Cadence) with j-antplus Java library.
 *
 * Registers Pigeon event channel stream handlers for scan results, battery status, device status,
 * and logging.
 *
 * @param binaryMessenger the Flutter binary messenger for Pigeon channel registration
 * @param onLogDataStreamHandler the log data listener
 */
abstract class AbstractHostApi(
    binaryMessenger: BinaryMessenger,
    val context: Context,
    val onLogDataStreamHandler: OnLogDataStreamHandlerImpl,
    val onUsbAdapterStateChangeHandler: OnUsbAdapterStateChangeStreamHandlerImpl
) {
    companion object {
        private val _TAG: String = AbstractHostApi::class.java.simpleName

        /**
         * Logs a message to the native log listener.
         * @param tag the log tag
         * @param level the log level
         * @param message the log message
         * @param throwable the throwable
         * @param onLogDataStreamHandler the on log data stream handler
         */
        fun log(
            tag: String,
            level: AntplusLogLevel,
            message: String,
            throwable: Throwable? = null,
            onLogDataStreamHandler: OnLogDataStreamHandlerImpl?
        ) {
            if (throwable != null) {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                Handler(Looper.getMainLooper()).post {
                    onLogDataStreamHandler?.add(AntplusLogData(level, tag, "$message $sw"))
                }
                Log.e(tag, message, throwable)
            } else {
                Handler(Looper.getMainLooper()).post {
                    onLogDataStreamHandler?.add(AntplusLogData(level, tag, message))
                }
            }
        }
    }

    private var _connectedAntChannel: AntChannel? = null
    private var _scanAntChannel: AntChannel? = null
    private var _scanSubscription: Disposable? = null
    private var _connectSubscription: Disposable? = null
    protected val onScanResultStreamHandler: OnScanResultStreamHandlerImpl =
        OnScanResultStreamHandlerImpl()
    protected val onDeviceStateChangeStreamHandler: OnDeviceStateChangeStreamHandlerImpl =
        OnDeviceStateChangeStreamHandlerImpl()

    protected val onBatteryStatusDataStreamHandler: OnBatteryStatusDataStreamHandlerImpl = OnBatteryStatusDataStreamHandlerImpl()

    private val _scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Registers all Pigeon Host APIs and event stream handlers on initialization.
     *
     * Listen adapter detection in order to restart last connection or scan
     */
    init {
        OnScanResultStreamHandler.register(
            binaryMessenger, onScanResultStreamHandler, deviceType().name
        )
        OnDeviceStateChangeStreamHandler.register(
            binaryMessenger, onDeviceStateChangeStreamHandler, deviceType().name
        )
        OnBatteryStatusDataStreamHandler.register(binaryMessenger, onBatteryStatusDataStreamHandler, deviceType().name)
        // listen adapter detection
        _scope.launch {
            onUsbAdapterStateChangeHandler.flow.collect { antplusRequestAccessResult ->
                if (antplusRequestAccessResult == AntplusUsbAdapterState.ATTACHED) {
                    if (_connectedAntChannel != null) {
                        // restart last connection
                        connect(_connectedAntChannel!!.deviceNumber)
                    } else if (_scanAntChannel != null) {
                        // restart last scan
                        startScan()
                    }
                }
            }
        }
    }

    /**
     * Starts an async scan for nearby ANT+ Bike Power Meters.
     * */
    fun startScan() {
        log(tag = _TAG, level = AntplusLogLevel.DEBUG, message = "<startScan>:")
        try {
            if (!UsbAntplusAdapterManager.getInstance()
                    .checkPermission(context, callback = { startScan() })
            ) {
                log(
                    tag = _TAG,
                    level = AntplusLogLevel.DEBUG,
                    message = "(startScan): permission not granted, request permission"
                )
                return
            }
            log(
                tag = _TAG,
                level = AntplusLogLevel.DEBUG,
                message = "(startScan): permission granted"
            )
            closeScanChannel()
            _scanAntChannel = deviceChannel(null)
            _scanSubscription = _scanAntChannel!!.events
                .doOnNext(Consumer { antMessage: AntMessage -> this.handleScanMessage(antMessage) })
                .subscribe()

        } catch (t: Throwable) {
            log(tag = _TAG, level = AntplusLogLevel.ERROR, message = "(startScan):", throwable = t)
        }
    }

    /**
     * Stops scan device
     * */
    fun stopScan() {
        log(tag = _TAG, level = AntplusLogLevel.VERBOSE, message = "<stopScan>:")
        closeScanChannel()
    }

    /**
     * Connects to the device identified by [deviceNumber].
     * @param deviceNumber the ANT+ device number of the sensor to connect to
     */
    open fun connect(deviceNumber: Long) {
        log(
            tag = _TAG,
            level = AntplusLogLevel.DEBUG,
            message = "<connect>: deviceNumber $deviceNumber"
        )
        // stop scan
        try {
            if (!UsbAntplusAdapterManager.getInstance()
                    .checkPermission(context, callback = { connect(deviceNumber) })
            ) {
                log(
                    tag = _TAG,
                    level = AntplusLogLevel.DEBUG,
                    message = "(connect): deviceNumber $deviceNumber, permission not granted, request permission"
                )
                return
            }
            log(
                tag = _TAG,
                level = AntplusLogLevel.DEBUG,
                message = "(connect): deviceNumber $deviceNumber, permission granted"
            )
            closeScanChannel()
            closeConnectChannel()
            _connectedAntChannel = deviceChannel(deviceNumber)
            _connectSubscription =
                _connectedAntChannel!!.events
                    .doOnNext(Consumer { antMessage: AntMessage ->
                        this.handleConnectedMessage(antMessage)
                    })
                    .subscribe()
            Handler(Looper.getMainLooper()).post {
                onDeviceStateChangeStreamHandler.add(AntplusDeviceState.PROCESSING_REQUEST)
            }
        } catch (t: Throwable) {
            log(tag = _TAG, level = AntplusLogLevel.ERROR, message = "(connect):", throwable = t)
        }
    }

    /** Disconnects from the currently connected device. */
    open fun disconnect() {
        log(tag = _TAG, level = AntplusLogLevel.DEBUG, message = "<disconnect>:")
        try {
            closeConnectChannel()
            Handler(Looper.getMainLooper()).post {
                onDeviceStateChangeStreamHandler.add(AntplusDeviceState.CLOSED)
            }
        } catch (t: Throwable) {
            log(tag = _TAG, level = AntplusLogLevel.ERROR, message = "(disconnect):", throwable = t)
        }
    }

    /** Closes all active event stream listeners. Called when the plugin is detached from the engine. */
    open fun close() {
        // close device listeners
        onScanResultStreamHandler.onEventsDone()
        onDeviceStateChangeStreamHandler.onEventsDone()
        closeConnectChannel()
        closeScanChannel()
        _scanSubscription?.dispose()
        _connectSubscription?.dispose()
    }

    /**
     * Gets the Ant+ device type.
     * @return the Ant+ device type
     */
    protected abstract fun deviceType(): AntplusDeviceType

    /**
     * Handles data page from the connected device.
     * @param dataPage: the data page from the connected device
     */
    protected abstract fun handleConnectedDataPage(dataPage: AbstractAntPlusDataPage)

    /**
     * Handles data page during scan devices.
     * @param dataPage: the data page during scan devices
     */
    protected abstract fun handleScanDataPage(dataPage: AbstractAntPlusDataPage)

    /**
     * Gets the data page registry for the specific Ant+ device (Bike Power Meter, Heart Rate Monitor, Cadence).
     */
    protected abstract fun dataRegistry(): AbstractDataPageRegistry

    /**
     * Gets the device channel for the specific Ant+ device (Bike Power Meter, Heart Rate Monitor, Cadence).
     * @return the device channel for the specific Ant+ device (Bike Power Meter, Heart Rate Monitor, Cadence)
     */
    protected abstract fun deviceChannel(deviceNumber: Long?): AntChannel

    /**
     * Handles a message of a connected device
     * @param antMessage the message to be handled
     */
    protected fun handleConnectedMessage(antMessage: AntMessage) {
        try {
            Handler(Looper.getMainLooper()).post {
                onDeviceStateChangeStreamHandler.add(AntplusDeviceState.TRACKING)
            }
            if (antMessage is BroadcastDataMessage) {
                val payLoad = antMessage.payLoad
                removeToggleBit(payLoad)
                val dataPage = dataRegistry().constructDataPage(payLoad) ?: return
                handleConnectedDataPage(dataPage)
            }
        } catch (t: Throwable) {
            log(
                tag = _TAG,
                level = AntplusLogLevel.ERROR,
                message = "(handleConnectedMessage):",
                throwable = t
            )
        }
    }

    /**
     * Logs a message to the native log listener.
     * @param tag the log tag
     * @param level the log level
     * @param message the log message
     * @param throwable the throwable
     */
    protected fun log(
        tag: String,
        level: AntplusLogLevel,
        message: String,
        throwable: Throwable? = null
    ) {
        log(tag, level, message, throwable, onLogDataStreamHandler)
    }

    /**
     * Handles a message during scan in order to find devices
     * @param antMessage the message to handle during scan
     */
    private fun handleScanMessage(antMessage: AntMessage) {
        try {
            if (antMessage is BroadcastDataMessage) {
                val payLoad = antMessage.payLoad
                removeToggleBit(payLoad)
                val dataPage = dataRegistry().constructDataPage(payLoad) ?: return
                handleScanDataPage(dataPage)
            }
        } catch (t: Throwable) {
            log(
                tag = _TAG,
                level = AntplusLogLevel.ERROR,
                message = "(handleScanMessage):",
                throwable = t
            )
        }
    }

    private fun closeScanChannel() {
        try {
            _scanSubscription?.dispose()
            if (_scanAntChannel != null) {
                UsbAntplusAdapterManager.getInstance().closeChannel(_scanAntChannel!!.channelNumber)
                _scanAntChannel = null
            }
        } catch (t: Throwable) {
            log(
                tag = _TAG,
                level = AntplusLogLevel.ERROR,
                message = "(closeScanChannel):",
                throwable = t
            )
        } finally {
            _scanAntChannel = null
        }
    }

    private fun closeConnectChannel() {
        try {
            _connectSubscription?.dispose()
            if (_connectedAntChannel != null) {
                UsbAntplusAdapterManager.getInstance()
                    .closeChannel(_connectedAntChannel!!.channelNumber)
                _connectedAntChannel = null
            }
            _connectedAntChannel = null
        } catch (t: Throwable) {
            log(
                tag = _TAG,
                level = AntplusLogLevel.ERROR,
                message = "(closeConnectChannel):",
                throwable = t
            )
        } finally {
            _connectedAntChannel = null
        }
    }

    private fun removeToggleBit(payLoad: ByteArray) {
        payLoad[0] = (127 and payLoad[0].toInt()).toByte()
    }
}