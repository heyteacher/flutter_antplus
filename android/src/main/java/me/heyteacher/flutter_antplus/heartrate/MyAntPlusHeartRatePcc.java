package me.heyteacher.flutter_antplus.heartrate;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusLegacyCommonPcc;
import com.dsi.ant.plugins.antplus.pccbase.AsyncScanController;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.EnumSet;

public class MyAntPlusHeartRatePcc extends AntPlusLegacyCommonPcc {
    private static final String TAG = MyAntPlusHeartRatePcc.class.getSimpleName();
    IHeartRateDataReceiver mHeartRateDataReceiver;
    IPage4AddtDataReceiver mPage4AddtDataReceiver;
    ICalculatedRrIntervalReceiver mCalculatedRrIntervalReceiver;
    MyLegacyHeartRateCompat mCompat;

    protected int getRequiredServiceVersionForBind() {
        return 0;
    }

    public static PccReleaseHandle<MyAntPlusHeartRatePcc> requestAccess(Activity userActivity, Context bindToContext, boolean skipPreferredSearch, int searchProximityThreshold, AntPluginPcc.IPluginAccessResultReceiver<MyAntPlusHeartRatePcc> resultReceiver, AntPluginPcc.IDeviceStateChangeReceiver stateReceiver) {
        MyAntPlusHeartRatePcc potentialRetObj = new MyAntPlusHeartRatePcc();
        return requestAccess_Helper_SearchActivity(userActivity, bindToContext, skipPreferredSearch, searchProximityThreshold, potentialRetObj, resultReceiver, stateReceiver);
    }

    public static PccReleaseHandle<MyAntPlusHeartRatePcc> requestAccess(Activity userActivity, Context bindToContext, AntPluginPcc.IPluginAccessResultReceiver<MyAntPlusHeartRatePcc> resultReceiver, AntPluginPcc.IDeviceStateChangeReceiver stateReceiver) {
        return requestAccess(userActivity, bindToContext, false, -1, resultReceiver, stateReceiver);
    }

    public static PccReleaseHandle<MyAntPlusHeartRatePcc> requestAccess(Context bindToContext, int antDeviceNumber, int searchProximityThreshold, AntPluginPcc.IPluginAccessResultReceiver<MyAntPlusHeartRatePcc> resultReceiver, AntPluginPcc.IDeviceStateChangeReceiver stateReceiver) {
        MyAntPlusHeartRatePcc potentialRetObj = new MyAntPlusHeartRatePcc();
        return requestAccess_Helper_AsyncSearchByDevNumber(bindToContext, antDeviceNumber, searchProximityThreshold, potentialRetObj, resultReceiver, stateReceiver);
    }

    public static AsyncScanController<MyAntPlusHeartRatePcc> requestAsyncScanController(Context bindToContext, int searchProximityThreshold, AsyncScanController.IAsyncScanResultReceiver scanResultReceiver) {
        MyAntPlusHeartRatePcc potentialRetObj = new MyAntPlusHeartRatePcc();
        return requestAccess_Helper_AsyncScanController(bindToContext, searchProximityThreshold, potentialRetObj, scanResultReceiver);
    }

    public MyAntPlusHeartRatePcc() {
    }

    protected Intent getServiceBindIntent() {
        Intent it = new Intent();
        it.setComponent(new ComponentName("com.dsi.ant.plugins.antplus", "com.dsi.ant.plugins.antplus.heartrate.HeartRateService"));
        return it;
    }

    protected String getPluginPrintableName() {
        return "ANT+ Plugin: Heart Rate";
    }

    protected void handlePluginEvent(Message eventMsg) {
        Bundle b;
        long estTimestamp;
        EnumSet<EventFlag> eventFlags;
        BigDecimal calculatedRrInterval;
        int computedHeartRate;
        switch (eventMsg.arg1) {
            case 201:
                if (this.mHeartRateDataReceiver != null) {
                    b = eventMsg.getData();
                    estTimestamp = b.getLong("long_EstTimestamp");
                    eventFlags = EventFlag.getEventFlagsFromLong(b.getLong("long_EventFlags"));
                    computedHeartRate = b.getInt("int_computedHeartRate");
                    long heartBeatCounter = b.getLong("long_heartBeatCounter");
                    BigDecimal heartBeatEventTime = (BigDecimal)b.getSerializable("decimal_timestampOfLastEvent");
                    DataState dataState;
                    if (b.containsKey("int_dataState")) {
                        dataState = DataState.getValueFromInt(b.getInt("int_dataState"));
                    } else {
                        dataState = DataState.LIVE_DATA;
                    }

                    this.mHeartRateDataReceiver.onNewHeartRateData(estTimestamp, eventFlags, computedHeartRate, heartBeatCounter, heartBeatEventTime, dataState);
                }
                break;
            case 202:
                if (this.mCompat != null) {
                    b = eventMsg.getData();
                    estTimestamp = b.getLong("long_EstTimestamp");
                    eventFlags = EventFlag.getEventFlagsFromLong(b.getLong("long_EventFlags"));
                    calculatedRrInterval = (BigDecimal)b.getSerializable("decimal_timestampOfLastEvent");
                    this.mCompat.onNewHeartRateDataTimestamp(estTimestamp, eventFlags, calculatedRrInterval);
                }
                break;
            case 203:
                if (this.mPage4AddtDataReceiver != null) {
                    b = eventMsg.getData();
                    estTimestamp = b.getLong("long_EstTimestamp");
                    eventFlags = EventFlag.getEventFlagsFromLong(b.getLong("long_EventFlags"));
                    computedHeartRate = b.getInt("int_manufacturerSpecificByte");
                    BigDecimal previousHeartBeatEventTime = (BigDecimal)b.getSerializable("decimal_timestampOfPreviousToLastHeartBeatEvent");
                    this.mPage4AddtDataReceiver.onNewPage4AddtData(estTimestamp, eventFlags, computedHeartRate, previousHeartBeatEventTime);
                }
                break;
            case 204:
            case 205:
            case 206:
            case 207:
                if (this.mCalculatedRrIntervalReceiver != null) {
                    b = eventMsg.getData();
                    estTimestamp = b.getLong("long_EstTimestamp");
                    eventFlags = EventFlag.getEventFlagsFromLong(b.getLong("long_EventFlags"));
                    calculatedRrInterval = (BigDecimal)b.getSerializable("decimal_calculatedRrInterval");
                    RrFlag rrFlag = RrFlag.getValueFromInt(b.getInt("int_rrFlag"));
                    this.mCalculatedRrIntervalReceiver.onNewCalculatedRrInterval(estTimestamp, eventFlags, calculatedRrInterval, rrFlag);
                }
            case 100:
            case 101:
            case 102:
            case 103:
            case 104:
            case 105:
            case 106:
            case 107:
            case 108:
            case 109:
                super.handlePluginEvent(eventMsg);
                break;
            default:
                Log.d(TAG, "arg1 " + eventMsg.arg1 + " keys " + Arrays.toString(eventMsg.getData().keySet().toArray()));
        }
    }

