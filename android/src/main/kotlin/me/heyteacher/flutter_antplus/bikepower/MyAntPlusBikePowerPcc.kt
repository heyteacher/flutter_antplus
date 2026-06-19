package me.heyteacher.flutter_antplus.bikepower

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Parcel
import android.os.Parcelable
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag
import com.dsi.ant.plugins.antplus.pcc.defines.RequestStatus
import com.dsi.ant.plugins.antplus.pccbase.AntPlusCommonPcc
import com.dsi.ant.plugins.antplus.pccbase.AsyncScanController
import com.dsi.ant.plugins.antplus.pccbase.AsyncScanController.IAsyncScanResultReceiver
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle
import com.dsi.ant.plugins.utility.log.LogAnt
import me.heyteacher.flutter_antplus.logging.OnLogDataListener
import me.heyteacher.flutter_antplus.logging.pigeons.AntplusLogData
import me.heyteacher.flutter_antplus.logging.pigeons.AntplusLogEvent
import java.math.BigDecimal
import java.util.EnumSet


class MyAntPlusBikePowerPcc private constructor() : AntPlusCommonPcc() {
    private var mRawPowerOnlyDataReceiver: IRawPowerOnlyDataReceiver? = null
    private var mPedalPowerBalanceReceiver: IPedalPowerBalanceReceiver? = null
    private var mInstantaneousCadenceReceiver: IInstantaneousCadenceReceiver? = null
    private var mRawWheelTorqueDataReceiver: IRawWheelTorqueDataReceiver? = null
    private var mRawCrankTorqueDataReceiver: IRawCrankTorqueDataReceiver? = null
    private var mTorqueEffectivenessReceiver: ITorqueEffectivenessReceiver? = null
    private var mPedalSmoothnessReceiver: IPedalSmoothnessReceiver? = null
    private var mRawCtfDataReceiver: IRawCtfDataReceiver? = null
    private var mCalibrationMessageReceiver: ICalibrationMessageReceiver? = null
    private var mRequestCalibrationMessageReceiver: ICalibrationMessageReceiver? = null
    private var mAutoZeroStatusReceiver: IAutoZeroStatusReceiver? = null
    private var mRequestAutoZeroStatusReceiver: IAutoZeroStatusReceiver? = null
    private var mCalculatedPowerReceiver: ICalculatedPowerReceiver? = null
    private var mCalculatedTorqueReceiver: ICalculatedTorqueReceiver? = null
    private var mCalculatedCrankCadenceReceiver: ICalculatedCrankCadenceReceiver? = null
    private var mCalculatedWheelSpeedReceiver: CalculatedWheelSpeedReceiver? = null
    private var mCalculatedWheelDistanceReceiver: CalculatedWheelDistanceReceiver? = null
    private var mMeasurementOutputDataReceiver: IMeasurementOutputDataReceiver? = null
    private var mRequestMeasurementOutputDataReceiver: IMeasurementOutputDataReceiver? = null
    private var mCrankParametersReceiver: ICrankParametersReceiver? = null
    private var mRequestCrankParametersReceiver: ICrankParametersReceiver? = null
    private var pccHandler: Handler = Handler()
    private var unsubscribeRequestCalibrationMessageReceiver: Runnable =
        Runnable { this@MyAntPlusBikePowerPcc.subscribeRequestCalibrationMessageEvent(null) }
    private var unsubscribeRequestAutoZeroStatusReceiver: Runnable =
        Runnable { this@MyAntPlusBikePowerPcc.subscribeRequestAutoZeroStatusEvent(null) }
    private var unsubscribeRequestMeasurementOutputDataReceiver: Runnable =
        Runnable { this@MyAntPlusBikePowerPcc.subscribeRequestMeasurementOutputDataEvent(null) }
    private var unsubscribeRequestCrankParametersReceiver: Runnable =
        Runnable { this@MyAntPlusBikePowerPcc.subscribeRequestCrankParametersEvent(null) }

    private lateinit var onLogDataListener: OnLogDataListener

    override fun getRequiredServiceVersionForBind(): Int {
        return 10800
    }

    override fun getServiceBindIntent(): Intent {
        val it = Intent()
        it.setComponent(
            ComponentName(
                "com.dsi.ant.plugins.antplus",
                "com.dsi.ant.plugins.antplus.bikepower.BikePowerService"
            )
        )
        return it
    }

    override fun getPluginPrintableName(): String {
        return "ANT+ Plugin: Bike Power"
    }

