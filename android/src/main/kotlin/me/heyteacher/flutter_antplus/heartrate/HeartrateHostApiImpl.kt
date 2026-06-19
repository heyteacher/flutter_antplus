package me.heyteacher.flutter_antplus.heartrate

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
import io.flutter.plugin.common.BinaryMessenger
import me.heyteacher.flutter_antplus.device.OnDeviceStateChangeListener
import me.heyteacher.flutter_antplus.device.OnRequestAccessResultListener
import me.heyteacher.flutter_antplus.device.OnScanResultListener
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDevice
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDeviceType
import me.heyteacher.flutter_antplus.device.pigeons.OnDeviceStateChangeStreamHandler
import me.heyteacher.flutter_antplus.device.pigeons.OnRequestAccessResultStreamHandler
import me.heyteacher.flutter_antplus.device.pigeons.OnScanResultStreamHandler
import me.heyteacher.flutter_antplus.heartrate.pigeons.HeartrateHostApi
import me.heyteacher.flutter_antplus.heartrate.pigeons.OnHeartRateDataStreamHandler
import me.heyteacher.flutter_antplus.logging.OnLogDataListener
import me.heyteacher.flutter_antplus.logging.pigeons.AntplusLogData
import me.heyteacher.flutter_antplus.logging.pigeons.AntplusLogEvent
import java.math.BigDecimal
import java.util.EnumSet
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDeviceState as PigeonDeviceState
import me.heyteacher.flutter_antplus.device.pigeons.AntplusRequestAccessResult as PigeonRequestAccessResult