    public void subscribeHeartRateDataEvent(IHeartRateDataReceiver HeartRateDataReceiver) {
        if (this.reportedServiceVersion < 20208) {
            if (HeartRateDataReceiver != null) {
                this.mCompat = new MyLegacyHeartRateCompat(HeartRateDataReceiver);
                this.subscribeToEvent(202);
            } else {
                this.mCompat = null;
                this.unsubscribeFromEvent(202);
            }

            HeartRateDataReceiver = this.mCompat;
        }

        this.mHeartRateDataReceiver = HeartRateDataReceiver;
        if (HeartRateDataReceiver != null) {
            this.subscribeToEvent(201);
        } else {
            this.unsubscribeFromEvent(201);
        }

    }

    public void subscribePage4AddtDataEvent(IPage4AddtDataReceiver Page4AddtDataReceiver) {
        this.mPage4AddtDataReceiver = Page4AddtDataReceiver;
        if (Page4AddtDataReceiver != null) {
            this.subscribeToEvent(203);
        } else {
            this.unsubscribeFromEvent(203);
        }

    }

    public void subscribeCalculatedRrIntervalEvent(ICalculatedRrIntervalReceiver CalculatedRrIntervalReceiver) {
        if (this.reportedServiceVersion < 20208) {
            Log.d(TAG, "subscribeCalculatedRrIntervalEvent requires ANT+ Plugins Service >20208, installed: " + this.reportedServiceVersion);
        } else {
            this.mCalculatedRrIntervalReceiver = CalculatedRrIntervalReceiver;
            if (CalculatedRrIntervalReceiver != null) {
                this.subscribeToEvent(207);
            } else {
                this.unsubscribeFromEvent(207);
            }
        }
    }


    public interface IHeartRateDataReceiver {
        void onNewHeartRateData(long var1, EnumSet<EventFlag> var3, int var4, long var5, BigDecimal var7, DataState var8);
    }

    public enum DataState {
        LIVE_DATA(1),
        INITIAL_VALUE(2),
        ZERO_DETECTED(3),
        UNRECOGNIZED(-1);

        private int intValue;

        DataState(int intValue) {
            this.intValue = intValue;
        }

        public int getIntValue() {
            return this.intValue;
        }

        public static DataState getValueFromInt(int intValue) {
            DataState[] var1 = values();

            for (DataState source : var1) {
                if (source.getIntValue() == intValue) {
                    return source;
                }
            }

            DataState unrecognized = UNRECOGNIZED;
            unrecognized.intValue = intValue;
            return unrecognized;
        }
    }

    public interface IPage4AddtDataReceiver {
        void onNewPage4AddtData(long var1, EnumSet<EventFlag> var3, int var4, BigDecimal var5);
    }

    public interface ICalculatedRrIntervalReceiver {
        void onNewCalculatedRrInterval(long var1, EnumSet<EventFlag> var3, BigDecimal var4, RrFlag var5);
    }

    public enum RrFlag {
        DATA_SOURCE_PAGE_4(1),
        DATA_SOURCE_CACHED(2),
        DATA_SOURCE_AVERAGED(3),
        HEART_RATE_ZERO_DETECTED(4),
        UNRECOGNIZED(-1);

        private int intValue;

        RrFlag(int intValue) {
            this.intValue = intValue;
        }

        public int getIntValue() {
            return this.intValue;
        }

        public static RrFlag getValueFromInt(int intValue) {
            RrFlag[] var1 = values();

            for (RrFlag source : var1) {
                if (source.getIntValue() == intValue) {
                    return source;
                }
            }

            RrFlag unrecognized = UNRECOGNIZED;
            unrecognized.intValue = intValue;
            return unrecognized;
        }
    }
}