    override fun handlePluginEvent(eventMsg: Message) {
        Handler(Looper.getMainLooper()).post {
            onLogDataListener.add(
                AntplusLogData(
                    AntplusLogEvent.DEBUG,
                    TAG,
                    "<handlePluginEvent> arg1 " + eventMsg.arg1 + " keys " + eventMsg.data.keySet()
                        .toTypedArray().contentToString()
                )
            )
        }
        val eventFlags: EnumSet<EventFlag?>
        val dataType: Int
        val timeStamp: BigDecimal?
        val measurementValue: BigDecimal?
        val accumulatedCrankTorque: BigDecimal?
        val b: Bundle
        val estTimestamp: Long
        val dataSource: DataSource
        val calculatedWheelDistance: BigDecimal?
        val ctfUpdateEventCount: Long
        val accumulatedCrankTicks: Long
        val rightPedalSmoothness: BigDecimal?
        when (eventMsg.arg1) {
            201 -> if (this.mRawPowerOnlyDataReceiver != null) {
                b = eventMsg.data
                estTimestamp = b.getLong("long_EstTimestamp")
                eventFlags = EventFlag.getEventFlagsFromLong(b.getLong("long_EventFlags"))
                ctfUpdateEventCount = b.getLong("long_powerOnlyUpdateEventCount")
                val instantaneousPower = b.getInt("int_instantaneousPower")
                val accumulatedPower = b.getLong("long_accumulatedPower")
                mRawPowerOnlyDataReceiver!!.onNewRawPowerOnlyData(
                    estTimestamp,
                    eventFlags,
                    ctfUpdateEventCount,
                    instantaneousPower,
                    accumulatedPower
                )
            }

            202 -> if (this.mPedalPowerBalanceReceiver != null) {
                b = eventMsg.data
                estTimestamp = b.getLong("long_EstTimestamp")
                eventFlags = EventFlag.getEventFlagsFromLong(b.getLong("long_EventFlags"))
                val rightPedalIndicator = b.getBoolean("bool_rightPedalIndicator")
                dataType = b.getInt("int_pedalPowerPercentage")
                mPedalPowerBalanceReceiver!!.onNewPedalPowerBalance(
                    estTimestamp, eventFlags, rightPedalIndicator, dataType
                )
            }

            203 -> if (this.mInstantaneousCadenceReceiver != null) {
                b = eventMsg.data
                estTimestamp = b.getLong("long_EstTimestamp")
                eventFlags = EventFlag.getEventFlagsFromLong(b.getLong("long_EventFlags"))
                dataSource = DataSource.getValueFromInt(b.getInt("int_dataSource"))
                dataType = b.getInt("int_instantaneousCadence")
                mInstantaneousCadenceReceiver!!.onNewInstantaneousCadence(
                    estTimestamp, eventFlags, dataSource, dataType
                )
            }

            204 -> if (this.mRawWheelTorqueDataReceiver != null) {
                b = eventMsg.data
                estTimestamp = b.getLong("long_EstTimestamp")
                eventFlags = EventFlag.getEventFlagsFromLong(b.getLong("long_EventFlags"))
                ctfUpdateEventCount = b.getLong("long_wheelTorqueUpdateEventCount")
                accumulatedCrankTicks = b.getLong("long_accumulatedWheelTicks")
                rightPedalSmoothness =
                    b.getSerializable("decimal_accumulatedWheelPeriod") as BigDecimal?
                accumulatedCrankTorque =
                    b.getSerializable("decimal_accumulatedWheelTorque") as BigDecimal?
                mRawWheelTorqueDataReceiver!!.onNewRawWheelTorqueData(
                    estTimestamp,
                    eventFlags,
                    ctfUpdateEventCount,
                    accumulatedCrankTicks,
                    rightPedalSmoothness,
                    accumulatedCrankTorque
                )
            }

            205 -> if (this.mRawCrankTorqueDataReceiver != null) {
                b = eventMsg.data
                estTimestamp = b.getLong("long_EstTimestamp")
                eventFlags = EventFlag.getEventFlagsFromLong(b.getLong("long_EventFlags"))
                ctfUpdateEventCount = b.getLong("long_crankTorqueUpdateEventCount")
                accumulatedCrankTicks = b.getLong("long_accumulatedCrankTicks")
                rightPedalSmoothness =
                    b.getSerializable("decimal_accumulatedCrankPeriod") as BigDecimal?
                accumulatedCrankTorque =
                    b.getSerializable("decimal_accumulatedCrankTorque") as BigDecimal?
                mRawCrankTorqueDataReceiver!!.onNewRawCrankTorqueData(
                    estTimestamp,
                    eventFlags,
                    ctfUpdateEventCount,
                    accumulatedCrankTicks,
                    rightPedalSmoothness,
                    accumulatedCrankTorque
                )
            }

            206 -> if (this.mTorqueEffectivenessReceiver != null) {
                b = eventMsg.data
                estTimestamp = b.getLong("long_EstTimestamp")
                eventFlags = EventFlag.getEventFlagsFromLong(b.getLong("long_EventFlags"))
                ctfUpdateEventCount = b.getLong("long_powerOnlyUpdateEventCount")
                timeStamp = b.getSerializable("decimal_leftTorqueEffectiveness") as BigDecimal?
                measurementValue =
                    b.getSerializable("decimal_rightTorqueEffectiveness") as BigDecimal?
                mTorqueEffectivenessReceiver!!.onNewTorqueEffectiveness(
                    estTimestamp, eventFlags, ctfUpdateEventCount, timeStamp, measurementValue
                )
            }

            207 -> if (this.mPedalSmoothnessReceiver != null) {
                b = eventMsg.data
                estTimestamp = b.getLong("long_EstTimestamp")
                eventFlags = EventFlag.getEventFlagsFromLong(b.getLong("long_EventFlags"))
                ctfUpdateEventCount = b.getLong("long_powerOnlyUpdateEventCount")
                val separatePedalSmoothnessSupport =
                    b.getBoolean("bool_separatePedalSmoothnessSupport")
                measurementValue =
                    b.getSerializable("decimal_leftOrCombinedPedalSmoothness") as BigDecimal?
                rightPedalSmoothness =
                    b.getSerializable("decimal_rightPedalSmoothness") as BigDecimal?
                mPedalSmoothnessReceiver!!.onNewPedalSmoothness(
                    estTimestamp,
                    eventFlags,
                    ctfUpdateEventCount,
                    separatePedalSmoothnessSupport,
                    measurementValue,
                    rightPedalSmoothness
                )
            }

            208 -> if (this.mRawCtfDataReceiver != null) {
                b = eventMsg.data
                estTimestamp = b.getLong("long_EstTimestamp")
                eventFlags = EventFlag.getEventFlagsFromLong(b.getLong("long_EventFlags"))
                ctfUpdateEventCount = b.getLong("long_ctfUpdateEventCount")
                timeStamp = b.getSerializable("decimal_instantaneousSlope") as BigDecimal?
                measurementValue = b.getSerializable("decimal_accumulatedTimeStamp") as BigDecimal?
                val accumulatedTorqueTicksStamp = b.getLong("long_accumulatedTorqueTicksStamp")
                mRawCtfDataReceiver!!.onNewRawCtfData(
                    estTimestamp,
                    eventFlags,
                    ctfUpdateEventCount,
                    timeStamp,
                    measurementValue,
                    accumulatedTorqueTicksStamp
                )
            }

            209 -> if (this.mCalibrationMessageReceiver != null || this.mRequestCalibrationMessageReceiver != null) {
                b = eventMsg.data
                b.classLoader = javaClass.classLoader
                estTimestamp = b.getLong("long_EstTimestamp")
                eventFlags = EventFlag.getEventFlagsFromLong(b.getLong("long_EventFlags"))
                if (this.mCalibrationMessageReceiver != null) {
                    mCalibrationMessageReceiver!!.onNewCalibrationMessage(
                        estTimestamp, eventFlags, b.getParcelable("parcelable_CalibrationMessage")
                    )
                }

                if (this.mRequestCalibrationMessageReceiver != null) {
                    mRequestCalibrationMessageReceiver!!.onNewCalibrationMessage(
                        estTimestamp, eventFlags, b.getParcelable("parcelable_CalibrationMessage")
                    )
                    pccHandler.removeCallbacksAndMessages(this.unsubscribeRequestCalibrationMessageReceiver)
                    pccHandler.postDelayed(this.unsubscribeRequestCalibrationMessageReceiver, 5000L)
                }
            }

            210 -> if (this.mAutoZeroStatusReceiver != null || this.mRequestAutoZeroStatusReceiver != null) {
                b = eventMsg.data
                estTimestamp = b.getLong("long_EstTimestamp")
                eventFlags = EventFlag.getEventFlagsFromLong(b.getLong("long_EventFlags"))
                val autoZeroStatus = AutoZeroStatus.getValueFromInt(b.getInt("int_autoZeroStatus"))
                if (this.mAutoZeroStatusReceiver != null) {
                    mAutoZeroStatusReceiver!!.onNewAutoZeroStatus(
                        estTimestamp, eventFlags, autoZeroStatus
                    )
                }

                if (this.mRequestAutoZeroStatusReceiver != null) {
                    mRequestAutoZeroStatusReceiver!!.onNewAutoZeroStatus(
                        estTimestamp, eventFlags, autoZeroStatus
                    )
                    pccHandler.removeCallbacksAndMessages(this.unsubscribeRequestAutoZeroStatusReceiver)
                    pccHandler.postDelayed(this.unsubscribeRequestAutoZeroStatusReceiver, 5000L)
                }
            }

            211 -> if (this.mCalculatedPowerReceiver != null) {
                b = eventMsg.data
                estTimestamp = b.getLong("long_EstTimestamp")
                eventFlags = EventFlag.getEventFlagsFromLong(b.getLong("long_EventFlags"))
                dataSource = DataSource.getValueFromInt(b.getInt("int_dataSource"))
                calculatedWheelDistance =
                    b.getSerializable("decimal_calculatedPower") as BigDecimal?
                mCalculatedPowerReceiver!!.onNewCalculatedPower(
                    estTimestamp, eventFlags, dataSource, calculatedWheelDistance
                )
            }

            212 -> if (this.mCalculatedTorqueReceiver != null) {
                b = eventMsg.data
                estTimestamp = b.getLong("long_EstTimestamp")
                eventFlags = EventFlag.getEventFlagsFromLong(b.getLong("long_EventFlags"))
                dataSource = DataSource.getValueFromInt(b.getInt("int_dataSource"))
                calculatedWheelDistance =
                    b.getSerializable("decimal_calculatedTorque") as BigDecimal?
                mCalculatedTorqueReceiver!!.onNewCalculatedTorque(
                    estTimestamp, eventFlags, dataSource, calculatedWheelDistance
                )
            }

            213 -> if (this.mCalculatedCrankCadenceReceiver != null) {
                b = eventMsg.data
                estTimestamp = b.getLong("long_EstTimestamp")
                eventFlags = EventFlag.getEventFlagsFromLong(b.getLong("long_EventFlags"))
                dataSource = DataSource.getValueFromInt(b.getInt("int_dataSource"))
                calculatedWheelDistance =
                    b.getSerializable("decimal_calculatedCrankCadence") as BigDecimal?
                mCalculatedCrankCadenceReceiver!!.onNewCalculatedCrankCadence(
                    estTimestamp, eventFlags, dataSource, calculatedWheelDistance
                )
            }

            214 -> if (this.mCalculatedWheelSpeedReceiver != null) {
                b = eventMsg.data
                estTimestamp = b.getLong("long_EstTimestamp")
                eventFlags = EventFlag.getEventFlagsFromLong(b.getLong("long_EventFlags"))
                dataSource = DataSource.getValueFromInt(b.getInt("int_dataSource"))
                calculatedWheelDistance =
                    b.getSerializable("decimal_calculatedWheelSpeed") as BigDecimal?
                checkNotNull(calculatedWheelDistance)
                mCalculatedWheelSpeedReceiver!!.onNewRawCalculatedWheelSpeed(
                    estTimestamp, eventFlags, dataSource, calculatedWheelDistance
                )
            }

            215 -> if (this.mCalculatedWheelDistanceReceiver != null) {
                b = eventMsg.data
                estTimestamp = b.getLong("long_EstTimestamp")
                eventFlags = EventFlag.getEventFlagsFromLong(b.getLong("long_EventFlags"))
                dataSource = DataSource.getValueFromInt(b.getInt("int_dataSource"))
                calculatedWheelDistance =
                    b.getSerializable("decimal_calculatedWheelDistance") as BigDecimal?
                mCalculatedWheelDistanceReceiver!!.onNewRawCalculatedWheelDistance(
                    estTimestamp, eventFlags, dataSource, calculatedWheelDistance!!
                )
            }

            216 -> if (this.mMeasurementOutputDataReceiver != null || this.mRequestMeasurementOutputDataReceiver != null) {
                b = eventMsg.data
                estTimestamp = b.getLong("long_EstTimestamp")
                eventFlags = EventFlag.getEventFlagsFromLong(b.getLong("long_EventFlags"))
                val numOfDataTypes = b.getInt("int_numOfDataTypes")
                dataType = b.getInt("int_dataType")
                Handler(Looper.getMainLooper()).post {
                    onLogDataListener.add(
                        AntplusLogData(
                            AntplusLogEvent.DEBUG,
                            TAG,
                            "MeasurementOutput (arg1 " + eventMsg.arg1 + ") keys " + eventMsg.data.keySet()
                                .toTypedArray()
                                .contentToString() + " dataType " + dataType + " (numOfDataTypes " + numOfDataTypes + ")"
                        )
                    )
                }
                timeStamp = b.getSerializable("decimal_timeStamp") as BigDecimal?
                measurementValue = b.getSerializable("decimal_measurementValue") as BigDecimal?
                if (this.mMeasurementOutputDataReceiver != null) {
                    mMeasurementOutputDataReceiver!!.onNewMeasurementOutputData(
                        estTimestamp,
                        eventFlags,
                        numOfDataTypes,
                        dataType,
                        timeStamp,
                        measurementValue
                    )
                    Handler(Looper.getMainLooper()).post {
                        onLogDataListener.add(
                            AntplusLogData(
                                AntplusLogEvent.DEBUG,
                                TAG,
                                "MeasurementOutput dataType $dataType measurementValue $measurementValue"
                            )
                        )
                    }
                }

                if (this.mRequestMeasurementOutputDataReceiver != null) {
                    mRequestMeasurementOutputDataReceiver!!.onNewMeasurementOutputData(
                        estTimestamp,
                        eventFlags,
                        numOfDataTypes,
                        dataType,
                        timeStamp,
                        measurementValue
                    )
                    pccHandler.removeCallbacksAndMessages(this.unsubscribeRequestMeasurementOutputDataReceiver)
                    pccHandler.postDelayed(
                        this.unsubscribeRequestMeasurementOutputDataReceiver, 5000L
                    )
                }
            }

            217 -> if (this.mCrankParametersReceiver != null || this.mRequestCrankParametersReceiver != null) {
                b = eventMsg.data
                b.classLoader = javaClass.classLoader
                estTimestamp = b.getLong("long_EstTimestamp")
                eventFlags = EventFlag.getEventFlagsFromLong(b.getLong("long_EventFlags"))
                if (this.mCrankParametersReceiver != null) {
                    mCrankParametersReceiver!!.onNewCrankParameters(
                        estTimestamp, eventFlags, b.getParcelable("parcelable_CrankParameters")
                    )
                }

                if (this.mRequestCrankParametersReceiver != null) {
                    mRequestCrankParametersReceiver!!.onNewCrankParameters(
                        estTimestamp, eventFlags, b.getParcelable("parcelable_CrankParameters")
                    )
                    pccHandler.removeCallbacksAndMessages(this.unsubscribeRequestCrankParametersReceiver)
                    pccHandler.postDelayed(this.unsubscribeRequestCrankParametersReceiver, 5000L)
                }
            }

            218 -> {
                val tempReceiver = this.mRequestFinishedReceiver
                this.mRequestFinishedReceiver = null
                mCommandLock.release()
                if (tempReceiver != null) {
                    b = eventMsg.data
                    val requestStatus = b.getInt("int_requestStatus")
                    tempReceiver.onNewRequestFinished(RequestStatus.getValueFromInt(requestStatus))
                }
            }

            100, 101, 102, 103, 104, 105, 106, 107, 108, 109 -> super.handlePluginEvent(eventMsg)
        }
    }

