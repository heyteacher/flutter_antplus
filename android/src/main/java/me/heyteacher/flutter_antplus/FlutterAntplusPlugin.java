package me.heyteacher.flutter_antplus;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;
import com.dsi.ant.plugins.antplus.pccbase.AsyncScanController;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;

import java.util.HashMap;
import java.util.Objects;

import io.flutter.Log;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import me.heyteacher.flutter_antplus.heartrate.MyAntPlusHeartRatePcc;

/**
 * FlutterAntplusPlugin
 */
public class FlutterAntplusPlugin implements FlutterPlugin, MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity

    private static final String TAG = FlutterAntplusPlugin.class.getSimpleName();
    protected PccReleaseHandle<MyAntPlusHeartRatePcc> releaseHandle = null;
    private MethodChannel channel;
    private Context context;
    private MyAntPlusHeartRatePcc hrPcc;
    protected AntPluginPcc.IDeviceStateChangeReceiver base_IDeviceStateChangeReceiver = newDeviceState -> {
        Log.d(TAG, "<onDeviceStateChange>: " + hrPcc.getAntDeviceNumber() + " newDeviceState " + newDeviceState);
        // TODO(heyteacher): implements device state change
    };
    protected AntPluginPcc.IPluginAccessResultReceiver<MyAntPlusHeartRatePcc> base_IPluginAccessResultReceiver = new AntPluginPcc.IPluginAccessResultReceiver<>() {
        //Handle the result, connecting to events on success or reporting failure to user.
        @Override
        public void onResultReceived(MyAntPlusHeartRatePcc result, RequestAccessResult resultCode, DeviceState initialDeviceState) {
            Log.d(TAG, "<onResultReceived>: resultCode " + resultCode);
            switch (resultCode) {
                case SUCCESS:
                    hrPcc = result;
                    Log.d(TAG, "(onResultReceived): resultCode " + resultCode + ". deviceNumber " + result.getAntDeviceNumber() + " initialDeviceState " + initialDeviceState);
                    HashMap<String, Object> response = new HashMap<>();
                    response.put("deviceNumber", hrPcc.getAntDeviceNumber());
                    response.put("displayName", hrPcc.getDeviceName());
                    response.put("alreadyConnected", true);
                    invokeMethodUIThread("onScanResult", response);
                    hrPcc.subscribeHeartRateDataEvent((estTimestamp, eventFlags, computedHeartRate, heartBeatCount, heartBeatEventTime, dataState) -> {
                        Log.d(TAG, "<subscribeHeartRateDataEvent>: computedHeartRate " + computedHeartRate);
                        HashMap<String, Object> hrResponse = new HashMap<>();
                        hrResponse.put("heartRate", computedHeartRate);
                        invokeMethodUIThread("onHeartRateData", hrResponse);
                    });
                    break;
                case DEPENDENCY_NOT_INSTALLED:
                    Log.e(TAG, "(onResultReceived): resultCode " + resultCode + ". The required service\"" + MyAntPlusHeartRatePcc.getMissingDependencyName() + "\"was not found. You need to install the ANT+ Plugins service or you may need to update your existing version if you already have it. Do you want to launch the Play Store to get it?");
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
    private AsyncScanController<MyAntPlusHeartRatePcc> hrScanCtrl;

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        Log.d(TAG, "<onMethodCall>: " + call.method);
        switch (call.method) {
            case "startScan" -> {
                startScan();
                result.success("Scan Started...");
            }
            case "stopScan" -> {
                hrScanCtrl.closeScanController();
                result.success("Scan Stopped");
            }
            case "connect" -> {
                HashMap<String, Object> args = call.arguments();
                Integer deviceNumber;
                deviceNumber = (Integer) Objects.requireNonNull(args).get("deviceNumber");
                if (deviceNumber != null) {
                    connect(deviceNumber);
                    result.success("Device " + deviceNumber + " connected");
                } else {
                    result.error("Connection failed: mandatory deviceNumber not set", null, null);
                }
            }
            case "disconnect" -> {
                int deviceNumber = hrPcc.getAntDeviceNumber();
                disconnect();
                result.success("Device " + deviceNumber + " disconnected");

            }
            default -> result.notImplemented();
        }
    }

    private void startScan() {
        Log.d(TAG, "<startScan>:");
        hrScanCtrl = MyAntPlusHeartRatePcc.requestAsyncScanController(context, 0, new AsyncScanController.IAsyncScanResultReceiver() {
            @Override
            public void onSearchStopped(RequestAccessResult reasonStopped) {
                Log.d(TAG, "<onSearchStopped>:");
                //The triggers calling this function use the same codes and require the same actions as those received by the standard access result receiver
                base_IPluginAccessResultReceiver.onResultReceived(null, reasonStopped, DeviceState.DEAD);
            }

            @Override
            public void onSearchResult(final AsyncScanController.AsyncScanResultDeviceInfo deviceFound) {
                Log.d(TAG, "<onSearchResult>: number " + deviceFound.getAntDeviceNumber() + " name " + deviceFound.getDeviceDisplayName() + " already connected " + deviceFound.isAlreadyConnected());
                HashMap<String, Object> response = new HashMap<>();
                response.put("deviceNumber", deviceFound.getAntDeviceNumber());
                response.put("displayName", deviceFound.getDeviceDisplayName());
                response.put("alreadyConnected", deviceFound.isAlreadyConnected());
                invokeMethodUIThread("onScanResult", response);
            }
        });
    }

    private void connect(int deviceNumber) {
        Log.d(TAG, "<connect>: deviceNumber " + deviceNumber);
        hrScanCtrl.closeScanController();
        MyAntPlusHeartRatePcc.requestAccess(context, deviceNumber, -1, base_IPluginAccessResultReceiver, base_IDeviceStateChangeReceiver);
    }

    private void disconnect() {
        Log.d(TAG, "<disconnect>:");
        HashMap<String, Object> response = new HashMap<>();
        response.put("deviceNumber", hrPcc.getAntDeviceNumber());
        response.put("displayName", hrPcc.getDeviceName());
        response.put("alreadyConnected", false);
        invokeMethodUIThread("onScanResult", response);
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

    private void invokeMethodUIThread(final String method, HashMap<String, Object> data) {
        new Handler(Looper.getMainLooper()).post(() -> {
            //Could already be teared down at this moment
            if (channel != null) {
                channel.invokeMethod(method, data);
            } else {
                Log.w(TAG, "(invokeMethodUIThread): tried to call method on closed channel: " + method);
            }
        });
    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        Log.d(TAG, "<onAttachedToEngine>:");
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "flutter_antplus");
        context = flutterPluginBinding.getApplicationContext();
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        Log.d(TAG, "<onDetachedFromEngine>:");
        channel.setMethodCallHandler(null);
        hrScanCtrl.closeScanController();
        hrPcc.releaseAccess();
    }
}
