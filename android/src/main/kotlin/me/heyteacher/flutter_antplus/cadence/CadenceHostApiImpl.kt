package me.heyteacher.flutter_antplus.cadence

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeCadencePcc
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IDeviceStateChangeReceiver
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IPluginAccessResultReceiver
import com.dsi.ant.plugins.antplus.pccbase.AntPlusBikeSpdCadCommonPcc
import com.dsi.ant.plugins.antplus.pccbase.AsyncScanController
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle
import io.flutter.plugin.common.BinaryMessenger
import me.heyteacher.flutter_antplus.cadence.pigeons.CadenceHostApi
import me.heyteacher.flutter_antplus.cadence.pigeons.OnCadenceDataStreamHandler
import me.heyteacher.flutter_antplus.device.OnDeviceStateChangeListener
import me.heyteacher.flutter_antplus.device.OnRequestAccessResultListener
import me.heyteacher.flutter_antplus.device.OnScanResultListener
import me.heyteacher.flutter_antplus.device.pigeons.Device
import me.heyteacher.flutter_antplus.device.pigeons.DeviceType
import me.heyteacher.flutter_antplus.device.pigeons.OnDeviceStateChangeStreamHandler
import me.heyteacher.flutter_antplus.device.pigeons.OnRequestAccessResultStreamHandler
import me.heyteacher.flutter_antplus.device.pigeons.OnScanResultStreamHandler
import me.heyteacher.flutter_antplus.logging.OnLogDataListener
import me.heyteacher.flutter_antplus.logging.pigeons.LogData
import me.heyteacher.flutter_antplus.logging.pigeons.LogEvent
import java.math.BigDecimal
import java.util.EnumSet
import me.heyteacher.flutter_antplus.device.pigeons.DeviceState as PigeonDeviceState
import me.heyteacher.flutter_antplus.device.pigeons.RequestAccessResult as PigeonRequestAccessResult