    fun subscribeRawPowerOnlyDataEvent(rawPowerOnlyDataReceiver: IRawPowerOnlyDataReceiver?) {
        this.mRawPowerOnlyDataReceiver = rawPowerOnlyDataReceiver
        if (rawPowerOnlyDataReceiver != null) {
            this.subscribeToEvent(201)
        } else {
            this.unsubscribeFromEvent(201)
        }
    }

    fun subscribePedalPowerBalanceEvent(pedalPowerBalanceReceiver: IPedalPowerBalanceReceiver?) {
        this.mPedalPowerBalanceReceiver = pedalPowerBalanceReceiver
        if (pedalPowerBalanceReceiver != null) {
            this.subscribeToEvent(202)
        } else {
            this.unsubscribeFromEvent(202)
        }
    }

    fun subscribeInstantaneousCadenceEvent(instantaneousCadenceReceiver: IInstantaneousCadenceReceiver?) {
        this.mInstantaneousCadenceReceiver = instantaneousCadenceReceiver
        if (instantaneousCadenceReceiver != null) {
            this.subscribeToEvent(203)
        } else {
            this.unsubscribeFromEvent(203)
        }
    }

    fun subscribeRawWheelTorqueDataEvent(rawWheelTorqueDataReceiver: IRawWheelTorqueDataReceiver?) {
        this.mRawWheelTorqueDataReceiver = rawWheelTorqueDataReceiver
        if (rawWheelTorqueDataReceiver != null) {
            this.subscribeToEvent(204)
        } else {
            this.unsubscribeFromEvent(204)
        }
    }

