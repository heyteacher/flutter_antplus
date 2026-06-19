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
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDevice
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDeviceType
import me.heyteacher.flutter_antplus.device.pigeons.OnDeviceStateChangeStreamHandler
import me.heyteacher.flutter_antplus.device.pigeons.OnRequestAccessResultStreamHandler
import me.heyteacher.flutter_antplus.device.pigeons.OnScanResultStreamHandler
import me.heyteacher.flutter_antplus.logging.OnLogDataListener
import me.heyteacher.flutter_antplus.logging.pigeons.AntplusLogData
import me.heyteacher.flutter_antplus.logging.pigeons.AntplusLogEvent
import java.math.BigDecimal
import java.util.EnumSet
import me.heyteacher.flutter_antplus.device.pigeons.AntplusDeviceState as PigeonDeviceState
import me.heyteacher.flutter_antplus.device.pigeons.AntplusRequestAccessResult as PigeonRequestAccessResult

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
    private var bcPcc: AntPlusBikeCadencePcc? = null

    private val stateChangeReceiver = IDeviceStateChangeReceiver { deviceState ->
        Handler(Looper.getMainLooper()).post {
            onLogDataListener.add(
                AntplusLogData(
                    AntplusLogEvent.VERBOSE,
                    TAG,
                    "<onDeviceStateChange>: " + bcPcc?.antDeviceNumber + " deviceState " + deviceState
                )
            )
            onDeviceStateChangeListener.add(PigeonDeviceState.valueOf(deviceState.toString()))
        }
    }
    private val accessResultReceiver: IPluginAccessResultReceiver<AntPlusBikeCadencePcc?> =
        IPluginAccessResultReceiver { result, resultCode, initialDeviceState ->
            when (resultCode) {
                RequestAccessResult.SUCCESS -> {
                    bcPcc = result
                    bcPcc!!.subscribeCalculatedCadenceEvent { _: Long, _: EnumSet<EventFlag?>?, computedCadence: BigDecimal ->
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
                            AntplusLogData(
                                AntplusLogEvent.WARNING, TAG, "<onResultReceived>: resultCode $resultCode"
                            )
                        )
                    }
                }
            }
        }
    private var bcScanCtrl: AsyncScanController<AntPlusBikeCadencePcc>? = null

    /**
     * Initializes
     */
    init {
        CadenceHostApi.setUp(binaryMessenger, this)
        OnCadenceDataStreamHandler.register(binaryMessenger, onCadenceDataListener)
        OnDeviceStateChangeStreamHandler.register(
            binaryMessenger, onDeviceStateChangeListener, AntplusDeviceType.CADENCE.name
        )
        OnScanResultStreamHandler.register(
            binaryMessenger, onScanResultListener, AntplusDeviceType.CADENCE.name
        )
        OnRequestAccessResultStreamHandler.register(
            binaryMessenger, onRequestAccessResultListener, AntplusDeviceType.CADENCE.name
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
        bcScanCtrl = AntPlusBikeCadencePcc.requestAsyncScanController(
            context,
            0,
            object : AntPlusBikeSpdCadCommonPcc.IBikeSpdCadAsyncScanResultReceiver {
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

                override fun onSearchResult(deviceFound: AntPlusBikeSpdCadCommonPcc.BikeSpdCadAsyncScanResultDeviceInfo?) {
                    Handler(Looper.getMainLooper()).post {
                        onLogDataListener.add(
                            AntplusLogData(
                                AntplusLogEvent.VERBOSE,
                                TAG,
                                "<onSearchResult>: number " + deviceFound?.resultInfo?.antDeviceNumber + " name " + deviceFound?.resultInfo?.deviceDisplayName + " already connected " + deviceFound?.resultInfo?.isAlreadyConnected
                            )
                        )
                        if (deviceFound?.resultInfo != null) {
                            onScanResultListener.add(
                                AntplusDevice(
                                    deviceFound.resultInfo.antDeviceNumber.toLong(),
                                    deviceFound.resultInfo.deviceDisplayName,
                                    AntplusDeviceType.CADENCE
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
                AntplusLogData(
                    AntplusLogEvent.VERBOSE, TAG, "<stopScan>:"
                )
            )
        }
        if (bcScanCtrl != null) {
            bcScanCtrl!!.closeScanController()
        }
        bcScanCtrl = null
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
                AntplusLogData(
                    AntplusLogEvent.VERBOSE, TAG, "<disconnect>:"
                )
            )
        }
        if (releaseHandle != null) {
            releaseHandle!!.close()
        }
        if (bcPcc != null) {
            Handler(Looper.getMainLooper()).post {
                onLogDataListener.add(
                    AntplusLogData(
                        AntplusLogEvent.VERBOSE, TAG, "(disconnect): deviceNumber " + bcPcc?.antDeviceNumber
                    )
                )
            }
            bcPcc?.subscribeCalculatedCadenceEvent(null)
            bcPcc?.releaseAccess()
        }
        releaseHandle = null
        bcPcc = null
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