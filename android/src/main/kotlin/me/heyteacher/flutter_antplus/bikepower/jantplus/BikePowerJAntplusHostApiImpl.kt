package me.heyteacher.flutter_antplus.bikepower.jantplus

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import be.glever.ant.message.AntMessage
import be.glever.ant.message.data.BroadcastDataMessage
import be.glever.ant.usb.AntUsbDevice
import be.glever.ant.usb.AntUsbDeviceFactory
import be.glever.antplus.common.datapage.DataPage81ProductInformation
import be.glever.antplus.common.datapage.DataPage82BatteryStatus
import be.glever.antplus.power.PowerChannel
import be.glever.antplus.power.datapage.PowerDataPageRegistry
import be.glever.antplus.power.datapage.main.PowerDataPage02GetSetParameters
import be.glever.antplus.power.datapage.main.PowerDataPage10PowerOnly
import be.glever.antplus.power.datapage.main.PowerDataPage13TorqueEffectivenessPedalSmoothness
import io.flutter.plugin.common.BinaryMessenger
import me.heyteacher.flutter_antplus.bikepower.OnBalanceDataListener
import me.heyteacher.flutter_antplus.bikepower.OnBatteryStatusListener
import me.heyteacher.flutter_antplus.bikepower.OnCadenceDataListener
import me.heyteacher.flutter_antplus.bikepower.OnPedalSmoothnessDataListener
import me.heyteacher.flutter_antplus.bikepower.OnPowerDataListener
import me.heyteacher.flutter_antplus.bikepower.OnTorqueEffectivenessDataListener
import me.heyteacher.flutter_antplus.bikepower.pigeons.AntplusBatteryStatus
import me.heyteacher.flutter_antplus.bikepower.pigeons.AntplusPedalSmoothnessData
import me.heyteacher.flutter_antplus.bikepower.pigeons.AntplusTorqueEffectivenessData
import me.heyteacher.flutter_antplus.bikepower.pigeons.BikepowerHostApi
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnBalanceDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnBatteryStatusDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnCadenceDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnPedalSmoothnessDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnPowerDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnTorqueEffectivenessDataStreamHandler
import me.heyteacher.flutter_antplus.device.OnDeviceStateChangeListener
import me.heyteacher.flutter_antplus.device.OnRequestAccessResultListener
import me.heyteacher.flutter_antplus.device.OnScanResultListener
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDevice
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDeviceState
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDeviceType
import me.heyteacher.flutter_antplus.device.pigeons.AntplusRequestAccessResult
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
 * Android implementation of the [BikepowerHostApi] Pigeon interface.
 *
 * Manages scanning, connection, and telemetry subscriptions for an ANT+ Bike Power Meter.
 * Registers Pigeon event channel stream handlers for power, cadence, balance, pedal smoothness,
 * torque effectiveness, and battery status, and forwards all native events to Flutter.
 *
 * @param binaryMessenger the Flutter binary messenger for Pigeon channel registration
 * @param onLogDataListener shared log listener to forward native log events to Flutter
 */
