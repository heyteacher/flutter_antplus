package me.heyteacher.flutter_antplus.heartrate;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;
import com.dsi.ant.plugins.antplus.pccbase.AsyncScanController;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;

import io.flutter.Log;
import io.flutter.plugin.common.BinaryMessenger;

public class HeartrateHostApiImpl implements HeartratePigeon.HeartrateHostApi {

    private static final String TAG = HeartrateHostApiImpl.class.getSimpleName();
    private final Context context;
    private final HeartratePigeon.HeartrateFlutterApi flutterApi;
    private final HeartratePigeon.VoidResult flutterApiResult = new HeartratePigeon.VoidResult() {
        @Override
        public void success() {
        }

        @Override
        public void error(@NonNull Throwable error) {
            Log.e(TAG, "<onSearchResult>: error", error);
        }
    };
    private PccReleaseHandle<AntPlusHeartRatePcc> releaseHandle = null;
    private AntPlusHeartRatePcc hrPcc;
    private final AntPluginPcc.IDeviceStateChangeReceiver stateChangeReceiver = new AntPluginPcc.IDeviceStateChangeReceiver() {

        @Override
        public void onDeviceStateChange(DeviceState deviceState) {
            Log.d(TAG, "<onDeviceStateChange>: " + hrPcc.getAntDeviceNumber() + " deviceState " + deviceState);
            new Handler(Looper.getMainLooper()).post(() -> flutterApi.onDeviceStateChange(switch (deviceState) {
                case DEAD -> HeartratePigeon.AntplusDeviceState.DEAD;
                case CLOSED -> HeartratePigeon.AntplusDeviceState.CLOSED;
                case PROCESSING_REQUEST -> HeartratePigeon.AntplusDeviceState.PROCESSING_REQUEST;
                case SEARCHING -> HeartratePigeon.AntplusDeviceState.SEARCHING;
                case TRACKING -> HeartratePigeon.AntplusDeviceState.TRACKING;
                case UNRECOGNIZED -> HeartratePigeon.AntplusDeviceState.UNRECOGNIZED;
            }, flutterApiResult));
        }
    };
    private final AntPluginPcc.IPluginAccessResultReceiver<AntPlusHeartRatePcc> accessResultReceiver = new AntPluginPcc.IPluginAccessResultReceiver<>() {
        //Handle the result, connecting to events on success or reporting failure to user.
        @Override
        public void onResultReceived(AntPlusHeartRatePcc result, RequestAccessResult resultCode, DeviceState initialDeviceState) {
            Log.d(TAG, "<onResultReceived>: resultCode " + resultCode);
            switch (resultCode) {
                case SUCCESS:
                    hrPcc = result;
                    Log.d(TAG, "(onResultReceived): resultCode " + resultCode + ". deviceNumber " + result.getAntDeviceNumber() + " initialDeviceState " + initialDeviceState);
                    new Handler(Looper.getMainLooper()).post(() -> flutterApi.onDeviceStateChange(switch (initialDeviceState) {
                        case DEAD -> HeartratePigeon.AntplusDeviceState.DEAD;
                        case CLOSED -> HeartratePigeon.AntplusDeviceState.CLOSED;
                        case PROCESSING_REQUEST -> HeartratePigeon.AntplusDeviceState.PROCESSING_REQUEST;
                        case SEARCHING -> HeartratePigeon.AntplusDeviceState.SEARCHING;
                        case TRACKING -> HeartratePigeon.AntplusDeviceState.TRACKING;
                        case UNRECOGNIZED -> HeartratePigeon.AntplusDeviceState.UNRECOGNIZED;
                    }, flutterApiResult));
                    hrPcc.subscribeHeartRateDataEvent((estTimestamp, eventFlags, computedHeartRate, heartBeatCount, heartBeatEventTime, dataState) -> new Handler(Looper.getMainLooper()).post(() -> flutterApi.onHeartRateData((long) computedHeartRate, flutterApiResult)));
                    break;
                case DEPENDENCY_NOT_INSTALLED:
                    Log.e(TAG, "(onResultReceived): resultCode " + resultCode + ". The required service\"" + AntPlusHeartRatePcc.getMissingDependencyName() + "\"was not found. You need to install the ANT+ Plugins service or you may need to update your existing version if you already have it. Do you want to launch the Play Store to get it?");
                    break;
                case UNRECOGNIZED:
                case CHANNEL_NOT_AVAILABLE:
                case ADAPTER_NOT_DETECTED:
                case BAD_PARAMS:
                case OTHER_FAILURE:
                case USER_CANCELLED:
                default:
            }
        }
    };
    private AsyncScanController<AntPlusHeartRatePcc> hrScanCtrl;

    public HeartrateHostApiImpl(Context context, BinaryMessenger binaryMessenger) {
        this.context = context;
        this.flutterApi = new HeartratePigeon.HeartrateFlutterApi(binaryMessenger);
        HeartratePigeon.HeartrateHostApi.setUp(binaryMessenger, this);
    }

    @Override
    public void startScan() {
        Log.d(TAG, "<startScan>:");
        hrScanCtrl = AntPlusHeartRatePcc.requestAsyncScanController(context, 0, new AsyncScanController.IAsyncScanResultReceiver() {
            @Override
            public void onSearchStopped(RequestAccessResult reasonStopped) {
                Log.d(TAG, "<onSearchStopped>:");
                //The triggers calling this function use the same codes and require the same actions as those received by the standard access result receiver
                accessResultReceiver.onResultReceived(null, reasonStopped, DeviceState.DEAD);
            }

            @Override
            public void onSearchResult(final AsyncScanController.AsyncScanResultDeviceInfo deviceFound) {
                Log.d(TAG, "<onSearchResult>: number " + deviceFound.getAntDeviceNumber() + " name " + deviceFound.getDeviceDisplayName() + " already connected " + deviceFound.isAlreadyConnected());
                HeartratePigeon.AntplusDevice.Builder builder = new HeartratePigeon.AntplusDevice.Builder().setName(deviceFound.getDeviceDisplayName()).setNumber((long) deviceFound.getAntDeviceNumber());
                new Handler(Looper.getMainLooper()).post(() -> flutterApi.onScanResult(builder.build(), flutterApiResult));
            }
        });
    }

    @Override
    public void stopScan() {
        if (hrScanCtrl != null) {
            hrScanCtrl.closeScanController();
        }
        hrScanCtrl = null;
    }

    @Override
    public void connect(Long deviceNumber) {
        Log.d(TAG, "<connect>: deviceNumber " + deviceNumber);
        stopScan();
        releaseHandle = AntPlusHeartRatePcc.requestAccess(context, deviceNumber.intValue(), -1, accessResultReceiver, stateChangeReceiver);
    }

    @Override
    public void disconnect() {
        Log.d(TAG, "<disconnect>:");
        if (releaseHandle != null) {
            releaseHandle.close();
        }
        if (hrPcc != null) {
            Log.d(TAG, "(disconnect): deviceNumber " + hrPcc.getAntDeviceNumber());
            hrPcc.subscribeHeartRateDataEvent(null);
            hrPcc.releaseAccess();
        }
        releaseHandle = null;
        hrPcc = null;
    }
}
