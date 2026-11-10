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
import be.glever.ant.usb.AntUsbDevice
import be.glever.ant.usb.AntUsbDeviceFactory
import be.glever.antplus.common.datapage.AbstractAntPlusDataPage
import be.glever.antplus.common.datapage.registry.AbstractDataPageRegistry
import io.flutter.plugin.common.BinaryMessenger
import me.heyteacher.flutter_antplus.device.OnDeviceStateChangeListener
import me.heyteacher.flutter_antplus.device.OnRequestAccessResultListener
import me.heyteacher.flutter_antplus.device.OnScanResultListener
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDeviceState
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDeviceType
import me.heyteacher.flutter_antplus.device.pigeons.OnDeviceStateChangeStreamHandler
import me.heyteacher.flutter_antplus.device.pigeons.OnRequestAccessResultStreamHandler
import me.heyteacher.flutter_antplus.device.pigeons.OnScanResultStreamHandler
import me.heyteacher.flutter_antplus.logging.OnLogDataListener
import me.heyteacher.flutter_antplus.logging.pigeons.AntplusLogData
import me.heyteacher.flutter_antplus.logging.pigeons.AntplusLogEvent
import reactor.core.Disposable
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
 * @param onLogDataListener shared log listener to forward native log events to Flutter
 */
abstract class AbstractHostApi(
    binaryMessenger: BinaryMessenger,
    private val onLogDataListener: OnLogDataListener
)  {

    companion object {
        private lateinit var antUsbDevice: AntUsbDevice
        private val TAG: String = AbstractHostApi::class.java.simpleName
        fun initializeUsbDevice(context: Context, onLogDataListener: OnLogDataListener) {
            antUsbDevice = AntUsbDeviceFactory.getAvailableAntDevices(context).stream().findFirst().getOrElse {
                Handler(Looper.getMainLooper()).post {
                    /*
                    onRequestAccessResultListener.add(
                        AntplusRequestAccessResult.ADAPTER_NOT_DETECTED
                    )

                     */
                    Log.e(TAG, "(initializeUsbDevice): adapter not detected")
                }
                return
            }
            Handler(Looper.getMainLooper()).postDelayed({
                requestPermission(context, onLogDataListener)
            }, 3000)
        }
        @SuppressLint("UnspecifiedRegisterReceiverFlag")
        private fun requestPermission(context: Context, onLogDataListener: OnLogDataListener) {
            try {
                val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                val androidDevice = antUsbDevice.androidDevice
                val hasPermission = usbManager.hasPermission(androidDevice)
                Handler(Looper.getMainLooper()).post {
                    onLogDataListener.add(AntplusLogData(
                        AntplusLogEvent.INFO,
                        TAG,
                        "(requestPermission): device ${androidDevice.productName?.trim()} " +
                            "has permission $hasPermission"))
                }
                if (!hasPermission) {
                    context.registerReceiver(
                        usbReceiver,
                        IntentFilter("${context.packageName}.USB_PERMISSION")
                    )
                    usbManager.requestPermission(
                        androidDevice,
                        PendingIntent.getBroadcast(
                            context,
                            0,
                            Intent("${context.packageName}.USB_PERMISSION"),
                            PendingIntent.FLAG_IMMUTABLE)
                    )
                } else {
                    antUsbDevice.initialize(context)
                }
            } catch (t: Throwable) {
                Handler(Looper.getMainLooper()).post {
                    onLogDataListener.add(AntplusLogData(AntplusLogEvent.ERROR, TAG, "(requestPermission): ${t.message}"))
                }
                Log.e(TAG, "(requestPermission):", t)
                throw t
            }
        }
        private val usbReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                try {
                    if ("${context.packageName}.USB_PERMISSION" == intent.action) {
                        synchronized(this) {
                            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                            if (usbManager.hasPermission(antUsbDevice.androidDevice)) {
                                Log.i(TAG,"(BroadcastReceiver.onReceive): " +
                                        "device ${antUsbDevice.androidDevice.productName?.trim()} " +
                                        "has permission true")
                                antUsbDevice.androidDevice.apply {
                                    Log.i(TAG,"(BroadcastReceiver.onReceive): initialize ant usb device")
                                    antUsbDevice.initialize(context)
                                }
                            }
                            else {
                                Log.w(TAG,"(BroadcastReceiver.onReceive): " +
                                        "device ${antUsbDevice.androidDevice.productName?.trim()} " +
                                        "has permission false")
                            }
                        }
                    }
                } catch (e: AntException) {
                    Log.e(TAG,"(BroadcastReceiver.onReceive):", e)
                }
            }
        }
    }

    private var connectChannelNumber: Byte? = null
    private var scanChannelNumber: Byte? = null
    private var scanSubscription: Disposable? = null
    private var connectSubscription: Disposable? = null
    protected val onScanResultListener: OnScanResultListener = OnScanResultListener()
    protected val onRequestAccessResultListener: OnRequestAccessResultListener =
        OnRequestAccessResultListener()
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
        OnRequestAccessResultStreamHandler.register(
            binaryMessenger, onRequestAccessResultListener, deviceType().name
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
    protected fun getAntUsbDevice(): AntUsbDevice { return antUsbDevice }

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
        log(tag = TAG, level = AntplusLogEvent.VERBOSE, message = "<startScan>:")
        try {
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
        log(tag = TAG, level = AntplusLogEvent.VERBOSE, message = "<connect>: deviceNumber $deviceNumber")
        // stop scan
        try {
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
                antUsbDevice.closeChannel(scanChannelNumber!!)
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
                antUsbDevice.closeChannel(connectChannelNumber!!)
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
            Handler(Looper.getMainLooper()).post {
                onLogDataListener.add(AntplusLogData(level, tag, "$message ${throwable.message}"))
            }
            Log.e(tag, message, throwable)
        }
        else {
            Handler(Looper.getMainLooper()).post {
                onLogDataListener.add(AntplusLogData(level, tag, message))
            }
        }
    }

    /** Closes all active event stream listeners. Called when the plugin is detached from the engine. */
    open fun close() {
        // close device listeners
        onScanResultListener.onEventsDone()
        onRequestAccessResultListener.onEventsDone()
        onDeviceStateChangeListener.onEventsDone()
        onLogDataListener.onEventsDone()
        closeConnectChannel()
        closeScanChannel()
        antUsbDevice.close()
        scanSubscription?.dispose()
        connectSubscription?.dispose()
    }

    private fun removeToggleBit(payLoad: ByteArray) {
        payLoad[0] = (127 and payLoad[0].toInt()).toByte()
    }
}