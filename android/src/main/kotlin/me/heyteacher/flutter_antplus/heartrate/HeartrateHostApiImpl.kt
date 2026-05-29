package me.heyteacher.flutter_antplus.heartrate

import AntplusDevice
import AntplusDeviceState
import OnDeviceStateChangeStreamHandler
import OnHeartRateDataStreamHandler
import OnScanResultStreamHandler
import PigeonEventSink
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.DataState
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IDeviceStateChangeReceiver
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IPluginAccessResultReceiver
import com.dsi.ant.plugins.antplus.pccbase.AsyncScanController
import com.dsi.ant.plugins.antplus.pccbase.AsyncScanController.AsyncScanResultDeviceInfo
import com.dsi.ant.plugins.antplus.pccbase.AsyncScanController.IAsyncScanResultReceiver
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle
import io.flutter.Log
import io.flutter.plugin.common.BinaryMessenger
import me.heyteacher.flutter_antplus.heartrate.pigeons.HeartrateHostApi
import me.heyteacher.flutter_antplus.heartrate.pigeons.HeartrateHostApi.Companion.setUp
import java.math.BigDecimal
import java.util.EnumSet

class HeartrateHostApiImpl(private val context: Context, binaryMessenger: BinaryMessenger) :
    HeartrateHostApi {

    companion object {
        private val TAG: String = HeartrateHostApiImpl::class.java.simpleName
    }

    private var onScanResultListener: OnScanResultListener = OnScanResultListener()
    private var onDeviceStateChangeListener: OnDeviceStateChangeListener =
        OnDeviceStateChangeListener()
    private var onHeartRateDataListener: OnHeartRateDataListener = OnHeartRateDataListener()
    private var releaseHandle: PccReleaseHandle<AntPlusHeartRatePcc>? = null
    private var hrPcc: AntPlusHeartRatePcc? = null
    private val stateChangeReceiver = IDeviceStateChangeReceiver { deviceState ->
        Log.d(
            TAG, "<onDeviceStateChange>: " + hrPcc!!.antDeviceNumber + " deviceState " + deviceState
        )
        Handler(Looper.getMainLooper()).post {
            onDeviceStateChangeListener.add(
                when (deviceState) {
                    DeviceState.DEAD -> AntplusDeviceState.DEAD
                    DeviceState.CLOSED -> AntplusDeviceState.CLOSED
                    DeviceState.PROCESSING_REQUEST -> AntplusDeviceState.PROCESSING_REQUEST
                    DeviceState.SEARCHING -> AntplusDeviceState.SEARCHING
                    DeviceState.TRACKING -> AntplusDeviceState.TRACKING
                    DeviceState.UNRECOGNIZED -> AntplusDeviceState.UNRECOGNIZED
                    null -> AntplusDeviceState.UNRECOGNIZED
                }
            )
        }
    }
    private val accessResultReceiver: IPluginAccessResultReceiver<AntPlusHeartRatePcc?> =
        IPluginAccessResultReceiver { result, resultCode, initialDeviceState ->

            //Handle the result, connecting to events on success or reporting failure to user.
            Log.d(
                TAG, "<onResultReceived>: resultCode $resultCode"
            )
            when (resultCode) {
                RequestAccessResult.SUCCESS -> {
                    hrPcc = result
                    Log.d(
                        TAG,
                        "(onResultReceived): resultCode $resultCode. deviceNumber " + (result?.antDeviceNumber
                            ?: "") + " initialDeviceState " + initialDeviceState
                    )
                    Handler(Looper.getMainLooper()).post {
                        onDeviceStateChangeListener.add(
                            when (initialDeviceState) {
                                DeviceState.DEAD -> AntplusDeviceState.DEAD
                                DeviceState.CLOSED -> AntplusDeviceState.CLOSED
                                DeviceState.PROCESSING_REQUEST -> AntplusDeviceState.PROCESSING_REQUEST
                                DeviceState.SEARCHING -> AntplusDeviceState.SEARCHING
                                DeviceState.TRACKING -> AntplusDeviceState.TRACKING
                                DeviceState.UNRECOGNIZED -> AntplusDeviceState.UNRECOGNIZED
                                null -> AntplusDeviceState.UNRECOGNIZED
                            }
                        )
                    }
                    hrPcc!!.subscribeHeartRateDataEvent { _: Long, _: EnumSet<EventFlag?>?, computedHeartRate: Int, _: Long, _: BigDecimal?, _: DataState? ->
                        Handler(Looper.getMainLooper()).post {
                            onHeartRateDataListener.add(computedHeartRate.toLong())
                        }
                    }
                }

                RequestAccessResult.DEPENDENCY_NOT_INSTALLED -> Log.e(
                    TAG,
                    "(onResultReceived): resultCode " + resultCode + ". The required service\"" + AntPlusHeartRatePcc.getMissingDependencyName() + "\"was not found. You need to install the ANT+ Plugins service or you may need to update your existing version if you already have it. Do you want to launch the Play Store to get it?"
                )

                RequestAccessResult.UNRECOGNIZED, RequestAccessResult.CHANNEL_NOT_AVAILABLE, RequestAccessResult.ADAPTER_NOT_DETECTED, RequestAccessResult.BAD_PARAMS, RequestAccessResult.OTHER_FAILURE, RequestAccessResult.USER_CANCELLED -> {}
                else -> {}
            }
        }
    private var hrScanCtrl: AsyncScanController<AntPlusHeartRatePcc>? = null

    init {
        setUp(binaryMessenger, this)
        OnHeartRateDataStreamHandler.register(binaryMessenger, onHeartRateDataListener)
        OnDeviceStateChangeStreamHandler.register(binaryMessenger, onDeviceStateChangeListener)
        OnScanResultStreamHandler.register(binaryMessenger, onScanResultListener)
    }

    override fun startScan() {
        Log.d(TAG, "<startScan>:")
        hrScanCtrl = AntPlusHeartRatePcc.requestAsyncScanController(
            context,
            0,
            object : IAsyncScanResultReceiver {
                override fun onSearchStopped(reasonStopped: RequestAccessResult) {
                    Log.d(TAG, "<onSearchStopped>:")
                    //The triggers calling this function use the same codes and require the same actions as those received by the standard access result receiver
                    accessResultReceiver.onResultReceived(null, reasonStopped, DeviceState.DEAD)
                }

                override fun onSearchResult(deviceFound: AsyncScanResultDeviceInfo) {
                    Log.d(
                        TAG,
                        "<onSearchResult>: number " + deviceFound.antDeviceNumber + " name " + deviceFound.deviceDisplayName + " already connected " + deviceFound.isAlreadyConnected
                    )
                    Handler(Looper.getMainLooper()).post {
                        onScanResultListener.add(
                            AntplusDevice(
                                deviceFound.antDeviceNumber.toLong(), deviceFound.deviceDisplayName
                            )
                        )
                    }
                }
            })
    }

    override fun stopScan() {
        if (hrScanCtrl != null) {
            hrScanCtrl!!.closeScanController()
        }
        hrScanCtrl = null
    }

    override fun connect(deviceNumber: Long) {
        Log.d(
            TAG, "<connect>: deviceNumber $deviceNumber"
        )
        stopScan()
        releaseHandle = AntPlusHeartRatePcc.requestAccess(
            context, deviceNumber.toInt(), -1, accessResultReceiver, stateChangeReceiver
        )
    }

    override fun disconnect() {
        Log.d(TAG, "<disconnect>:")
        if (releaseHandle != null) {
            releaseHandle!!.close()
        }
        if (hrPcc != null) {
            Log.d(TAG, "(disconnect): deviceNumber " + hrPcc!!.antDeviceNumber)
            hrPcc!!.subscribeHeartRateDataEvent(null)
            hrPcc!!.releaseAccess()
        }
        releaseHandle = null
        hrPcc = null
    }

    fun close() {
        onScanResultListener.onEventsDone()
        onHeartRateDataListener.onEventsDone()
        onDeviceStateChangeListener.onEventsDone()
    }
}

class OnHeartRateDataListener : OnHeartRateDataStreamHandler() {
    private var eventSink: PigeonEventSink<Long>? = null

    override fun onListen(p0: Any?, sink: PigeonEventSink<Long>) {
        eventSink = sink
    }

    fun add(bpm: Long) {
        eventSink?.success(bpm)
    }

    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}

class OnScanResultListener : OnScanResultStreamHandler() {
    private var eventSink: PigeonEventSink<AntplusDevice>? = null

    override fun onListen(p0: Any?, sink: PigeonEventSink<AntplusDevice>) {
        eventSink = sink
    }

    fun add(antplusDevice: AntplusDevice) {
        eventSink?.success(antplusDevice)
    }

    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}

class OnDeviceStateChangeListener : OnDeviceStateChangeStreamHandler() {
    private var eventSink: PigeonEventSink<AntplusDeviceState>? = null

    override fun onListen(p0: Any?, sink: PigeonEventSink<AntplusDeviceState>) {
        eventSink = sink
    }

    fun add(antplusDeviceState: AntplusDeviceState) {
        eventSink?.success(antplusDeviceState)
    }

    fun onEventsDone() {
        eventSink?.endOfStream()
        eventSink = null
    }
}