    fun subscribeRawCrankTorqueDataEvent(rawCrankTorqueDataReceiver: IRawCrankTorqueDataReceiver?) {
        this.mRawCrankTorqueDataReceiver = rawCrankTorqueDataReceiver
        if (rawCrankTorqueDataReceiver != null) {
            this.subscribeToEvent(205)
        } else {
            this.unsubscribeFromEvent(205)
        }
    }

    fun subscribeTorqueEffectivenessEvent(torqueEffectivenessReceiver: ITorqueEffectivenessReceiver?) {
        this.mTorqueEffectivenessReceiver = torqueEffectivenessReceiver
        if (torqueEffectivenessReceiver != null) {
            this.subscribeToEvent(206)
        } else {
            this.unsubscribeFromEvent(206)
        }
    }

    fun subscribePedalSmoothnessEvent(pedalSmoothnessReceiver: IPedalSmoothnessReceiver?) {
        this.mPedalSmoothnessReceiver = pedalSmoothnessReceiver
        if (pedalSmoothnessReceiver != null) {
            this.subscribeToEvent(207)
        } else {
            this.unsubscribeFromEvent(207)
        }
    }

    fun subscribeRawCtfDataEvent(rawCtfDataReceiver: IRawCtfDataReceiver?) {
        this.mRawCtfDataReceiver = rawCtfDataReceiver
        if (rawCtfDataReceiver != null) {
            this.subscribeToEvent(208)
        } else {
            this.unsubscribeFromEvent(208)
        }
    }

    fun subscribeCalibrationMessageEvent(calibrationMessageReceiver: ICalibrationMessageReceiver?) {
        if (this.mRequestCalibrationMessageReceiver == null) {
            if (calibrationMessageReceiver != null && this.mCalibrationMessageReceiver == null) {
                this.subscribeToEvent(209)
            } else if (calibrationMessageReceiver == null && this.mCalibrationMessageReceiver != null) {
                this.unsubscribeFromEvent(209)
            }
        }

        this.mCalibrationMessageReceiver = calibrationMessageReceiver
    }

    private fun subscribeRequestCalibrationMessageEvent(calibrationMessageReceiver: ICalibrationMessageReceiver?) {
        if (this.mCalibrationMessageReceiver == null) {
            if (calibrationMessageReceiver != null && this.mRequestCalibrationMessageReceiver == null) {
                this.subscribeToEvent(209)
            } else if (calibrationMessageReceiver == null && this.mRequestCalibrationMessageReceiver != null) {
                this.unsubscribeFromEvent(209)
            }
        }

        this.mRequestCalibrationMessageReceiver = calibrationMessageReceiver
    }

    fun subscribeAutoZeroStatusEvent(autoZeroStatusReceiver: IAutoZeroStatusReceiver?) {
        if (this.mRequestAutoZeroStatusReceiver == null) {
            if (autoZeroStatusReceiver != null && this.mAutoZeroStatusReceiver == null) {
                this.subscribeToEvent(210)
            } else if (autoZeroStatusReceiver == null && this.mAutoZeroStatusReceiver != null) {
                this.unsubscribeFromEvent(210)
            }
        }

        this.mAutoZeroStatusReceiver = autoZeroStatusReceiver
    }

    private fun subscribeRequestAutoZeroStatusEvent(autoZeroStatusReceiver: IAutoZeroStatusReceiver?) {
        if (this.mAutoZeroStatusReceiver == null) {
            if (autoZeroStatusReceiver != null && this.mRequestAutoZeroStatusReceiver == null) {
                this.subscribeToEvent(210)
            } else if (autoZeroStatusReceiver == null && this.mRequestAutoZeroStatusReceiver != null) {
                this.unsubscribeFromEvent(210)
            }
        }

        this.mRequestAutoZeroStatusReceiver = autoZeroStatusReceiver
    }

