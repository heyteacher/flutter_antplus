package me.heyteacher.flutter_antplus;

import androidx.annotation.NonNull;

import io.flutter.Log;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import me.heyteacher.flutter_antplus.heartrate.HeartrateHostApiImpl;

/**
 * Antplus Plugin
 */
@SuppressWarnings({"unused"})
public class AntplusFlutterPlugin implements FlutterPlugin {
    private static final String TAG = AntplusFlutterPlugin.class.getSimpleName();

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        Log.d(TAG, "<onAttachedToEngine>:");
        new HeartrateHostApiImpl(flutterPluginBinding.getApplicationContext(), flutterPluginBinding.getBinaryMessenger());
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        Log.d(TAG, "<onDetachedFromEngine>:");
    }
}
