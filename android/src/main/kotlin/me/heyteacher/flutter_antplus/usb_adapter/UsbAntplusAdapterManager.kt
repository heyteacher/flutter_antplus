package me.heyteacher.flutter_antplus.usb_adapter

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
import be.glever.ant.usb.AndroidAntUsbDeviceFactory
import be.glever.ant.usb.impl.AndroidAntUsbDevice
import me.heyteacher.flutter_antplus.usb_adapter.pigeons.AntplusUsbAdapterState
import me.heyteacher.flutter_antplus.logging.OnLogDataStreamHandlerImpl
import me.heyteacher.flutter_antplus.logging.OnLogDataStreamLogger
import me.heyteacher.flutter_antplus.logging.pigeons.AntplusLogData
import me.heyteacher.flutter_antplus.logging.pigeons.AntplusLogEvent
import kotlin.jvm.optionals.getOrElse

/**
 * Singleton manager responsible for the lifecycle, permission requests, and broadcast receivers
 * of the ANT+ USB stick hardware adapter.
 *
 * Handles detection of attached USB ANT+ hardware adapters, manages USB permissions, enqueues
 * pending permission callbacks, and responds to USB adapter attach and detach broadcast events.
 */
class UsbAntplusAdapterManager private constructor() {

    companion object {
        private val TAG: String = UsbAntplusAdapterManager::class.java.simpleName

        @Volatile
        private var instance: UsbAntplusAdapterManager? = null

        /**
         * Gets singleton instance
         * @return the singleton instance
         */
        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: UsbAntplusAdapterManager().also { instance = it }
            }
    }

    private var _usbAntplusAdapter: AndroidAntUsbDevice? = null
    private var _onLogDataStreamHandler: OnLogDataStreamHandlerImpl? = null
    private var _onUsbAdapterStateChangeStreamHandler: OnUsbAdapterStateChangeStreamHandlerImpl? = null
    private val _pendingCallbacks: MutableList<(() -> Unit)?> = mutableListOf()

    /**
     * Gets the android ant usb device
     * @return the android ant usb device
     */
    fun getAntplusUsbAdapter(): AndroidAntUsbDevice? {
        return _usbAntplusAdapter
    }

    /**
     * Initializes the android ant usb devices and register request permission, attach and detach
     * device broadcast receiver
     * @param onLogDataListener the log listener
     * @param onRequestAccessListener the request access listener
     */
    fun initializeListeners(
        onLogDataListener: OnLogDataStreamHandlerImpl?,
        onRequestAccessListener: OnUsbAdapterStateChangeStreamHandlerImpl?
    ) {
        Log.d(TAG, "<initializeListeners>:")
        // set the listeners
        this._onLogDataStreamHandler = onLogDataListener
        this._onUsbAdapterStateChangeStreamHandler = onRequestAccessListener
    }

    /**
     * Initializes the android ant usb devices and register request permission, attach and detach
     * device broadcast receiver
     * @param context the application context
     */
    fun initializeUsbAdapter(
        context: Context,
    ) {
        Log.d(TAG, "<initializeUsbAdapter>:")
        // close ant usb device if was already initialized
        if (this._usbAntplusAdapter != null) {
            Log.d(
                TAG,
                "(initializeUsbAdapter): ${usbAntplusAdapterName()} already initialized, do nothing"
            )
            return
        }
        // register USB_PERMISSION, USB_DEVICE_ATTACHED and USB_DEVICE_DETACHED broadcast message
        val intentFilter = IntentFilter()
        intentFilter.addAction(usbPermission(context))
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        Log.d(
            TAG,
            "(initializeUsbAdapter): register usb receiver (permission, attach and detach intents"
        )
        context.registerReceiver(
            usbReceiver,
            intentFilter
        )
        // set ant usb device as the first available device if found
        this._usbAntplusAdapter = AndroidAntUsbDeviceFactory.getAvailableAntDevices(
            context,
            OnLogDataStreamLogger(this._onLogDataStreamHandler!!, "j-antplus")
        )
            .stream().findFirst().getOrElse {
                // no device available, yield and adapter not detected message
                _onUsbAdapterStateChangeStreamHandler?.add(
                    AntplusUsbAdapterState.DETACHED
                )
                Log.e(TAG, "(initializeUsbAdapter): adapter not detected")
                return
            }
        Log.d(TAG, "(initializeUsbAdapter): ${usbAntplusAdapterName()} found")
        // device found, if user grants permission, initialize it
        if (hasPermission(context)) {
            Log.d(
                TAG,
                "(initializeUsbAdapter): permission granted, initialize ${usbAntplusAdapterName()}"
            )
            _usbAntplusAdapter!!.initialize()
            invokePendingCallbacks()
        }
    }

    /**
     * Checks if user has granted permission on usb device. If permission is not granted, enqueue
     * the callback in order to execute when permission will be granted
     * @param context the Application context
     * @param callback the callback to be executed when permission will be granted
     * @return true if user has grant permission, false otherwise
     */
    fun checkPermission(context: Context, callback: (() -> Unit)): Boolean {
        Log.d(TAG, "<checkPermission>:")
        if (_usbAntplusAdapter == null) {
            initializeUsbAdapter(context)
        }
        if (_usbAntplusAdapter != null) {
            // check is user has granted permission
            if (hasPermission(context)) {
                Log.d(
                    TAG,
                    "(checkPermission): ${usbAntplusAdapterName()} found and permission granted, returns true"
                )
                return true
            }
            // permission not granted, request permission
            else {
                val permissionNotAlreadyRequested = _pendingCallbacks.isEmpty()
                // enqueue callback in order to execute when permission is granted
                _pendingCallbacks.add(callback)
                // request permission only the first time (requestPermissionCallbacks is empty)
                if (permissionNotAlreadyRequested) {
                    Log.d(
                        TAG,
                        "(checkPermission): ${usbAntplusAdapterName()} found, permission NOT granted " +
                                "and not already requested. Request permission and returns false"
                    )
                    requestPermission(context)
                } else {
                    Log.d(
                        TAG,
                        "(checkPermission): ${usbAntplusAdapterName()} found, permission NOT granted " +
                                "and already requested. Returns false"
                    )
                }
                return false
            }
        } else {
            Log.d(TAG, "(checkPermission): usb adapter not found, returns false")
            return false
        }
    }

    /**
     * Closes the channel identified by channelNumber
     * @param channelNumber the number of channel to close
     */
    fun closeChannel(channelNumber: Byte) {
        Log.d(TAG, "<checkPermission>: channelNumber $channelNumber")
        _usbAntplusAdapter?.closeChannel(channelNumber)
    }

    /**
     * Closes listeners and android ant usb device
     */
    fun close() {
        Log.d(TAG, "<close>:")
        // close device listeners
        _usbAntplusAdapter?.close()
    }

    private fun usbAntplusAdapterName(): String =
        "usb adapter ${_usbAntplusAdapter?.androidDevice?.productName?.trim()}"

    private fun usbPermission(context: Context): String = "${context.packageName}.USB_PERMISSION"

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun requestPermission(context: Context) {
        Log.d(TAG, "<requestPermission>:")
        try {
            val androidDevice = _usbAntplusAdapter!!.androidDevice
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val hasPermission = hasPermission(context)
            Log.d(
                TAG,
                "(requestPermission): has permission on ${usbAntplusAdapterName()} $hasPermission"
            )
            if (!hasPermission) {
                Log.d(
                    TAG,
                    "(requestPermission): permission not granted, request permission on usb manager"
                )
                usbManager.requestPermission(
                    androidDevice,
                    PendingIntent.getBroadcast(
                        context,
                        0,
                        Intent(usbPermission(context)),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
            } else {
                Log.d(
                    TAG,
                    "(requestPermission): permission granted, initialize ${usbAntplusAdapterName()}"
                )
                _usbAntplusAdapter!!.initialize()
                invokePendingCallbacks()
            }
        } catch (t: Throwable) {
            Handler(Looper.getMainLooper()).post {
                _onLogDataStreamHandler?.add(
                    AntplusLogData(
                        AntplusLogEvent.ERROR,
                        TAG,
                        "(requestPermission): ${t.message}"
                    )
                )
            }
            Log.e(TAG, "(requestPermission):", t)
            throw t
        }
    }

    private fun hasPermission(context: Context): Boolean {
        Log.d(TAG, "<hasPermission>:")
        if (_usbAntplusAdapter == null) {
            Log.d(TAG, "(hasPermission): usb adapter not found, returns false")
            return false
        }
        val androidDevice = _usbAntplusAdapter!!.androidDevice
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val hasPermission = usbManager.hasPermission(androidDevice)
        Log.d(TAG, "(hasPermission):  ${usbAntplusAdapterName()} found, returns $hasPermission")
        return hasPermission
    }

    private fun invokePendingCallbacks() {
        Log.d(TAG, "<invokePendingCallbacks>:")
        try {
            Log.d(
                TAG,
                "(invokePendingCallbacks): invoke ${_pendingCallbacks.count()} pending callbacks"
            )
            for (requestPermissionCallback in _pendingCallbacks) {
                requestPermissionCallback?.invoke()
            }
        } catch (t: Exception) {
            Handler(Looper.getMainLooper()).post {
                _onLogDataStreamHandler?.add(
                    AntplusLogData(
                        AntplusLogEvent.ERROR,
                        TAG,
                        "(invokePendingCallbacks): ${t.message}"
                    )
                )
            }
            Log.e(TAG, "(invokePendingCallbacks):", t)
        } finally {
            Log.d(TAG, "(invokePendingCallbacks): clear pending callbacks")
            _pendingCallbacks.clear()
        }
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                Log.d(TAG, "<onReceive>: action ${intent.action}")
                when (intent.action) {
                    // usb device attached, initialize
                    UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                        Log.d(
                            TAG, "(onReceive): ${usbAntplusAdapterName()} attached, initialize"
                        )
                        if (_usbAntplusAdapter == null) {
                            initializeUsbAdapter(context)
                        }
                        if (hasPermission(context)) {
                            _usbAntplusAdapter!!.initialize()
                            invokePendingCallbacks()
                        }
                        _onUsbAdapterStateChangeStreamHandler?.add(AntplusUsbAdapterState.ATTACHED)
                    }
                    // usb device detached, close
                    UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                        Log.d(
                            TAG, "(onReceive): ${usbAntplusAdapterName()} detached, close it"
                        )
                        close()
                        _onUsbAdapterStateChangeStreamHandler?.add(
                            AntplusUsbAdapterState.DETACHED
                        )
                    }
                    // usb permission intent
                    usbPermission(context) -> {
                        synchronized(this) {
                            if (hasPermission(context)) {
                                Log.d(
                                    TAG,
                                    "(onReceive): permission granted on ${usbAntplusAdapterName()}"
                                )
                                _usbAntplusAdapter?.androidDevice.apply {
                                    Log.d(TAG, "(onReceive): initialize ${usbAntplusAdapterName()}")
                                    _usbAntplusAdapter!!.initialize()
                                    invokePendingCallbacks()
                                }
                            } else {
                                Log.w(
                                    TAG,
                                    "(onReceive): permission NOT granted on ${usbAntplusAdapterName()}"
                                )
                                Handler(Looper.getMainLooper()).post {
                                    _onUsbAdapterStateChangeStreamHandler?.add(
                                        AntplusUsbAdapterState.PERMISSION_NOT_GRANTED
                                    )
                                    _onLogDataStreamHandler?.add(
                                        AntplusLogData(
                                            AntplusLogEvent.WARNING,
                                            TAG,
                                            "(onReceive): permission NOT granted on ${usbAntplusAdapterName()}"
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: AntException) {
                Log.e(TAG, "(onReceive):", e)
                Handler(Looper.getMainLooper()).post {
                    _onUsbAdapterStateChangeStreamHandler?.add(
                        AntplusUsbAdapterState.DETACHED
                    )
                    _onLogDataStreamHandler?.add(
                        AntplusLogData(
                            AntplusLogEvent.ERROR,
                            TAG,
                            "(onReceive): error ${e.message}"
                        )
                    )
                }
            }
        }
    }
}