class CadenceHostApiImpl(
    private val context: Context,
    binaryMessenger: BinaryMessenger,
    private var onLogDataListener: OnLogDataListener
) : CadenceHostApi {

    companion object {
        private val TAG: String = CadenceHostApiImpl::class.java.simpleName
    }

    private var onScanResultListener: OnScanResultListener = OnScanResultListener()
    private var onDeviceStateChangeListener: OnDeviceStateChangeListener =
        OnDeviceStateChangeListener()
    private var onRequestAccessResultListener: OnRequestAccessResultListener =
        OnRequestAccessResultListener()
    private var onCadenceDataListener: OnCadenceDataListener = OnCadenceDataListener()

    private var releaseHandle: PccReleaseHandle<AntPlusBikeCadencePcc>? = null
    private var hrPcc: AntPlusBikeCadencePcc? = null

    private val stateChangeReceiver = IDeviceStateChangeReceiver { deviceState ->
        Handler(Looper.getMainLooper()).post {
            onLogDataListener.add(
                LogData(
                    LogEvent.VERBOSE,
                    TAG,
                    "<onDeviceStateChange>: " + hrPcc?.antDeviceNumber + " deviceState " + deviceState
                )
            )
            onDeviceStateChangeListener.add(PigeonDeviceState.valueOf(deviceState.toString()))
        }
    }
    private val accessResultReceiver: IPluginAccessResultReceiver<AntPlusBikeCadencePcc?> =
        IPluginAccessResultReceiver { result, resultCode, initialDeviceState ->
            when (resultCode) {
                RequestAccessResult.SUCCESS -> {
                    hrPcc = result
                    hrPcc!!.subscribeCalculatedCadenceEvent { _: Long, _: EnumSet<EventFlag?>?,  computedCadence: BigDecimal ->
                        Handler(Looper.getMainLooper()).post {
                            onCadenceDataListener.add(computedCadence.toLong())
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
                            LogData(
                                LogEvent.INFO,
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
                        LogData(
                            LogEvent.ERROR,
                            TAG,
                            "(onResultReceived): resultCode " + resultCode + ". The required service\"" + AntPlusBikeCadencePcc.getMissingDependencyName() + "\"was not found. You need to install the ANT+ Plugins service or you may need to update your existing version if you already have it. Do you want to launch the Play Store to get it?"
                        )
                    )
                }

                else -> {
                    Handler(Looper.getMainLooper()).post {
                        onRequestAccessResultListener.add(
                            PigeonRequestAccessResult.valueOf(resultCode.name)
                        )
                        onLogDataListener.add(
                            LogData(
                                LogEvent.WARNING, TAG, "<onResultReceived>: resultCode $resultCode"
                            )
                        )
                    }
                }
            }
        }
    private var hrScanCtrl: AsyncScanController<AntPlusBikeCadencePcc>? = null

    /**
     * Initializes
     */
    init {
        CadenceHostApi.setUp(binaryMessenger, this)
        OnCadenceDataStreamHandler.register(binaryMessenger, onCadenceDataListener)
        OnDeviceStateChangeStreamHandler.register(
            binaryMessenger, onDeviceStateChangeListener, DeviceType.CADENCE.name
        )
        OnScanResultStreamHandler.register(
            binaryMessenger, onScanResultListener, DeviceType.CADENCE.name
        )
        OnRequestAccessResultStreamHandler.register(
            binaryMessenger, onRequestAccessResultListener, DeviceType.CADENCE.name
        )
    }

    /**
     * Starts the scan
     */
    override fun startScan() {
        Handler(Looper.getMainLooper()).post {
            onLogDataListener.add(
                LogData(
                    LogEvent.VERBOSE, TAG, "<startScan>:"
                )
            )
        }
        hrScanCtrl = AntPlusBikeCadencePcc.requestAsyncScanController(
            context,
            0,
            object : AntPlusBikeSpdCadCommonPcc.IBikeSpdCadAsyncScanResultReceiver {
                override fun onSearchStopped(reasonStopped: RequestAccessResult) {
                    Handler(Looper.getMainLooper()).post {
                        onLogDataListener.add(
                            LogData(
                                LogEvent.VERBOSE, TAG, "<onSearchStopped>:"
                            )
                        )
                    }
                    //The triggers calling this function use the same codes and require the same actions as those received by the standard access result receiver
                    accessResultReceiver.onResultReceived(null, reasonStopped, DeviceState.DEAD)
                }

                override fun onSearchResult(deviceFound: AntPlusBikeSpdCadCommonPcc.BikeSpdCadAsyncScanResultDeviceInfo?) {
                    Handler(Looper.getMainLooper()).post {
                        onLogDataListener.add(
                            LogData(
                                LogEvent.VERBOSE,
                                TAG,
                                "<onSearchResult>: number " + deviceFound?.resultInfo?.antDeviceNumber + " name " + deviceFound?.resultInfo?.deviceDisplayName + " already connected " + deviceFound?.resultInfo?.isAlreadyConnected
                            )
                        )
                        if (deviceFound?.resultInfo != null) {
                            onScanResultListener.add(
                                Device(
                                    deviceFound.resultInfo.antDeviceNumber.toLong(),
                                    deviceFound.resultInfo.deviceDisplayName,
                                    DeviceType.CADENCE
                                )
                            )
                        }
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
                LogData(
                    LogEvent.VERBOSE, TAG, "<stopScan>:"
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
                LogData(
                    LogEvent.VERBOSE, TAG, "<connect>: deviceNumber $deviceNumber"
                )
            )
        }
        stopScan()
        releaseHandle = AntPlusBikeCadencePcc.requestAccess(
            context, deviceNumber.toInt(), -1, false, accessResultReceiver, stateChangeReceiver
        )
    }

    /**
     * Disconnects the device
     */
    override fun disconnect() {
        Handler(Looper.getMainLooper()).post {
            onLogDataListener.add(
                LogData(
                    LogEvent.VERBOSE, TAG, "<disconnect>:"
                )
            )
        }
        if (releaseHandle != null) {
            releaseHandle!!.close()
        }
        if (hrPcc != null) {
            Handler(Looper.getMainLooper()).post {
                onLogDataListener.add(
                    LogData(
                        LogEvent.VERBOSE, TAG, "(disconnect): deviceNumber " + hrPcc?.antDeviceNumber
                    )
                )
            }
            hrPcc?.subscribeCalculatedCadenceEvent(null)
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
        // closes cadence listener
        onCadenceDataListener.onEventsDone()
    }
}