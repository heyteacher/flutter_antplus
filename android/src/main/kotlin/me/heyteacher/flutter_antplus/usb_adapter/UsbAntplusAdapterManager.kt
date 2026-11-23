package me.heyteacher.flutter_antplus.usb_adapter

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import be.glever.ant.AntException
import be.glever.ant.usb.AndroidAntUsbDeviceFactory
import be.glever.ant.usb.impl.AndroidAntUsbDevice
import me.heyteacher.flutter_antplus.AbstractHostApi
import me.heyteacher.flutter_antplus.usb_adapter.pigeons.AntplusUsbAdapterState
import me.heyteacher.flutter_antplus.logging.OnLogDataStreamHandlerImpl
import me.heyteacher.flutter_antplus.logging.OnLogDataStreamLogger
import me.heyteacher.flutter_antplus.logging.pigeons.AntplusLogLevel
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
        private val _TAG: String = UsbAntplusAdapterManager::class.java.simpleName

        @Volatile
        private var _instance: UsbAntplusAdapterManager? = null

        /**
         * Gets singleton instance
         * @return the singleton instance
         */
        fun getInstance() =
            _instance ?: synchronized(this) {
                _instance ?: UsbAntplusAdapterManager().also { _instance = it }
            }
    }

    private var _usbAntplusAdapter: AndroidAntUsbDevice? = null
    private var _onLogDataStreamHandler: OnLogDataStreamHandlerImpl? = null
    private var _onUsbAdapterStateChangeStreamHandler: OnUsbAdapterStateChangeStreamHandlerImpl? =
        null
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
        log(_TAG, AntplusLogLevel.DEBUG, "<initializeListeners>:")
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
        log(_TAG, AntplusLogLevel.DEBUG, "<initializeUsbAdapter>:")
        // close ant usb device if was already initialized
        if (this._usbAntplusAdapter != null) {
           log(
                _TAG,
               AntplusLogLevel.DEBUG,
                "(initializeUsbAdapter): ${usbAntplusAdapterName()} already initialized, do nothing"
            )
            return
        }
        // register USB_PERMISSION, USB_DEVICE_ATTACHED and USB_DEVICE_DETACHED broadcast message
        val intentFilter = IntentFilter()
        intentFilter.addAction(usbPermission(context))
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        log(
            _TAG,
            AntplusLogLevel.DEBUG,
            "(initializeUsbAdapter): register usb receiver (permission, attach and detach intents"
        )

        // Source - https://stackoverflow.com/a/77529595
        // Posted by Mohd. Jafar Iqbal khan, modified by community. See post 'Timeline' for change history
        // Retrieved 2026-11-22, License - CC BY-SA 4.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                usbReceiver,
                intentFilter,
                Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(
                usbReceiver,
                intentFilter)
        }

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
                log(_TAG, AntplusLogLevel.ERROR,"(initializeUsbAdapter): adapter not detected")
                return
            }
        log(_TAG, AntplusLogLevel.DEBUG,"(initializeUsbAdapter): ${usbAntplusAdapterName()} found")
        // device found, if user grants permission, initialize it
        if (hasPermission(context)) {
            log(
                _TAG,
                AntplusLogLevel.DEBUG,
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
        log(_TAG, AntplusLogLevel.DEBUG,"<checkPermission>:")
        if (_usbAntplusAdapter == null) {
            initializeUsbAdapter(context)
        }
        if (_usbAntplusAdapter != null) {
            // check is user has granted permission
            if (hasPermission(context)) {
                log(
                    _TAG,
                    AntplusLogLevel.DEBUG,
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
                    log(
                        _TAG,
                        AntplusLogLevel.DEBUG,
                        "(checkPermission): ${usbAntplusAdapterName()} found, permission NOT granted " +
                                "and not already requested. Request permission and returns false"
                    )
                    requestPermission(context)
                } else {
                    log(
                        _TAG,
                        AntplusLogLevel.DEBUG,
                        "(checkPermission): ${usbAntplusAdapterName()} found, permission NOT granted " +
                                "and already requested. Returns false"
                    )
                }
                return false
            }
        } else {
            log(_TAG, AntplusLogLevel.DEBUG,"(checkPermission): usb adapter not found, returns false")
            return false
        }
    }

    /**
     * Closes the channel identified by channelNumber
     * @param channelNumber the number of channel to close
     */
    fun closeChannel(channelNumber: Byte) {
        log(_TAG,AntplusLogLevel.DEBUG, "<checkPermission>: channelNumber $channelNumber")
        _usbAntplusAdapter?.closeChannel(channelNumber)
    }

    /**
     * Closes listeners and android ant usb device
     */
    fun close() {
        log(_TAG, AntplusLogLevel.DEBUG,"<close>:")
        // close device listeners
        _usbAntplusAdapter?.close()
    }

    private fun usbAntplusAdapterName(): String =
        "usb adapter ${_usbAntplusAdapter?.androidDevice?.productName?.trim()}"

    private fun usbPermission(context: Context): String = "${context.packageName}.USB_PERMISSION"

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun requestPermission(context: Context) {
        log(_TAG, AntplusLogLevel.DEBUG,"<requestPermission>:")
        try {
            val androidDevice = _usbAntplusAdapter!!.androidDevice
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val hasPermission = hasPermission(context)
            log(
                _TAG,
                AntplusLogLevel.DEBUG,
                "(requestPermission): has permission on ${usbAntplusAdapterName()} $hasPermission"
            )
            if (!hasPermission) {
                log(
                    _TAG,
                    AntplusLogLevel.DEBUG,
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
                log(
                    _TAG,
                    AntplusLogLevel.DEBUG,
                    "(requestPermission): permission granted, initialize ${usbAntplusAdapterName()}"
                )
                _usbAntplusAdapter!!.initialize()
                invokePendingCallbacks()
            }
        } catch (t: Throwable) {
            log(_TAG, AntplusLogLevel.ERROR,"(requestPermission):", t)
            throw t
        }
    }

    private fun hasPermission(context: Context): Boolean {
        log(_TAG, AntplusLogLevel.DEBUG,"<hasPermission>:")
        if (_usbAntplusAdapter == null) {
            log(_TAG, AntplusLogLevel.DEBUG,"(hasPermission): usb adapter not found, returns false")
            return false
        }
        val androidDevice = _usbAntplusAdapter!!.androidDevice
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val hasPermission = usbManager.hasPermission(androidDevice)
        log(_TAG, AntplusLogLevel.DEBUG,"(hasPermission):  ${usbAntplusAdapterName()} found, returns $hasPermission")
        return hasPermission
    }

    private fun invokePendingCallbacks() {
        log(_TAG, AntplusLogLevel.DEBUG,"<invokePendingCallbacks>:")
        try {
            log(
                _TAG,
                AntplusLogLevel.DEBUG,
                "(invokePendingCallbacks): invoke ${_pendingCallbacks.count()} pending callbacks"
            )
            for (requestPermissionCallback in _pendingCallbacks) {
                requestPermissionCallback?.invoke()
            }
        } catch (t: Exception) {
            log(_TAG, AntplusLogLevel.ERROR,"(invokePendingCallbacks):", t)
        } finally {
            log(
                _TAG,
                AntplusLogLevel.DEBUG,
                "(invokePendingCallbacks): clear pending callbacks"
            )
            _pendingCallbacks.clear()
        }
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                log(
                    _TAG,
                    AntplusLogLevel.DEBUG,
                    "<onReceive>: action ${intent.action}"
                )
                when (intent.action) {
                    // usb device attached, initialize
                    UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                        log(
                            _TAG,
                            AntplusLogLevel.DEBUG,
                            "(onReceive): ${usbAntplusAdapterName()} attached, initialize"
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
                        log(
                            _TAG,
                            AntplusLogLevel.DEBUG,
                            "(onReceive): ${usbAntplusAdapterName()} detached, close it"
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
                                log(
                                    _TAG,
                                    AntplusLogLevel.DEBUG,
                                    "(onReceive): permission granted on ${usbAntplusAdapterName()}"
                                )
                                _usbAntplusAdapter?.androidDevice.apply {
                                    log(
                                        _TAG,
                                        AntplusLogLevel.DEBUG,
                                        "(onReceive): initialize ${usbAntplusAdapterName()}"
                                    )
                                    _usbAntplusAdapter!!.initialize()
                                    invokePendingCallbacks()
                                }
                            } else {
                                log(
                                    _TAG,
                                    AntplusLogLevel.WARNING,
                                    "(onReceive): permission NOT granted on ${usbAntplusAdapterName()}"
                                )
                                Handler(Looper.getMainLooper()).post {
                                    _onUsbAdapterStateChangeStreamHandler?.add(
                                        AntplusUsbAdapterState.PERMISSION_NOT_GRANTED
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: AntException) {
                log(_TAG, AntplusLogLevel.ERROR, "(onReceive):", e)
                Handler(Looper.getMainLooper()).post {
                    _onUsbAdapterStateChangeStreamHandler?.add(
                        AntplusUsbAdapterState.DETACHED
                    )
                }
            }
        }
    }

    private fun log(
        tag: String,
        level: AntplusLogLevel,
        message: String,
        throwable: Throwable? = null
    ) {
        AbstractHostApi.log(tag, level, message, throwable, _onLogDataStreamHandler)
    }
}