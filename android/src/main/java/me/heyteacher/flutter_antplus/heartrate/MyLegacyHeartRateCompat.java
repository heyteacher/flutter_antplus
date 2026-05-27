package me.heyteacher.flutter_antplus.heartrate;
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//


import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import java.math.BigDecimal;
import java.util.EnumSet;

public class MyLegacyHeartRateCompat implements MyAntPlusHeartRatePcc.IHeartRateDataReceiver {
    private final MyAntPlusHeartRatePcc.IHeartRateDataReceiver mHeartRateDataReceiver;
    private long mEstTimestamp = -1L;
    private EnumSet<EventFlag> mEventFlags;
    private int mComputedHeartRate;
    private long mHeartBeatCount = -1L;
    private MyAntPlusHeartRatePcc.DataState mDataState;

    public MyLegacyHeartRateCompat(MyAntPlusHeartRatePcc.IHeartRateDataReceiver HeartRateDataReceiver) {
        this.mHeartRateDataReceiver = HeartRateDataReceiver;
    }

    public void onNewHeartRateDataTimestamp(long estTimestamp, EnumSet<EventFlag> ignoredEventFlags, BigDecimal heartBeatEventTime) {
        if (estTimestamp == this.mEstTimestamp) {
            this.mHeartRateDataReceiver.onNewHeartRateData(this.mEstTimestamp, this.mEventFlags, this.mComputedHeartRate, this.mHeartBeatCount, heartBeatEventTime, this.mDataState);
        }

    }

    public void onNewHeartRateData(long estTimestamp, EnumSet<EventFlag> eventFlags, int computedHeartRate, long heartBeatCount, BigDecimal heartBeatEventTime, MyAntPlusHeartRatePcc.DataState dataState) {
        this.mEstTimestamp = estTimestamp;
        this.mEventFlags = eventFlags;
        this.mComputedHeartRate = computedHeartRate;
        this.mHeartBeatCount = heartBeatCount;
        this.mDataState = dataState;
    }
}

