package me.heyteacher.flutter_antplus.bikepower

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IDeviceStateChangeReceiver
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IPluginAccessResultReceiver
import com.dsi.ant.plugins.antplus.pccbase.AsyncScanController
import com.dsi.ant.plugins.antplus.pccbase.AsyncScanController.AsyncScanResultDeviceInfo
import com.dsi.ant.plugins.antplus.pccbase.AsyncScanController.IAsyncScanResultReceiver
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle
import io.flutter.plugin.common.BinaryMessenger
import me.heyteacher.flutter_antplus.bikepower.pigeons.AntplusBatteryStatus
import me.heyteacher.flutter_antplus.bikepower.pigeons.BikepowerHostApi
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnBalanceDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnBatteryStatusDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnCadenceDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnPedalSmoothnessDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnPowerDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.OnTorqueEffectivenessDataStreamHandler
import me.heyteacher.flutter_antplus.bikepower.pigeons.AntplusPedalSmoothnessData
import me.heyteacher.flutter_antplus.bikepower.pigeons.AntplusTorqueEffectivenessData
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

class BikePowerHostApiImpl(
    private val context: Context,
    binaryMessenger: BinaryMessenger,
    private var onLogDataListener: OnLogDataListener
) : BikepowerHostApi {

    companion object {
        private val TAG: String = BikePowerHostApiImpl::class.java.simpleName
    }

    private var releaseHandle: PccReleaseHandle<MyAntPlusBikePowerPcc>? = null
    private var pwrPcc: MyAntPlusBikePowerPcc? = null
    private var pwrScanCtrl: AsyncScanController<MyAntPlusBikePowerPcc>? = null

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
    private val stateChangeReceiver = IDeviceStateChangeReceiver { deviceState ->
        Handler(Looper.getMainLooper()).post {
            onLogDataListener.add(
                AntplusLogData(
                    AntplusLogEvent.VERBOSE,
                    TAG,
                    "<onDeviceStateChange>:  " + pwrPcc?.antDeviceNumber + " deviceState " + deviceState
                )
            )
        }
        Handler(Looper.getMainLooper()).post {
            onDeviceStateChangeListener.add(PigeonDeviceState.valueOf(deviceState.toString()))
        }
    }
    private val accessResultReceiver: IPluginAccessResultReceiver<MyAntPlusBikePowerPcc> =
        IPluginAccessResultReceiver { result, resultCode, initialDeviceState ->
            //Handle the result, connecting to events on success or reporting failure to user.
            when (resultCode) {
                RequestAccessResult.SUCCESS -> {
                    pwrPcc = result
                    pwrPcc!!.setOnLogDataListener(onLogDataListener)
                    Handler(Looper.getMainLooper()).post {
                        onRequestAccessResultListener.add(
                            me.heyteacher.flutter_antplus.device.pigeons.AntplusRequestAccessResult.valueOf(
                                resultCode.name
                            )
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
                                "(onResultReceived): resultCode ${resultCode.name}. deviceNumber " + (result?.antDeviceNumber
                                    ?: "") + " initialDeviceState " + initialDeviceState
                            )
                        )
                    }
                    pwrPcc!!.subscribeCalculatedPowerEvent(object :
                        MyAntPlusBikePowerPcc.ICalculatedPowerReceiver {
                        override fun onNewCalculatedPower(
                            estTimestamp: Long,
                            eventFlags: EnumSet<EventFlag?>?,
                            dataSource: MyAntPlusBikePowerPcc.DataSource?,
                            calculatedPower: BigDecimal?
                        ) {
                            Handler(Looper.getMainLooper()).post {
                                onPowerDataListener.add(calculatedPower!!.toLong())
                            }
                        }
                    })
                    pwrPcc!!.subscribeCalculatedCrankCadenceEvent(object :
                        MyAntPlusBikePowerPcc.ICalculatedCrankCadenceReceiver {
                        override fun onNewCalculatedCrankCadence(
                            estTimestamp: Long,
                            eventFlags: EnumSet<EventFlag?>?,
                            dataSource: MyAntPlusBikePowerPcc.DataSource?,
                            calculatedCrankCadence: BigDecimal?
                        ) {
                            Handler(Looper.getMainLooper()).post {
                                onCadenceDataListener.add(calculatedCrankCadence!!.toLong())
                            }
                        }

                    })
                    pwrPcc!!.subscribePedalPowerBalanceEvent((object :
                        MyAntPlusBikePowerPcc.IPedalPowerBalanceReceiver {
                        override fun onNewPedalPowerBalance(
                            estTimestamp: Long,
                            eventFlags: EnumSet<EventFlag?>?,
                            rightPedalIndicator: Boolean,
                            pedalPowerPercentage: Int
                        ) {
                            Handler(Looper.getMainLooper()).post {
                                onBalanceDataListener.add(pedalPowerPercentage.toLong())
                            }
                        }
                    }))
                    pwrPcc!!.subscribePedalSmoothnessEvent(object :
                        MyAntPlusBikePowerPcc.IPedalSmoothnessReceiver {
                        override fun onNewPedalSmoothness(
                            estTimestamp: Long,
                            eventFlags: EnumSet<EventFlag?>?,
                            powerOnlyUpdateEventCount: Long,
                            separatePedalSmoothnessSupport: Boolean,
                            leftOrCombinedPedalSmoothness: BigDecimal?,
                            rightPedalSmoothness: BigDecimal?
                        ) {
                            Handler(Looper.getMainLooper()).post {
                                onPedalSmoothnessDataListener.add(
                                    AntplusPedalSmoothnessData(
                                        separatePedalSmoothnessSupport,
                                        leftOrCombinedPedalSmoothness!!.toDouble(),
                                        rightPedalSmoothness!!.toDouble()
                                    )
                                )
                            }
                        }
                    })
                    pwrPcc!!.subscribeTorqueEffectivenessEvent(object :
                        MyAntPlusBikePowerPcc.ITorqueEffectivenessReceiver {
                        override fun onNewTorqueEffectiveness(
                            estTimestamp: Long,
                            eventFlags: EnumSet<EventFlag?>?,
                            powerOnlyUpdateEventCount: Long,
                            leftTorqueEffectiveness: BigDecimal?,
                            rightTorqueEffectiveness: BigDecimal?
                        ) {
                            Handler(Looper.getMainLooper()).post {
                                onTorqueEffectivenessDataListener.add(
                                    AntplusTorqueEffectivenessData(
                                        leftTorqueEffectiveness!!.toDouble(),
                                        rightTorqueEffectiveness!!.toDouble()
                                    )
                                )
                            }
                        }
                    })
                    pwrPcc!!.subscribeBatteryStatusEvent { _, _, _, _, batteryStatus, _, _, _ ->
                        Handler(Looper.getMainLooper()).post {
                            onBatteryStatusListener.add(
                                when (batteryStatus) {
                                    com.dsi.ant.plugins.antplus.pcc.defines.BatteryStatus.NEW -> AntplusBatteryStatus.NEW_STATUS
                                    else -> AntplusBatteryStatus.valueOf(batteryStatus.toString())
                                }
                            )
                        }
                    }
                    // estTimestamp, eventFlags, numOfDataTypes, dataType, timeStamp, measurementValue
                }

                RequestAccessResult.DEPENDENCY_NOT_INSTALLED -> Handler(Looper.getMainLooper()).post {
                    onRequestAccessResultListener.add(
                        me.heyteacher.flutter_antplus.device.pigeons.AntplusRequestAccessResult.valueOf(
                            resultCode.name
                        )
                    )
                    onLogDataListener.add(
                        AntplusLogData(
                            AntplusLogEvent.ERROR,
                            TAG,
                            "(onResultReceived): resultCode ${RequestAccessResult.DEPENDENCY_NOT_INSTALLED.name}. " +
                                    "The required service \"${AntPluginPcc.getMissingDependencyName()}\" was not found. " +
                                    "You need to install the ANT+ Plugins service or you may " +
                                    "need to update your existing version if you already have it. " +
                                    "Do you want to launch the Play Store to get it?"
                        )
                    )
                }

                else -> {
                    Handler(Looper.getMainLooper()).post {
                        onRequestAccessResultListener.add(
                            me.heyteacher.flutter_antplus.device.pigeons.AntplusRequestAccessResult.valueOf(
                                resultCode.name
                            )
                        )
                        onLogDataListener.add(
                            AntplusLogData(
                                AntplusLogEvent.WARNING,
                                TAG,
                                "(onResultReceived): resultCode ${resultCode.name}"
                            )
                        )
                    }
                }
            }
        }

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
    }

    override fun startScan() {
        Handler(Looper.getMainLooper()).post {
            onLogDataListener.add(
                AntplusLogData(
                    AntplusLogEvent.VERBOSE, TAG, "<startScan>:"
                )
            )
        }
        pwrScanCtrl = MyAntPlusBikePowerPcc.requestAsyncScanController(
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
                                AntplusDeviceType.BIKEPOWER
                            )
                        )
                    }
                }
            })}

    override fun stopScan() {
        if (pwrScanCtrl != null) {
            pwrScanCtrl!!.closeScanController()
        }
        pwrScanCtrl = null
    }

    override fun connect(deviceNumber: Long) {
        Handler(Looper.getMainLooper()).post {
            onLogDataListener.add(
                AntplusLogData(
                    AntplusLogEvent.VERBOSE, TAG, "<connect>: deviceNumber $deviceNumber"
                )
            )
        }
        stopScan()
        releaseHandle = MyAntPlusBikePowerPcc.requestAccess(
            context, deviceNumber.toInt(), -1, accessResultReceiver, stateChangeReceiver
        )
    }

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
        if (pwrPcc != null) {
            Handler(Looper.getMainLooper()).post {
                onLogDataListener.add(
                    AntplusLogData(
                        AntplusLogEvent.VERBOSE,
                        TAG,
                        "(disconnect): deviceNumber " + pwrPcc?.antDeviceNumber
                    )
                )
            }
            pwrPcc?.subscribeCalculatedPowerEvent(null)
            pwrPcc?.releaseAccess()
        }
        releaseHandle = null
        pwrPcc = null
    }

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
    }
}