class HeartrateHostApiImpl(
    private val context: Context,
    binaryMessenger: BinaryMessenger,
    private var onLogDataListener: OnLogDataListener
) : HeartrateHostApi {

    companion object {
        private val TAG: String = HeartrateHostApiImpl::class.java.simpleName
    }

    private var onScanResultListener: OnScanResultListener = OnScanResultListener()
    private var onDeviceStateChangeListener: OnDeviceStateChangeListener =
        OnDeviceStateChangeListener()
    private var onRequestAccessResultListener: OnRequestAccessResultListener =
        OnRequestAccessResultListener()
    private var onHeartRateDataListener: OnHeartRateDataListener = OnHeartRateDataListener()

    private var releaseHandle: PccReleaseHandle<AntPlusHeartRatePcc>? = null
    private var hrPcc: AntPlusHeartRatePcc? = null

    private val stateChangeReceiver = IDeviceStateChangeReceiver { deviceState ->
        Handler(Looper.getMainLooper()).post {
            onLogDataListener.add(
                AntplusLogData(
                    AntplusLogEvent.VERBOSE,
                    TAG,
                    "<onDeviceStateChange>: " + hrPcc?.antDeviceNumber + " deviceState " + deviceState
                )
            )
            onDeviceStateChangeListener.add(PigeonDeviceState.valueOf(deviceState.toString()))
        }
    }
    private val accessResultReceiver: IPluginAccessResultReceiver<AntPlusHeartRatePcc?> =
        IPluginAccessResultReceiver { result, resultCode, initialDeviceState ->
            when (resultCode) {
                RequestAccessResult.SUCCESS -> {
                    hrPcc = result
                    hrPcc!!.subscribeHeartRateDataEvent { _: Long, _: EnumSet<EventFlag?>?, computedHeartRate: Int, _: Long, _: BigDecimal?, _: DataState? ->
                        Handler(Looper.getMainLooper()).post {
                            onHeartRateDataListener.add(computedHeartRate.toLong())
                        }
                    }
                    Handler(Looper.getMainLooper()).post {
                        onRequestAccessResultListener.add(
                            PigeonRequestAccessResult.valueOf(resultCode.name)
                        )
                        onDeviceStateChangeListener.add(
                            PigeonDeviceState.valueOf(
                                initialDeviceState.toString()
                            )
                        )
                        onLogDataListener.add(
                            AntplusLogData(
                                AntplusLogEvent.INFO,
                                TAG,
                                "(onResultReceived): resultCode $resultCode. deviceNumber " + (result?.antDeviceNumber
                                    ?: "") + " initialDeviceState " + initialDeviceState
                            )
                        )
                    }
                }

                RequestAccessResult.DEPENDENCY_NOT_INSTALLED -> Handler(Looper.getMainLooper()).post {
                    onRequestAccessResultListener.add(
                        PigeonRequestAccessResult.valueOf(resultCode.name)
                    )
                    onLogDataListener.add(
                        AntplusLogData(
                            AntplusLogEvent.ERROR,
                            TAG,
                            "(onResultReceived): resultCode " + resultCode + ". The required service\"" + AntPlusHeartRatePcc.getMissingDependencyName() + "\"was not found. You need to install the ANT+ Plugins service or you may need to update your existing version if you already have it. Do you want to launch the Play Store to get it?"
                        )
                    )
                }

                else -> {
                    Handler(Looper.getMainLooper()).post {
                        onRequestAccessResultListener.add(
                            PigeonRequestAccessResult.valueOf(resultCode.name)
                        )
                        onLogDataListener.add(
                            AntplusLogData(
                                AntplusLogEvent.WARNING, TAG, "<onResultReceived>: resultCode $resultCode"
                            )
                        )
                    }
                }
            }
        }
    private var hrScanCtrl: AsyncScanController<AntPlusHeartRatePcc>? = null

    /**
     * Initializes
     */
    init {
        HeartrateHostApi.setUp(binaryMessenger, this)
        OnHeartRateDataStreamHandler.register(binaryMessenger, onHeartRateDataListener)
        OnDeviceStateChangeStreamHandler.register(
            binaryMessenger, onDeviceStateChangeListener, AntplusDeviceType.HEARTRATE.name
        )
        OnScanResultStreamHandler.register(
            binaryMessenger, onScanResultListener, AntplusDeviceType.HEARTRATE.name
        )
        OnRequestAccessResultStreamHandler.register(
            binaryMessenger, onRequestAccessResultListener, AntplusDeviceType.HEARTRATE.name
        )
    }

    /**
     * Starts the scan
     */
    override fun startScan() {
        Handler(Looper.getMainLooper()).post {
            onLogDataListener.add(
                AntplusLogData(
                    AntplusLogEvent.VERBOSE, TAG, "<startScan>:"
                )
            )
        }
        hrScanCtrl = AntPlusHeartRatePcc.requestAsyncScanController(
            context,
            0,
            object : IAsyncScanResultReceiver {
                override fun onSearchStopped(reasonStopped: RequestAccessResult) {
                    Handler(Looper.getMainLooper()).post {
                        onLogDataListener.add(
                            AntplusLogData(
                                AntplusLogEvent.VERBOSE, TAG, "<onSearchStopped>:"
                            )
                        )
                    }
                    //The triggers calling this function use the same codes and require the same actions as those received by the standard access result receiver
                    accessResultReceiver.onResultReceived(null, reasonStopped, DeviceState.DEAD)
                }

                override fun onSearchResult(deviceFound: AsyncScanResultDeviceInfo) {
                    Handler(Looper.getMainLooper()).post {
                        onLogDataListener.add(
                            AntplusLogData(
                                AntplusLogEvent.VERBOSE,
                                TAG,
                                "<onSearchResult>: number " + deviceFound.antDeviceNumber + " name " + deviceFound.deviceDisplayName + " already connected " + deviceFound.isAlreadyConnected
                            )
                        )
                        onScanResultListener.add(
                            AntplusDevice(
                                deviceFound.antDeviceNumber.toLong(),
                                deviceFound.deviceDisplayName,
                                AntplusDeviceType.HEARTRATE
                            )
                        )
                    }
                }
            })
    }

    /**
     * Stops the scan
     */
    override fun stopScan() {
        Handler(Looper.getMainLooper()).post {
            onLogDataListener.add(
                AntplusLogData(
                    AntplusLogEvent.VERBOSE, TAG, "<stopScan>:"
                )
            )
        }
        if (hrScanCtrl != null) {
            hrScanCtrl!!.closeScanController()
        }
        hrScanCtrl = null
    }

    /**
     * Connects to device with device number specified
     * @param deviceNumber the device number
     */
    override fun connect(deviceNumber: Long) {
        Handler(Looper.getMainLooper()).post {
            onLogDataListener.add(
                AntplusLogData(
                    AntplusLogEvent.VERBOSE, TAG, "<connect>: deviceNumber $deviceNumber"
                )
            )
        }
        stopScan()
        releaseHandle = AntPlusHeartRatePcc.requestAccess(
            context, deviceNumber.toInt(), -1, accessResultReceiver, stateChangeReceiver
        )
    }

    /**
     * Disconnects the device
     */
    override fun disconnect() {
        Handler(Looper.getMainLooper()).post {
            onLogDataListener.add(
                AntplusLogData(
                    AntplusLogEvent.VERBOSE, TAG, "<disconnect>:"
                )
            )
        }
        if (releaseHandle != null) {
            releaseHandle!!.close()
        }
        if (hrPcc != null) {
            Handler(Looper.getMainLooper()).post {
                onLogDataListener.add(
                    AntplusLogData(
                        AntplusLogEvent.VERBOSE, TAG, "(disconnect): deviceNumber " + hrPcc?.antDeviceNumber
                    )
                )
            }
            hrPcc?.subscribeHeartRateDataEvent(null)
            hrPcc?.releaseAccess()
        }
        releaseHandle = null
        hrPcc = null
    }

    /**
     * Closes listeners
     */
    fun close() {
        // closes device listeners
        onScanResultListener.onEventsDone()
        onRequestAccessResultListener.onEventsDone()
        onDeviceStateChangeListener.onEventsDone()
        // closes heartrate listener
        onHeartRateDataListener.onEventsDone()
    }
}