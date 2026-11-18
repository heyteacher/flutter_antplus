package me.heyteacher.flutter_antplus

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import be.glever.ant.AntException
import be.glever.ant.channel.AntChannel
import be.glever.ant.message.AntMessage
import be.glever.ant.message.data.BroadcastDataMessage
import be.glever.ant.usb.impl.AndroidAntUsbDevice
import be.glever.ant.usb.AndroidAntUsbDeviceFactory
import be.glever.antplus.common.datapage.AbstractAntPlusDataPage
import be.glever.antplus.common.datapage.registry.AbstractDataPageRegistry
import io.flutter.plugin.common.BinaryMessenger
import me.heyteacher.flutter_antplus.device.OnDeviceStateChangeListener
import me.heyteacher.flutter_antplus.device.OnRequestAccessListener
import me.heyteacher.flutter_antplus.device.OnScanResultListener
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDeviceState
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDeviceType
import me.heyteacher.flutter_antplus.device.pigeons.AntplusRequestAccessResult
import me.heyteacher.flutter_antplus.device.pigeons.OnDeviceStateChangeStreamHandler
import me.heyteacher.flutter_antplus.device.pigeons.OnScanResultStreamHandler
import me.heyteacher.flutter_antplus.logging.OnLogDataListener
import me.heyteacher.flutter_antplus.logging.OnLogDataListenerLogger
import me.heyteacher.flutter_antplus.logging.pigeons.AntplusLogData
import me.heyteacher.flutter_antplus.logging.pigeons.AntplusLogEvent
import reactor.core.Disposable
import java.io.PrintWriter
import java.io.StringWriter
import java.util.function.Consumer
import kotlin.jvm.optionals.getOrElse

/**
 * Abstract implementation for scanning, connection, and telemetry subscriptions for an ANT+ device
 * (Bike Power Meter, Heart Rate Monitor, Cadence) with j-antplus Java library.
 *
 * Initializes Ant+ Stick USB device requesting permission from the user.
 *
 * Registers Pigeon event channel stream handlers for scan results, battery status, device status,
 * and logging.
 *
 * @param binaryMessenger the Flutter binary messenger for Pigeon channel registration
 */