    fun subscribeCalculatedPowerEvent(calculatedPowerReceiver: ICalculatedPowerReceiver?) {
        this.mCalculatedPowerReceiver = calculatedPowerReceiver
        if (calculatedPowerReceiver != null) {
            this.subscribeToEvent(211)
        } else {
            this.unsubscribeFromEvent(211)
        }
    }

    fun subscribeCalculatedTorqueEvent(calculatedTorqueReceiver: ICalculatedTorqueReceiver?) {
        this.mCalculatedTorqueReceiver = calculatedTorqueReceiver
        if (calculatedTorqueReceiver != null) {
            this.subscribeToEvent(212)
        } else {
            this.unsubscribeFromEvent(212)
        }
    }

    fun subscribeCalculatedCrankCadenceEvent(calculatedCrankCadenceReceiver: ICalculatedCrankCadenceReceiver?) {
        this.mCalculatedCrankCadenceReceiver = calculatedCrankCadenceReceiver
        if (calculatedCrankCadenceReceiver != null) {
            this.subscribeToEvent(213)
        } else {
            this.unsubscribeFromEvent(213)
        }
    }

    fun subscribeCalculatedWheelSpeedEvent(calculatedWheelSpeedReceiver: CalculatedWheelSpeedReceiver?) {
        this.mCalculatedWheelSpeedReceiver = calculatedWheelSpeedReceiver
        if (calculatedWheelSpeedReceiver != null) {
            this.subscribeToEvent(214)
        } else {
            this.unsubscribeFromEvent(214)
        }
    }

    fun subscribeCalculatedWheelDistanceEvent(calculatedWheelDistanceReceiver: CalculatedWheelDistanceReceiver?) {
        this.mCalculatedWheelDistanceReceiver = calculatedWheelDistanceReceiver
        if (calculatedWheelDistanceReceiver != null) {
            this.subscribeToEvent(215)
        } else {
            this.unsubscribeFromEvent(215)
        }
    }

    private fun subscribeRequestMeasurementOutputDataEvent(measurementOutputDataReceiver: IMeasurementOutputDataReceiver?) {
        if (this.mMeasurementOutputDataReceiver == null) {
            if (measurementOutputDataReceiver != null && this.mRequestMeasurementOutputDataReceiver == null) {
                this.subscribeToEvent(216)
            } else if (measurementOutputDataReceiver == null && this.mRequestMeasurementOutputDataReceiver != null) {
                this.unsubscribeFromEvent(216)
            }
        }

        this.mRequestMeasurementOutputDataReceiver = measurementOutputDataReceiver
    }

    fun subscribeCrankParametersEvent(crankParametersReceiver: ICrankParametersReceiver?) {
        if (this.mRequestCrankParametersReceiver == null) {
            if (crankParametersReceiver != null && this.mCrankParametersReceiver == null) {
                this.subscribeToEvent(217)
            } else if (crankParametersReceiver == null && this.mCrankParametersReceiver != null) {
                this.unsubscribeFromEvent(217)
            }
        }

        this.mCrankParametersReceiver = crankParametersReceiver
    }

    private fun subscribeRequestCrankParametersEvent(crankParametersReceiver: ICrankParametersReceiver?) {
        if (this.mCrankParametersReceiver == null) {
            if (crankParametersReceiver != null && this.mRequestCrankParametersReceiver == null) {
                this.subscribeToEvent(217)
            } else if (crankParametersReceiver == null && this.mRequestCrankParametersReceiver != null) {
                this.unsubscribeFromEvent(217)
            }
        }

        this.mRequestCrankParametersReceiver = crankParametersReceiver
    }

    @JvmOverloads
    fun requestManualCalibration(
        requestFinishedReceiver: IRequestFinishedReceiver?,
        calibrationMessageReceiver: ICalibrationMessageReceiver? = this.mRequestCalibrationMessageReceiver,
        measurementOutputDataReceiver: IMeasurementOutputDataReceiver? = this.mRequestMeasurementOutputDataReceiver
    ): Boolean {
        val cmdName = "requestManualCalibration"
        val whatCmd = 20001
        this.subscribeRequestCalibrationMessageEvent(calibrationMessageReceiver)
        this.subscribeRequestMeasurementOutputDataEvent(measurementOutputDataReceiver)
        return this.sendRequestCommand(cmdName, whatCmd, requestFinishedReceiver)
    }

    @JvmOverloads
    fun requestSetAutoZero(
        autoZeroEnable: Boolean,
        requestFinishedReceiver: IRequestFinishedReceiver?,
        calibrationMessageReceiver: ICalibrationMessageReceiver? = this.mRequestCalibrationMessageReceiver,
        autoZeroStatusReceiver: IAutoZeroStatusReceiver? = this.mRequestAutoZeroStatusReceiver
    ): Boolean {
        val cmdName = "requestSetAutoZero"
        val whatCmd = 20002
        val params = Bundle()
        params.putBoolean("bool_autoZeroEnable", autoZeroEnable)
        this.subscribeRequestCalibrationMessageEvent(calibrationMessageReceiver)
        this.subscribeRequestAutoZeroStatusEvent(autoZeroStatusReceiver)
        return this.sendRequestCommand(cmdName, whatCmd, params, requestFinishedReceiver)
    }

    @JvmOverloads
    fun requestSetCtfSlope(
        slope: BigDecimal?,
        requestFinishedReceiver: IRequestFinishedReceiver?,
        calibrationMessageReceiver: ICalibrationMessageReceiver? = this.mRequestCalibrationMessageReceiver
    ): Boolean {
        val cmdName = "requestSetCtfSlope"
        val whatCmd = 20003
        val params = Bundle()
        params.putSerializable("decimal_slope", slope)
        this.subscribeRequestCalibrationMessageEvent(calibrationMessageReceiver)
        return this.sendRequestCommand(cmdName, whatCmd, params, requestFinishedReceiver)
    }

    @JvmOverloads
    fun requestCustomCalibrationParameters(
        manufacturerSpecificParameters: ByteArray?,
        requestFinishedReceiver: IRequestFinishedReceiver?,
        calibrationMessageReceiver: ICalibrationMessageReceiver? = this.mRequestCalibrationMessageReceiver
    ): Boolean {
        val cmdName = "requestCustomCalibrationParameters"
        val whatCmd = 20004
        val params = Bundle()
        params.putByteArray(
            "arrayByte_manufacturerSpecificParameters", manufacturerSpecificParameters
        )
        this.subscribeRequestCalibrationMessageEvent(calibrationMessageReceiver)
        return this.sendRequestCommand(cmdName, whatCmd, params, requestFinishedReceiver)
    }

