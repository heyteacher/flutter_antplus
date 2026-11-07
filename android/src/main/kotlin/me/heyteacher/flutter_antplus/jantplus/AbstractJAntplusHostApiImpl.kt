package me.heyteacher.flutter_antplus.jantplus

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
import be.glever.antplus.common.datapage.DataPage81ProductInformation
import be.glever.antplus.common.datapage.registry.CommonDataPageRegistry
import be.glever.antplus.power.PowerChannel
import io.flutter.plugin.common.BinaryMessenger
import me.heyteacher.flutter_antplus.device.OnDeviceStateChangeListener
import me.heyteacher.flutter_antplus.device.OnRequestAccessResultListener
import me.heyteacher.flutter_antplus.device.OnScanResultListener
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDevice
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
 *
 * Abstract implementation for scanning, connection, and telemetry subscriptions for an ANT+ device
 * (Bike Power Meter, Heart Rate Monitor, Cadence) with j-antplus  Java library
 * Registers Pigeon event channel stream handlers for scan results, battery status, device status, and logging.
 *
 * @param binaryMessenger the Flutter binary messenger for Pigeon channel registration
 * @param onLogDataListener shared log listener to forward native log events to Flutter
 */
abstract class AbstractJAntplusHostApiImpl(
    binaryMessenger: BinaryMessenger,
    private val onLogDataListener: OnLogDataListener
)  {

    companion object {
        private lateinit var antUsbDevice: AntUsbDevice
        private val TAG: String = AbstractJAntplusHostApiImpl::class.java.simpleName
        fun initializeUsbDevice(context: Context): AntUsbDevice? {
            antUsbDevice = AntUsbDeviceFactory.getAvailableAntDevices(context).stream().findFirst().getOrElse {
                Handler(Looper.getMainLooper()).post {
                    /*
                    onRequestAccessResultListener.add(
                        AntplusRequestAccessResult.ADAPTER_NOT_DETECTED
                    )

                     */
                    Log.e(TAG, "(initializeUsbDevice): adapter not detected")
                }
                return null
            }
            Handler(Looper.getMainLooper()).postDelayed({
                requestPermission(context)
            }, 3000)
            return antUsbDevice
        }
        @SuppressLint("UnspecifiedRegisterReceiverFlag")
        private fun requestPermission(context: Context) {
            try {
                val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                val androidDevice = antUsbDevice.androidDevice
                val hasPermission = usbManager.hasPermission(androidDevice)
                Log.i(TAG, "(requestPermission): device ${androidDevice.productName?.trim()} has permission $hasPermission")
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
                Log.e(TAG, "(requestPermission): error", t)
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
                                Log.i(TAG,"(BroadcastReceiver.onReceive): device ${antUsbDevice.androidDevice.productName} has permission true")
                                antUsbDevice.androidDevice.apply {
                                    Log.i(TAG,"(BroadcastReceiver.onReceive): initialize ant usb device")
                                    antUsbDevice.initialize(context)
                                }
                            }
                            else {
                                Log.w(TAG,"(BroadcastReceiver.onReceive): device ${antUsbDevice.androidDevice.productName} has permission false")
                            }
                        }
                    }
                } catch (e: AntException) {
                    Log.e(TAG,"error", e)
                }
            }
        }
    }

    private var connectChannelNumber: Byte? = null
    private var scanChannelNumber: Byte? = null
    private var scanSubscription: Disposable? = null
    private var connectSubscription: Disposable? = null
    private val commonDataPageRegistry = CommonDataPageRegistry()
    private val onScanResultListener: OnScanResultListener = OnScanResultListener()
    private val onRequestAccessResultListener: OnRequestAccessResultListener =
        OnRequestAccessResultListener()
    protected val onDeviceStateChangeListener: OnDeviceStateChangeListener =
        OnDeviceStateChangeListener()
    /**
     * Registers all Pigeon Host APIs and event stream handlers on initialization.
     */
    init {
        OnScanResultStreamHandler.register(
            binaryMessenger, onScanResultListener, AntplusDeviceType.BIKEPOWER.name
        )
        OnDeviceStateChangeStreamHandler.register(
            binaryMessenger, onDeviceStateChangeListener, AntplusDeviceType.BIKEPOWER.name
        )
        OnRequestAccessResultStreamHandler.register(
            binaryMessenger, onRequestAccessResultListener, AntplusDeviceType.BIKEPOWER.name
        )
    }

    protected fun getAntUsbDevice(): AntUsbDevice { return antUsbDevice }

    /** Starts an async scan for nearby ANT+ Bike Power Meters. */
    fun startScan() {
        Handler(Looper.getMainLooper()).post {
            onLogDataListener.add(
                AntplusLogData(
                    AntplusLogEvent.VERBOSE, TAG, "<startScan>:"
                )
            )
        }
        try {
            stopScan()
            val scanChannel = PowerChannel(antUsbDevice)
            scanChannelNumber = scanChannel.channelNumber
            scanSubscription = scanChannel.events
                .doOnNext(Consumer { antMessage: AntMessage -> this.handleScanMessage(antMessage) }).subscribe()

        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                onLogDataListener.add(
                    AntplusLogData(
                        AntplusLogEvent.ERROR,
                        TAG,
                        "(startScan): error " + e.message
                    )
                )
            }
        }
    }

    /** Stops scan device */
    fun stopScan() {
        Handler(Looper.getMainLooper()).post {
            onLogDataListener.add(
                AntplusLogData(
                    AntplusLogEvent.VERBOSE, TAG, "<stopScan>:"
                )
            )
        }
        scanSubscription?.dispose()
        //antUsbDevice.close()
        if (scanChannelNumber != null) {
            antUsbDevice.closeChannel(scanChannelNumber!!)
            scanChannelNumber = null
        }
    }

    /**
     * Connects to the device identified by [deviceNumber].
     * @param deviceNumber the ANT+ device number of the sensor to connect to
     */
    fun connect(deviceNumber: Long) {
        Handler(Looper.getMainLooper()).post {
            onLogDataListener.add(
                AntplusLogData(
                    AntplusLogEvent.VERBOSE, TAG, "<connect>: deviceNumber $deviceNumber"
                )
            )
        }
        // stop scan
        stopScan()
        disconnect()
        val deviceChannel = deviceChannel()
        connectChannelNumber = deviceChannel.channelNumber
        connectSubscription =
            deviceChannel.events
            .doOnNext(Consumer { antMessage: AntMessage -> this.handleConnectedMessage(antMessage)
            })
            .subscribe()
        Handler(Looper.getMainLooper()).post {
            onDeviceStateChangeListener.add(AntplusDeviceState.PROCESSING_REQUEST)
        }
    }

    protected abstract fun deviceChannel(): AntChannel

    /** Disconnects from the currently connected device. */
    fun disconnect() {
        Handler(Looper.getMainLooper()).post {
            onLogDataListener.add(
                AntplusLogData(
                    AntplusLogEvent.VERBOSE, TAG, "<disconnect>:"
                )
            )
        }
        connectSubscription?.dispose()
        if (connectChannelNumber != null) {
            antUsbDevice.closeChannel(connectChannelNumber!!)
            connectChannelNumber = null
        }
        Handler(Looper.getMainLooper()).post {
            onDeviceStateChangeListener.add(AntplusDeviceState.CLOSED)
        }
    }

    /** Closes all active event stream listeners. Called when the plugin is detached from the engine. */
    open fun close() {
        // close device listeners
        onScanResultListener.onEventsDone()
        onRequestAccessResultListener.onEventsDone()
        onDeviceStateChangeListener.onEventsDone()
        onLogDataListener.onEventsDone()
        disconnect()
        stopScan()
        antUsbDevice.close()
        scanSubscription?.dispose()
        connectSubscription?.dispose()
    }
    private fun handleScanMessage(antMessage:AntMessage ) {
        try {
            if (antMessage is BroadcastDataMessage) {
                val payLoad = antMessage.payLoad
                removeToggleBit(payLoad)
                val dataPage = commonDataPageRegistry.constructDataPage(payLoad) ?: return
                if (dataPage is DataPage81ProductInformation) {
                    Handler(Looper.getMainLooper()).post {
                        onLogDataListener.add(
                            AntplusLogData(
                                AntplusLogEvent.INFO,
                                TAG,
                                "(handleScan): serial number ${dataPage.serialNumber} name ${dataPage.name}"
                            )
                        )
                        onScanResultListener.add(
                            AntplusDevice(
                                dataPage.serialNumber,
                                dataPage.name,
                                AntplusDeviceType.BIKEPOWER
                            )
                        )
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "(handleScan): error ", e)
        }
    }

    protected abstract fun handleConnectedMessage(antMessage:AntMessage )
    protected fun removeToggleBit(payLoad: ByteArray) {
        payLoad[0] = (127 and payLoad[0].toInt()).toByte()
    }
}