abstract class AbstractHostApi(
    binaryMessenger: BinaryMessenger,
    val context: Context,
)  {

    companion object {
        private var antUsbDevice: AndroidAntUsbDevice? = null
        private val TAG: String = AbstractHostApi::class.java.simpleName
        var onLogDataListener: OnLogDataListener? = null

        var onRequestAccessListener: OnRequestAccessListener? = null

        fun initializeUsbDevice(
            context: Context,
            onLogDataListener: OnLogDataListener,
            onRequestAccessListener: OnRequestAccessListener
        ) {
            AbstractHostApi.onLogDataListener = onLogDataListener
            AbstractHostApi.onRequestAccessListener = onRequestAccessListener
            antUsbDevice = AndroidAntUsbDeviceFactory.getAvailableAntDevices(
                context,
                OnLogDataListenerLogger(onLogDataListener))
                .stream().findFirst().getOrElse {
                    Handler(Looper.getMainLooper()).postDelayed({
                        onRequestAccessListener.add(
                            AntplusRequestAccessResult.ADAPTER_NOT_DETECTED
                        )
                        Log.e(TAG, "(initializeUsbDevice): adapter not detected")
                    }, 3000)
                return
            }
            Handler(Looper.getMainLooper()).postDelayed({
                requestPermission(context)
            }, 3000)
        }

        var requestPermissionCallback: (()->Unit)? = null
        @SuppressLint("UnspecifiedRegisterReceiverFlag")
        private fun requestPermission(context: Context) {
            try {
                val androidDevice = antUsbDevice!!.androidDevice
                val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                val hasPermission = usbManager.hasPermission(androidDevice)
                Handler(Looper.getMainLooper()).post {
                    onLogDataListener?.add(AntplusLogData(
                        AntplusLogEvent.DEBUG,
                        TAG,
                        "(requestPermission): device ${androidDevice.productName?.trim()} " +
                            "has ${usbPermission(context)} $hasPermission"))
                }
                if (!hasPermission) {
                    context.registerReceiver(
                        usbReceiver,
                        IntentFilter(usbPermission(context))
                    )
                    usbManager.requestPermission(
                        androidDevice,
                        PendingIntent.getBroadcast(
                            context,
                            0,
                            Intent(usbPermission(context)),
                            PendingIntent.FLAG_IMMUTABLE)
                    )
                } else {
                    antUsbDevice!!.initialize()
                    requestPermissionCallback?.invoke()
                    requestPermissionCallback = null
                }
            } catch (t: Throwable) {
                Handler(Looper.getMainLooper()).post {
                    onLogDataListener?.add(AntplusLogData(AntplusLogEvent.ERROR, TAG, "(requestPermission): ${t.message}"))
                }
                Log.e(TAG, "(requestPermission):", t)
                throw t
            }
        }

        private fun hasPermission(context: Context): Boolean {
            if (antUsbDevice != null) {
                val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                return usbManager.hasPermission(antUsbDevice!!.androidDevice)
            } else {
                return false
            }
        }

        private val usbReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                try {
                    if (usbPermission(context) == intent.action) {
                        synchronized(this) {
                            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                            if (usbManager.hasPermission(antUsbDevice!!.androidDevice)) {
                                Log.d(TAG,"(BroadcastReceiver.onReceive): " +
                                        "device ${antUsbDevice!!.androidDevice.productName?.trim()} " +
                                        "has ${usbPermission(context)} true")
                                antUsbDevice!!.androidDevice.apply {
                                    Log.i(TAG,"(BroadcastReceiver.onReceive): initialize ant usb device")
                                    antUsbDevice!!.initialize()
                                    requestPermissionCallback?.invoke()
                                    requestPermissionCallback = null
                                }
                            }
                            else {
                                Handler(Looper.getMainLooper()).post {
                                    onRequestAccessListener?.add(
                                        AntplusRequestAccessResult.USER_CANCELLED
                                    )
                                    onLogDataListener?.add(AntplusLogData(
                                        AntplusLogEvent.WARNING,
                                        TAG,
                                        "(BroadcastReceiver.onReceive): " +
                                            "permission NOT granted for device ${antUsbDevice!!.androidDevice.productName?.trim()}"))
                                }
                            }
                        }
                    }
                } catch (e: AntException) {
                    Handler(Looper.getMainLooper()).post {
                        onRequestAccessListener?.add(
                            AntplusRequestAccessResult.USER_CANCELLED
                        )
                        onLogDataListener?.add(AntplusLogData(
                            AntplusLogEvent.ERROR,
                            TAG,
                            "(BroadcastReceiver.onReceive): " +
                                    "error ${e.message}"))
                    }
                    Log.e(TAG, "(BroadcastReceiver.onReceive):", e)
                }
            }
        }
        private fun usbPermission(context: Context): String = "${context.packageName}.USB_PERMISSION"
    }

    private var connectChannelNumber: Byte? = null
    private var scanChannelNumber: Byte? = null
    private var scanSubscription: Disposable? = null
    private var connectSubscription: Disposable? = null
    protected val onScanResultListener: OnScanResultListener = OnScanResultListener()
    protected val onDeviceStateChangeListener: OnDeviceStateChangeListener =
        OnDeviceStateChangeListener()

    /**
     * Registers all Pigeon Host APIs and event stream handlers on initialization.
     */
    init {
        OnScanResultStreamHandler.register(
            binaryMessenger, onScanResultListener, deviceType().name
        )
        OnDeviceStateChangeStreamHandler.register(
            binaryMessenger, onDeviceStateChangeListener, deviceType().name
        )
    }

    /**
     * Gets the Ant+ device type.
     * @return the Ant+ device type
     */
    abstract fun deviceType(): AntplusDeviceType

    /**
     * Gets the Ant+ Stick USB device.
     * @return the Ant+ Stick USB device
     */
    protected fun getAntUsbDevice(): AndroidAntUsbDevice? { return antUsbDevice }

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
    protected abstract fun deviceChannel(): AntChannel

    protected fun handleConnectedMessage(antMessage: AntMessage) {
        try {
            Handler(Looper.getMainLooper()).post {
                onDeviceStateChangeListener.add(AntplusDeviceState.TRACKING)
            }
            if (antMessage is BroadcastDataMessage) {
                val payLoad = antMessage.payLoad
                removeToggleBit(payLoad)
                val dataPage = dataRegistry().constructDataPage(payLoad) ?: return
                handleConnectedDataPage(dataPage)
            }
        } catch (t: Throwable) {
            log(tag = TAG, level = AntplusLogEvent.ERROR, message = "(handleConnectedMessage):", throwable = t)
        }
    }

    /** Starts an async scan for nearby ANT+ Bike Power Meters. */
    fun startScan() {
        log(tag = TAG, level = AntplusLogEvent.DEBUG, message = "<startScan>:")
        try {
            if (!hasPermission(context)) {
                requestPermissionCallback = { startScan() }
                requestPermission(context)
                return
            }
            closeScanChannel()
            val scanChannel = deviceChannel()
            scanChannelNumber = scanChannel.channelNumber
            scanSubscription = scanChannel.events
                .doOnNext(Consumer { antMessage: AntMessage -> this.handleScanMessage(antMessage) }).subscribe()

        } catch (t: Throwable) {
            log(tag = TAG, level = AntplusLogEvent.ERROR, message = "(startScan):", throwable = t)
        }
    }

    /** Stops scan device */
    fun stopScan() {
        log(tag = TAG, level = AntplusLogEvent.VERBOSE, message = "<stopScan>:")
        closeScanChannel()
    }

    /**
     * Connects to the device identified by [deviceNumber].
     * @param deviceNumber the ANT+ device number of the sensor to connect to
     */
    open fun connect(deviceNumber: Long) {
        log(tag = TAG, level = AntplusLogEvent.DEBUG, message = "<connect>: deviceNumber $deviceNumber")
        // stop scan
        try {
            if (!hasPermission(context)) {
                requestPermissionCallback = { connect(deviceNumber) }
                requestPermission(context)
                return
            }
            closeScanChannel()
            closeConnectChannel()
            val deviceChannel = deviceChannel()
            connectChannelNumber = deviceChannel.channelNumber
            connectSubscription =
                deviceChannel.events
                .doOnNext(Consumer { antMessage: AntMessage ->
                    this.handleConnectedMessage(antMessage)
                })
                .subscribe()
            Handler(Looper.getMainLooper()).post {
                onDeviceStateChangeListener.add(AntplusDeviceState.PROCESSING_REQUEST)
            }
        } catch (t: Throwable) {
            log(tag = TAG, level = AntplusLogEvent.ERROR, message = "(connect):", throwable = t)
        }
    }

    /** Disconnects from the currently connected device. */
    open fun disconnect() {
        log(tag = TAG, level = AntplusLogEvent.VERBOSE, message = "<disconnect>:")
        try {
            closeConnectChannel()
            Handler(Looper.getMainLooper()).post {
                onDeviceStateChangeListener.add(AntplusDeviceState.CLOSED)
            }
        } catch (t: Throwable) {
            log(tag = TAG, level = AntplusLogEvent.ERROR, message = "(disconnect):", throwable = t)
        }
    }

    private fun handleScanMessage(antMessage: AntMessage) {
        try {
            if (antMessage is BroadcastDataMessage) {
                val payLoad = antMessage.payLoad
                removeToggleBit(payLoad)
                val dataPage = dataRegistry().constructDataPage(payLoad) ?: return
                handleScanDataPage(dataPage)
            }
        } catch (t: Throwable) {
            log(tag = TAG, level = AntplusLogEvent.ERROR, message = "(handleScanMessage):", throwable = t)
        }
    }

    private fun closeScanChannel() {
        try {
            scanSubscription?.dispose()
            if (scanChannelNumber != null) {
                antUsbDevice?.closeChannel(scanChannelNumber!!)
                scanChannelNumber = null
            }
        } catch (t: Throwable) {
            log(tag = TAG, level = AntplusLogEvent.ERROR, message = "(closeScanChannel):", throwable = t)
        }
        finally {
            scanChannelNumber = null
        }
    }

    private fun closeConnectChannel() {
        try {
            connectSubscription?.dispose()
            if (connectChannelNumber != null) {
                antUsbDevice?.closeChannel(connectChannelNumber!!)
                connectChannelNumber = null
            }
        } catch (t: Exception) {
            log(tag = TAG, level = AntplusLogEvent.ERROR, message = "(closeConnectChannel):", throwable = t)
        }
        finally {
            connectChannelNumber = null
        }
    }

    /**
     * Logs a message to the native log listener.
     * @param tag the log tag
     * @param level the log level
     * @param message the log message
     */
    protected fun log(tag: String, level: AntplusLogEvent, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            Handler(Looper.getMainLooper()).post {
                onLogDataListener?.add(AntplusLogData(level, tag, "$message $sw"))
            }
            Log.e(tag, message, throwable)
        }
        else {
            Handler(Looper.getMainLooper()).post {
                onLogDataListener?.add(AntplusLogData(level, tag, message))
            }
        }
    }

    /** Closes all active event stream listeners. Called when the plugin is detached from the engine. */
    open fun close() {
        // close device listeners
        onScanResultListener.onEventsDone()
        onDeviceStateChangeListener.onEventsDone()
        onRequestAccessListener?.onEventsDone()
        onLogDataListener?.onEventsDone()
        closeConnectChannel()
        closeScanChannel()
        antUsbDevice?.close()
        scanSubscription?.dispose()
        connectSubscription?.dispose()
    }

    private fun removeToggleBit(payLoad: ByteArray) {
        payLoad[0] = (127 and payLoad[0].toInt()).toByte()
    }
}