    @JvmOverloads
    fun requestSetCustomCalibrationParameters(
        manufacturerSpecificParameters: ByteArray?,
        requestFinishedReceiver: IRequestFinishedReceiver?,
        calibrationMessageReceiver: ICalibrationMessageReceiver? = this.mRequestCalibrationMessageReceiver
    ): Boolean {
        val cmdName = "requestSetCustomCalibrationParameters"
        val whatCmd = 20005
        val params = Bundle()
        params.putByteArray(
            "arrayByte_manufacturerSpecificParameters", manufacturerSpecificParameters
        )
        this.subscribeRequestCalibrationMessageEvent(calibrationMessageReceiver)
        return this.sendRequestCommand(cmdName, whatCmd, params, requestFinishedReceiver)
    }

    @JvmOverloads
    fun requestCrankParameters(
        requestFinishedReceiver: IRequestFinishedReceiver?,
        crankParametersReceiver: ICrankParametersReceiver? = this.mRequestCrankParametersReceiver
    ): Boolean {
        val cmdName = "requestCrankParameters"
        val whatCmd = 20006
        this.subscribeRequestCrankParametersEvent(crankParametersReceiver)
        return this.sendRequestCommand(cmdName, whatCmd, requestFinishedReceiver)
    }

    fun requestSetCrankParameters(
        crankLengthSetting: CrankLengthSetting,
        fullCrankLength: BigDecimal?,
        requestFinishedReceiver: IRequestFinishedReceiver?
    ): Boolean {
        val cmdName = "requestSetCrankParameters"
        val whatCmd = 20007
        val params = Bundle()
        params.putInt("int_crankLengthSetting", crankLengthSetting.intValue)
        params.putSerializable("decimal_fullCrankLength", fullCrankLength)
        return this.sendRequestCommand(cmdName, whatCmd, params, requestFinishedReceiver)
    }

    fun requestCommandBurst(
        requestedCommandId: Int,
        commandData: ByteArray?,
        requestFinishedReceiver: IRequestFinishedReceiver?
    ): Boolean {
        val cmdName = "requestCommandBurst"
        val whatCmd = 104
        val params = Bundle()
        params.putInt("int_requestedCommandId", requestedCommandId)
        params.putByteArray("arrayByte_commandData", commandData)
        return this.sendRequestCommand(cmdName, whatCmd, params, requestFinishedReceiver, 20206)
    }

    fun setOnLogDataListener(onLogDataListener: OnLogDataListener) {
        this.onLogDataListener = onLogDataListener
    }

    interface ICrankParametersReceiver {
        fun onNewCrankParameters(var1: Long, var3: EnumSet<EventFlag?>?, var4: CrankParameters?)
    }

    interface IMeasurementOutputDataReceiver {
        fun onNewMeasurementOutputData(
            var1: Long,
            var3: EnumSet<EventFlag?>?,
            var4: Int,
            var5: Int,
            var6: BigDecimal?,
            var7: BigDecimal?
        )
    }

    interface IAutoZeroStatusReceiver {
        fun onNewAutoZeroStatus(var1: Long, var3: EnumSet<EventFlag?>?, var4: AutoZeroStatus?)
    }

    interface ICalibrationMessageReceiver {
        fun onNewCalibrationMessage(
            var1: Long, var3: EnumSet<EventFlag?>?, var4: CalibrationMessage?
        )
    }

    interface IRawPowerOnlyDataReceiver {
        fun onNewRawPowerOnlyData(
            var1: Long, var3: EnumSet<EventFlag?>?, var4: Long, var6: Int, var7: Long
        )
    }

    interface IPedalPowerBalanceReceiver {
        fun onNewPedalPowerBalance(
            estTimestamp: Long,
            eventFlags: EnumSet<EventFlag?>?,
            rightPedalIndicator: Boolean,
            pedalPowerPercentage: Int
        )
    }

    interface IInstantaneousCadenceReceiver {
        fun onNewInstantaneousCadence(
            var1: Long, var3: EnumSet<EventFlag?>?, var4: DataSource?, var5: Int
        )
    }

    enum class DataSource(var intValue: Int) {
        POWER_ONLY_DATA(16), WHEEL_TORQUE_DATA(17), CRANK_TORQUE_DATA(18), CTF_DATA(32), COAST_OR_STOP_DETECTED(
            65536
        ),
        INITIAL_VALUE_POWER_ONLY_DATA(65296), INITIAL_VALUE_WHEEL_TORQUE_DATA(65297), INITIAL_VALUE_CRANK_TORQUE_DATA(
            65298
        ),
        INITIAL_VALUE_CTF_DATA(65312), INVALID(-1), INVALID_CTF_CAL_REQ(-2), UNRECOGNIZED(-3);

        companion object {
            fun getValueFromInt(intValue: Int): DataSource {
                val var1 = entries.toTypedArray()

                for (source in var1) {
                    if (source.intValue == intValue) {
                        return source
                    }
                }

                val unrecognized = UNRECOGNIZED
                unrecognized.intValue = intValue
                return unrecognized
            }
        }
    }

    interface IRawWheelTorqueDataReceiver {
        fun onNewRawWheelTorqueData(
            var1: Long,
            var3: EnumSet<EventFlag?>?,
            var4: Long,
            var6: Long,
            var8: BigDecimal?,
            var9: BigDecimal?
        )
    }

    interface IRawCrankTorqueDataReceiver {
        fun onNewRawCrankTorqueData(
            var1: Long,
            var3: EnumSet<EventFlag?>?,
            var4: Long,
            var6: Long,
            var8: BigDecimal?,
            var9: BigDecimal?
        )
    }

    interface ITorqueEffectivenessReceiver {
        fun onNewTorqueEffectiveness(
            estTimestamp: Long,
            eventFlags: EnumSet<EventFlag?>?,
            powerOnlyUpdateEventCount: Long,
            leftTorqueEffectiveness: BigDecimal?,
            rightTorqueEffectiveness: BigDecimal?
        )
    }

    interface IPedalSmoothnessReceiver {
        fun onNewPedalSmoothness(
            estTimestamp: Long,
            eventFlags: EnumSet<EventFlag?>?,
            powerOnlyUpdateEventCount: Long,
            separatePedalSmoothnessSupport: Boolean,
            leftOrCombinedPedalSmoothness: BigDecimal?,
            rightPedalSmoothness: BigDecimal?
        )
    }

