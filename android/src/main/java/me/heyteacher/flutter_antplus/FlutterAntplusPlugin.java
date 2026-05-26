package me.heyteacher.flutter_antplus;

import android.content.Context;

import androidx.annotation.NonNull;

import io.flutter.Log;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import me.heyteacher.flutter_antplus.heartrate.MyAntPlusHeartRatePcc;
import me.heyteacher.flutter_antplus.heartrate.MyAntPlusHeartRatePcc.DataState;

import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;
import com.dsi.ant.plugins.antplus.pccbase.AsyncScanController;

/** FlutterAntplusPlugin */
public class FlutterAntplusPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity

  private static final String TAG = FlutterAntplusPlugin.class.getSimpleName();
  private MethodChannel channel;
  private Context context;
  //private Activity activity;
  private MyAntPlusHeartRatePcc hrPcc;
  private AsyncScanController<MyAntPlusHeartRatePcc> hrScanCtrl;
  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    Log.d(TAG, "onAttachedToEngine ");
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "flutter_antplus");
    context = flutterPluginBinding.getApplicationContext();
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    Log.d(TAG, "onMethodCall " + call.method);
    if (call.method.equals("scan")) {
      requestAccessToPcc();
      result.success("Start Scan...");
    } else {
      result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    Log.d(TAG, "onDetachedFromEngine");
    channel.setMethodCallHandler(null);
    hrScanCtrl.closeScanController();
    hrPcc.releaseAccess();
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding activityPluginBinding) {
    Log.d(TAG, "onAttachedToActivity");
    //activity = activityPluginBinding.getActivity();
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    Log.d(TAG, "onDetachedFromActivityForConfigChanges");
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding activityPluginBinding) {
    Log.d(TAG, "onReattachedToActivityForConfigChanges");
  }

  @Override
  public void onDetachedFromActivity() {
    Log.d(TAG, "onDetachedFromActivity");
  }

  private void requestAccessToPcc()
  {
    Log.d(TAG, "requestAccessToPcc");
    hrScanCtrl = MyAntPlusHeartRatePcc.requestAsyncScanController(context, 0,
            new AsyncScanController.IAsyncScanResultReceiver()
            {
              @Override
              public void onSearchStopped(RequestAccessResult reasonStopped)
              {
                Log.d(TAG, "onSearchStopped");
                //The triggers calling this function use the same codes and require the same actions as those received by the standard access result receiver
                base_IPluginAccessResultReceiver.onResultReceived(null, reasonStopped, DeviceState.DEAD);
              }

              @Override
              public void onSearchResult(final AsyncScanController.AsyncScanResultDeviceInfo deviceFound)
              {
                Log.d(TAG, "onSearchResult number " + deviceFound.getAntDeviceNumber() +
                        " name " + deviceFound.getDeviceDisplayName() +
                        " already connected " + deviceFound.isAlreadyConnected() );
              }
            });
  }
  protected AntPluginPcc.IPluginAccessResultReceiver<MyAntPlusHeartRatePcc> base_IPluginAccessResultReceiver =
          new AntPluginPcc.IPluginAccessResultReceiver<>() {
              //Handle the result, connecting to events on success or reporting failure to user.
              @Override
              public void onResultReceived(MyAntPlusHeartRatePcc result, RequestAccessResult resultCode,
                                           DeviceState initialDeviceState) {
                Log.d(TAG, "onResultReceived resultCode " + resultCode);
                switch (resultCode) {
                      case SUCCESS:
                          hrPcc = result;
                          Log.i(TAG, result.getDeviceName() + ": " + initialDeviceState);
                          subscribeToHrEvents();
                          break;
                      case DEPENDENCY_NOT_INSTALLED:
                          Log.i(TAG, "The required service\"" + MyAntPlusHeartRatePcc.getMissingDependencyName() + "\"was not found. You need to install the ANT+ Plugins service or you may need to update your existing version if you already have it. Do you want to launch the Play Store to get it?");
                          break;
                      case UNRECOGNIZED:
                      case CHANNEL_NOT_AVAILABLE:
                      case ADAPTER_NOT_DETECTED:
                      case BAD_PARAMS:
                      case OTHER_FAILURE:
                      case USER_CANCELLED:
                          Log.i(TAG, "Cancelled. Do Menu->Reset.");
                          break;
                      default:
                          Log.i(TAG, "Error. Do Menu->Reset.");
                          break;
                  }
              }
          };
  public void subscribeToHrEvents() {
    Log.d(TAG, "subscribeToHrEvents");
    hrPcc.subscribeHeartRateDataEvent((estTimestamp, eventFlags, computedHeartRate, heartBeatCount, heartBeatEventTime, dataState) -> {
      Log.d(TAG, "subscribeHeartRateDataEvent");
      // Mark heart rate with asterisk if zero detected
      final String textHeartRate = computedHeartRate
              + ((DataState.ZERO_DETECTED.equals(dataState)) ? "*" : "");

      // Mark heart beat count and heart beat event time with asterisk if initial value
      final String textHeartBeatCount = heartBeatCount
              + ((DataState.INITIAL_VALUE.equals(dataState)) ? "*" : "");
      final String textHeartBeatEventTime = heartBeatEventTime
              + ((DataState.INITIAL_VALUE.equals(dataState)) ? "*" : "");
      Log.d(TAG, " HeartRate " + textHeartRate +
              "HeartBeatCount" + textHeartBeatCount +
              "HeartBeatEventTime" + textHeartBeatEventTime);
    });
  }
}