class BikePowerJAntplusHostApiImpl(
    private val context: Context,
    binaryMessenger: BinaryMessenger,
    private var onLogDataListener: OnLogDataListener
) : BikepowerHostApi {

    companion object {
        private val TAG: String = BikePowerJAntplusHostApiImpl::class.java.simpleName
    }

    private lateinit var antUsbDevice: AntUsbDevice
    private var scanSubscription: Disposable? = null
    private var connectSubscription: Disposable? = null
    private val registry = PowerDataPageRegistry()

    private var onScanResultListener: OnScanResultListener = OnScanResultListener()
    private var onRequestAccessResultListener: OnRequestAccessResultListener =
        OnRequestAccessResultListener()
    private var onDeviceStateChangeListener: OnDeviceStateChangeListener =
        OnDeviceStateChangeListener()
    private var onPowerDataListener: OnPowerDataListener = OnPowerDataListener()
    private var onBalanceDataListener: OnBalanceDataListener = OnBalanceDataListener()
    private var onCadenceDataListener: OnCadenceDataListener = OnCadenceDataListener()
    private var onPedalSmoothnessDataListener: OnPedalSmoothnessDataListener =
        OnPedalSmoothnessDataListener()
    private var onTorqueEffectivenessDataListener: OnTorqueEffectivenessDataListener =
        OnTorqueEffectivenessDataListener()
    private var onBatteryStatusListener: OnBatteryStatusListener = OnBatteryStatusListener()
    /**
     * Registers all Pigeon Host APIs and event stream handlers on initialization.
     */
    init {
        BikepowerHostApi.setUp(binaryMessenger, this)
        OnScanResultStreamHandler.register(
            binaryMessenger, onScanResultListener, AntplusDeviceType.BIKEPOWER.name
        )
        OnDeviceStateChangeStreamHandler.register(
            binaryMessenger, onDeviceStateChangeListener, AntplusDeviceType.BIKEPOWER.name
        )
        OnRequestAccessResultStreamHandler.register(
            binaryMessenger, onRequestAccessResultListener, AntplusDeviceType.BIKEPOWER.name
        )

        OnPowerDataStreamHandler.register(binaryMessenger, onPowerDataListener)
        OnPedalSmoothnessDataStreamHandler.register(binaryMessenger, onPedalSmoothnessDataListener)
        OnBatteryStatusDataStreamHandler.register(binaryMessenger, onBatteryStatusListener)
        OnBalanceDataStreamHandler.register(binaryMessenger, onBalanceDataListener)
        OnCadenceDataStreamHandler.register(binaryMessenger, onCadenceDataListener)
        OnTorqueEffectivenessDataStreamHandler.register(
            binaryMessenger, onTorqueEffectivenessDataListener
        )
        initializeUsbDevice()
    }

    /** Starts an async scan for nearby ANT+ Bike Power Meters. */
    override fun startScan() {
        Handler(Looper.getMainLooper()).post {
            onLogDataListener.add(
                AntplusLogData(
                    AntplusLogEvent.VERBOSE, TAG, "<startScan>:"
                )
            )
        }
        try {
            antUsbDevice.initialize(context)
            scanSubscription?.dispose()
            scanSubscription = PowerChannel(antUsbDevice).events
                .doOnNext(Consumer { antMessage: AntMessage -> this.handleScan(antMessage) }).subscribe()

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

    /** Stops the active async scan for Bike Power Meters. */
    override fun stopScan() {
        Handler(Looper.getMainLooper()).post {
            onLogDataListener.add(
                AntplusLogData(
                    AntplusLogEvent.VERBOSE, TAG, "<stopScan>:"
                )
            )
        }
        scanSubscription?.dispose()
        antUsbDevice.close()
    }

    /**
     * Connects to the ANT+ Bike Power Meter identified by [deviceNumber].
     * @param deviceNumber the ANT+ device number of the sensor to connect to
     */
    override fun connect(deviceNumber: Long) {
        Handler(Looper.getMainLooper()).post {
            onLogDataListener.add(
                AntplusLogData(
                    AntplusLogEvent.VERBOSE, TAG, "<connect>: deviceNumber $deviceNumber"
                )
            )
        }
        // stop scan
        scanSubscription?.dispose()
        antUsbDevice.close()
        Handler(Looper.getMainLooper()).post {
            onDeviceStateChangeListener.add(AntplusDeviceState.SEARCHING)
        }
        antUsbDevice.initialize(context)
        antUsbDevice.closeAllChannels()
        connectSubscription?.dispose()
        connectSubscription =
            PowerChannel(
                antUsbDevice,
                DataPage81ProductInformation.getByteArraySerialNumber(deviceNumber)
            ).events
            .doOnNext(Consumer { antMessage: AntMessage -> this.handleAntDevice(antMessage) })
            .subscribe()
        Handler(Looper.getMainLooper()).post {
            onDeviceStateChangeListener.add(AntplusDeviceState.PROCESSING_REQUEST)
        }
    }

    /** Disconnects from the currently connected ANT+ Bike Power Meter. */
    override fun disconnect() {
        Handler(Looper.getMainLooper()).post {
            onLogDataListener.add(
                AntplusLogData(
                    AntplusLogEvent.VERBOSE, TAG, "<disconnect>:"
                )
            )
        }
        connectSubscription?.dispose()
        antUsbDevice.close()
        Handler(Looper.getMainLooper()).post {
            onDeviceStateChangeListener.add(AntplusDeviceState.CLOSED)
        }
    }

    private val actionUsbPermission = "com.android.example.USB_PERMISSION"


    override fun requestPermission() {
        try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val permissionIntent = PendingIntent.getBroadcast(context, 0,
                Intent(actionUsbPermission), PendingIntent.FLAG_IMMUTABLE)
            //val filter = IntentFilter(actionUsbPermission)
            //context.registerReceiver(usbReceiver, filter)
            usbManager.requestPermission(antUsbDevice.androidDevice, permissionIntent)
        } catch (t: Throwable) {
            Log.e(TAG, "(requestPermission): error", t)
            onLogDataListener.add(
                AntplusLogData(
                    AntplusLogEvent.ERROR,
                    TAG,
                    "(requestPermission): error  ${t.message}"
                )
            )
            throw t
        }
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (actionUsbPermission == intent.action) {
                synchronized(this) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (antUsbDevice.androidDevice == null) {
                            onLogDataListener.add(
                                AntplusLogData(
                                    AntplusLogEvent.WARNING,
                                    TAG,
                                    "(BroadcastReceiver.onReceive): device is null"
                                )
                            )
                            return
                        }
                        antUsbDevice.androidDevice.apply {
                            onLogDataListener.add(
                                AntplusLogData(
                                    AntplusLogEvent.INFO,
                                    TAG,
                                    "(BroadcastReceiver.onReceive): permission $actionUsbPermission GRANTED"
                                )
                            )
                        }
                    } else {
                        onLogDataListener.add(
                            AntplusLogData(
                                AntplusLogEvent.WARNING,
                                TAG,
                                "(setActivity): permission $actionUsbPermission DENIED"
                            )
                        )
                    }
                }
            }
        }
    }
    /** Closes all active event stream listeners. Called when the plugin is detached from the engine. */
    fun close() {
        // close device listeners
        onScanResultListener.onEventsDone()
        onRequestAccessResultListener.onEventsDone()
        onDeviceStateChangeListener.onEventsDone()
        // close bikepower listeners
        onPowerDataListener.onEventsDone()
        onBalanceDataListener.onEventsDone()
        onBatteryStatusListener.onEventsDone()
        onCadenceDataListener.onEventsDone()
        onTorqueEffectivenessDataListener.onEventsDone()
        onPedalSmoothnessDataListener.onEventsDone()
        onLogDataListener.onEventsDone()
        antUsbDevice.close()
        scanSubscription?.dispose()
        connectSubscription?.dispose()
    }
    private fun initializeUsbDevice(): AntUsbDevice? {
        antUsbDevice = AntUsbDeviceFactory.getAvailableAntDevices(context).stream().findFirst().getOrElse {
            Handler(Looper.getMainLooper()).post {
                onRequestAccessResultListener.add(
                    AntplusRequestAccessResult.ADAPTER_NOT_DETECTED
                )
                onLogDataListener.add(
                    AntplusLogData(
                        AntplusLogEvent.WARNING,
                        TAG,
                        "(startScan): adapter not detected"
                    )
                )
            }
            return null
        }
        return antUsbDevice
    }
    private fun handleScan(antMessage:AntMessage ) {
        try {
            if (antMessage is BroadcastDataMessage) {
                val payLoad = antMessage.payLoad
                removeToggleBit(payLoad)
                val dataPage = registry.constructDataPage(payLoad) ?: return
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
    private fun handleAntDevice(antMessage:AntMessage ) {
        try {
            Handler(Looper.getMainLooper()).post {
                onDeviceStateChangeListener.add(AntplusDeviceState.TRACKING)
            }
            if (antMessage is BroadcastDataMessage) {
                val payLoad = antMessage.payLoad
                removeToggleBit(payLoad)
                val dataPage = registry.constructDataPage(payLoad) ?: return
                when (dataPage) {
                    is PowerDataPage10PowerOnly  -> {
                        Handler(Looper.getMainLooper()).post {
                            onPowerDataListener.add(dataPage.instantaneousPower.toLong())
                            onCadenceDataListener.add(dataPage.instantaneousCadence.toLong())
                            onBalanceDataListener.add(dataPage.pedalPowerDistribution.percentage.toLong())
                        }
                    }
                    is PowerDataPage13TorqueEffectivenessPedalSmoothness -> {
                        Handler(Looper.getMainLooper()).post {
                            onTorqueEffectivenessDataListener.add(
                                AntplusTorqueEffectivenessData(
                                    dataPage.leftTorqueEffectiveness.toDouble(),
                                    dataPage.rightTorqueEffectiveness.toDouble()
                                )
                            )
                            onPedalSmoothnessDataListener.add(
                                AntplusPedalSmoothnessData(
                                    dataPage.combinedPedalSmoothness == 0,
                                    dataPage.leftPedalSmoothness.toDouble(),
                                    dataPage.rightPedalSmoothness.toDouble()
                                )
                            )
                        }
                    }
                    is DataPage82BatteryStatus -> {
                        Handler(Looper.getMainLooper()).post {
                            onBatteryStatusListener.add(AntplusBatteryStatus.ofRaw(dataPage.batteryStatus.value ) ?: AntplusBatteryStatus.INVALID)
                        }
                    }
                    is PowerDataPage02GetSetParameters -> {
                        Handler(Looper.getMainLooper()).post {
                            onLogDataListener.add(
                                AntplusLogData(
                                    AntplusLogEvent.VERBOSE,
                                    TAG,
                                    "(handle): PowerDataPage02GetSetParameters ${dataPage.subPage}"
                                )
                            )
                        }
                    }
                    else -> {
                        /*
                        Handler(Looper.getMainLooper()).post {
                            onLogDataListener.add(
                                AntplusLogData(
                                    AntplusLogEvent.VERBOSE,
                                    TAG,
                                    "(handle): not handled pageNumber ${dataPage.pageNumber} " +
                                            "payload ${ByteUtils.hexString(dataPage.dataPageBytes)}"
                                )
                            )
                        }
                         */
                    }
                }
            }
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                onLogDataListener.add(
                    AntplusLogData(
                        AntplusLogEvent.ERROR,
                        TAG,
                        "(handleAntDevice): error ${e.message} "
                    )
                )
            }
        }
    }
    private fun removeToggleBit(payLoad: ByteArray) {
        payLoad[0] = (127 and payLoad[0].toInt()).toByte()
    }
}