    interface IRawCtfDataReceiver {
        fun onNewRawCtfData(
            var1: Long,
            var3: EnumSet<EventFlag?>?,
            var4: Long,
            var6: BigDecimal?,
            var7: BigDecimal?,
            var8: Long
        )
    }

    class CalibrationMessage(`in`: Parcel) : Parcelable {
        private val calibrationId: CalibrationId
        private val calibrationData: Int?
        private val ctfOffset: Int?
        private val manufacturerSpecificData: ByteArray?

        init {
            val incomingVersion = `in`.readInt()
            if (incomingVersion != 1) {
                LogAnt.i(
                    TAG,
                    "Decoding version $incomingVersion CalibrationMessage parcel with version 1 parser."
                )
            }

            this.calibrationId = CalibrationId.getValueFromInt(`in`.readInt())
            this.calibrationData = `in`.readValue(Int::class.java.classLoader) as Int?
            this.ctfOffset = `in`.readValue(Int::class.java.classLoader) as Int?
            this.manufacturerSpecificData =
                `in`.readValue(ByteArray::class.java.classLoader) as ByteArray?
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            val ipcVersionNumber = 1
            dest.writeInt(ipcVersionNumber)
            dest.writeInt(calibrationId.intValue)
            dest.writeValue(this.calibrationData)
            dest.writeValue(this.ctfOffset)
            dest.writeValue(this.manufacturerSpecificData)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<CalibrationMessage?> =
                object : Parcelable.Creator<CalibrationMessage?> {
                    override fun createFromParcel(source: Parcel): CalibrationMessage {
                        return CalibrationMessage(source)
                    }

                    override fun newArray(size: Int): Array<CalibrationMessage?> {
                        return arrayOfNulls(size)
                    }
                }
        }
    }

    enum class AutoZeroStatus(var intValue: Int) {
        OFF(0), ON(1), NOT_SUPPORTED(255), INVALID(-1), UNKNOWN(-2), UNRECOGNIZED(-3);

        companion object {
            fun getValueFromInt(intValue: Int): AutoZeroStatus {
                val var1 = entries.toTypedArray()

                for (status in var1) {
                    if (status.intValue == intValue) {
                        return status
                    }
                }

                val unrecognized = UNRECOGNIZED
                unrecognized.intValue = intValue
                return unrecognized
            }
        }
    }

    interface ICalculatedPowerReceiver {
        fun onNewCalculatedPower(
            estTimestamp: Long,
            eventFlags: EnumSet<EventFlag?>?,
            dataSource: DataSource?,
            calculatedPower: BigDecimal?
        )
    }

    interface ICalculatedTorqueReceiver {
        fun onNewCalculatedTorque(
            estTimestamp: Long,
            eventFlags: EnumSet<EventFlag?>?,
            dataSource: DataSource?,
            var5: BigDecimal?
        )
    }

    interface ICalculatedCrankCadenceReceiver {
        fun onNewCalculatedCrankCadence(
            estTimestamp: Long,
            eventFlags: EnumSet<EventFlag?>?,
            dataSource: DataSource?,
            calculatedCrankCadence: BigDecimal?
        )
    }

    abstract class CalculatedWheelSpeedReceiver(private var wheelCircumference: BigDecimal) {
        abstract fun onNewCalculatedWheelSpeed(
            estTimestamp: Long,
            eventFlags: EnumSet<EventFlag?>?,
            dataSource: DataSource?,
            var5: BigDecimal?
        )

        fun onNewRawCalculatedWheelSpeed(
            estTimestamp: Long,
            eventFlags: EnumSet<EventFlag?>,
            dataSource: DataSource,
            rawCalculatedWheelSpeed: BigDecimal
        ) {
            this.onNewCalculatedWheelSpeed(
                estTimestamp, eventFlags, dataSource, rawCalculatedWheelSpeed.multiply(
                    this.wheelCircumference
                )
            )
        }
    }

    abstract class CalculatedWheelDistanceReceiver(private var wheelCircumference: BigDecimal) {
        private var initialDistanceZeroVal: BigDecimal? = null

        abstract fun onNewCalculatedWheelDistance(
            estTimestamp: Long,
            eventFlags: EnumSet<EventFlag?>?,
            dataSource: DataSource?,
            var5: BigDecimal?
        )

        fun onNewRawCalculatedWheelDistance(
            estTimestamp: Long,
            eventFlags: EnumSet<EventFlag?>,
            dataSource: DataSource,
            rawCalculatedWheelDistance: BigDecimal
        ) {
            if (this.initialDistanceZeroVal == null) {
                this.initialDistanceZeroVal =
                    rawCalculatedWheelDistance.multiply(this.wheelCircumference)
            }

            this.onNewCalculatedWheelDistance(
                estTimestamp, eventFlags, dataSource, rawCalculatedWheelDistance.multiply(
                    this.wheelCircumference
                ).subtract(this.initialDistanceZeroVal)
            )
        }
    }

    class CrankParameters(`in`: Parcel) : Parcelable {
        private val fullCrankLength: BigDecimal
        private val crankLengthStatus: CrankLengthStatus
        private val sensorSoftwareMismatchStatus: SensorSoftwareMismatchStatus
        private val sensorAvailabilityStatus: SensorAvailabilityStatus
        private val customCalibrationStatus: CustomCalibrationStatus
        private val isAutoCrankLengthSupported: Boolean

        init {
            val incomingVersion = `in`.readInt()
            if (incomingVersion != 1) {
                LogAnt.i(
                    TAG,
                    "Decoding version $incomingVersion CrankParameters parcel with version 1 parser."
                )
            }

            this.fullCrankLength = BigDecimal(`in`.readString())
            this.crankLengthStatus = CrankLengthStatus.getValueFromInt(`in`.readInt())
            this.sensorSoftwareMismatchStatus =
                SensorSoftwareMismatchStatus.getValueFromInt(`in`.readInt())
            this.sensorAvailabilityStatus = SensorAvailabilityStatus.getValueFromInt(`in`.readInt())
            this.customCalibrationStatus = CustomCalibrationStatus.getValueFromInt(`in`.readInt())
            this.isAutoCrankLengthSupported = `in`.readByte().toInt() != 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            val ipcVersionNumber = 1
            dest.writeInt(ipcVersionNumber)
            dest.writeString(fullCrankLength.toString())
            dest.writeInt(crankLengthStatus.intValue)
            dest.writeInt(sensorSoftwareMismatchStatus.intValue)
            dest.writeInt(sensorAvailabilityStatus.intValue)
            dest.writeInt(customCalibrationStatus.intValue)
            dest.writeByte((if (this.isAutoCrankLengthSupported) 1 else 0).toByte())
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<CrankParameters?> =
                object : Parcelable.Creator<CrankParameters?> {
                    override fun createFromParcel(source: Parcel): CrankParameters {
                        return CrankParameters(source)
                    }

                    override fun newArray(size: Int): Array<CrankParameters?> {
                        return arrayOfNulls(size)
                    }
                }
        }
    }

    enum class CrankLengthSetting(val intValue: Int) {
        AUTO_CRANK_LENGTH(254), MANUAL_CRANK_LENGTH(65280), INVALID(255)
    }

    enum class CustomCalibrationStatus {
        UNDEFINED, CUSTOM_CALIBRATION_NOT_REQUIRED, CUSTOM_CALIBRATION_REQUIRED;

        val intValue: Int
            get() = this.ordinal

        companion object {
            fun getValueFromInt(intValue: Int): CustomCalibrationStatus {
                return when (intValue) {
                    0, 3 -> UNDEFINED
                    1 -> CUSTOM_CALIBRATION_NOT_REQUIRED
                    2 -> CUSTOM_CALIBRATION_REQUIRED
                    else -> throw IllegalArgumentException("Undefined Custom Calibration Status")
                }
            }
        }
    }

    enum class SensorAvailabilityStatus {
        UNDEFINED, LEFT_SENSOR_PRESENT, RIGHT_SENSOR_PRESENT, LEFT_AND_RIGHT_SENSOR_PRESENT;

        val intValue: Int
            get() = this.ordinal

        companion object {
            fun getValueFromInt(intValue: Int): SensorAvailabilityStatus {
                return when (intValue) {
                    0 -> UNDEFINED
                    1 -> LEFT_SENSOR_PRESENT
                    2 -> RIGHT_SENSOR_PRESENT
                    3 -> LEFT_AND_RIGHT_SENSOR_PRESENT
                    else -> throw IllegalArgumentException("Undefined Sensor Availability Status")
                }
            }
        }
    }

    enum class SensorSoftwareMismatchStatus {
        UNDEFINED, MISMATCH_RIGHT_SENSOR_OLDER, MISMATCH_LEFT_SENSOR_OLDER, SW_MATCHES;

        val intValue: Int
            get() = this.ordinal

        companion object {
            fun getValueFromInt(intValue: Int): SensorSoftwareMismatchStatus {
                return when (intValue) {
                    0 -> UNDEFINED
                    1 -> MISMATCH_RIGHT_SENSOR_OLDER
                    2 -> MISMATCH_LEFT_SENSOR_OLDER
                    3 -> SW_MATCHES
                    else -> throw IllegalArgumentException("Undefined Sensor Software Mismatch Status")
                }
            }
        }
    }

    enum class CrankLengthStatus {
        INVALID_CRANK_LENGTH, DEFAULT_USED, SET_MANUALLY, SET_AUTOMATICALLY;

        val intValue: Int
            get() = this.ordinal

        companion object {
            fun getValueFromInt(intValue: Int): CrankLengthStatus {
                return when (intValue) {
                    0 -> INVALID_CRANK_LENGTH
                    1 -> DEFAULT_USED
                    2 -> SET_MANUALLY
                    3 -> SET_AUTOMATICALLY
                    else -> throw IllegalArgumentException("Undefined Crank Length Status")
                }
            }
        }
    }

    enum class CalibrationId(var intValue: Int) {
        GENERAL_CALIBRATION_SUCCESS(172), GENERAL_CALIBRATION_FAIL(175), CTF_MESSAGE(16), CTF_ZERO_OFFSET(
            4097
        ),
        CTF_SLOPE_ACK(1092610), CTF_SERIAL_NUMBER_ACK(1092611), CAPABILITIES(18), CUSTOM_CALIBRATION_RESPONSE(
            187
        ),
        CUSTOM_CALIBRATION_UPDATE_SUCCESS(189), INVALID(-1), UNRECOGNIZED(-2);

        companion object {
            fun getValueFromInt(intValue: Int): CalibrationId {
                val var1 = entries.toTypedArray()

                for (ident in var1) {
                    if (ident.intValue == intValue) {
                        return ident
                    }
                }

                val unrecognized = UNRECOGNIZED
                unrecognized.intValue = intValue
                return unrecognized
            }
        }
    }

    companion object {
        private val TAG: String = MyAntPlusBikePowerPcc::class.java.simpleName
        private fun requestAccess(
            userActivity: Activity?,
            bindToContext: Context,
            skipPreferredSearch: Boolean,
            searchProximityThreshold: Int,
            resultReceiver: IPluginAccessResultReceiver<MyAntPlusBikePowerPcc>,
            stateReceiver: IDeviceStateChangeReceiver
        ): PccReleaseHandle<MyAntPlusBikePowerPcc> {
            val potentialRetObj = MyAntPlusBikePowerPcc()
            return requestAccess_Helper_SearchActivity(
                userActivity,
                bindToContext,
                skipPreferredSearch,
                searchProximityThreshold,
                potentialRetObj,
                resultReceiver,
                stateReceiver
            )
        }

        fun requestAccess(
            userActivity: Activity?,
            bindToContext: Context,
            resultReceiver: IPluginAccessResultReceiver<MyAntPlusBikePowerPcc>,
            stateReceiver: IDeviceStateChangeReceiver
        ): PccReleaseHandle<MyAntPlusBikePowerPcc> {
            return requestAccess(
                userActivity, bindToContext, false, -1, resultReceiver, stateReceiver
            )
        }

        fun requestAccess(
            bindToContext: Context,
            antDeviceNumber: Int,
            searchProximityThreshold: Int,
            resultReceiver: IPluginAccessResultReceiver<MyAntPlusBikePowerPcc>,
            stateReceiver: IDeviceStateChangeReceiver
        ): PccReleaseHandle<MyAntPlusBikePowerPcc> {
            val potentialRetObj = MyAntPlusBikePowerPcc()
            return requestAccess_Helper_AsyncSearchByDevNumber(
                bindToContext,
                antDeviceNumber,
                searchProximityThreshold,
                potentialRetObj,
                resultReceiver,
                stateReceiver
            )
        }

        fun requestAsyncScanController(
            bindToContext: Context?,
            searchProximityThreshold: Int,
            scanResultReceiver: IAsyncScanResultReceiver?
        ): AsyncScanController<MyAntPlusBikePowerPcc> {
            val potentialRetObj = MyAntPlusBikePowerPcc()
            return requestAccess_Helper_AsyncScanController(
                bindToContext, searchProximityThreshold, potentialRetObj, scanResultReceiver
            )
        }
    }
}