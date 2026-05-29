package me.heyteacher.flutter_antplus

import io.flutter.embedding.engine.plugins.FlutterPlugin
import me.heyteacher.flutter_antplus.heartrate.HeartrateHostApiImpl

class AntplusFlutterPlugin : FlutterPlugin {

    private lateinit var heartrateHostApiImpl: HeartrateHostApiImpl

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        heartrateHostApiImpl = HeartrateHostApiImpl(
            flutterPluginBinding.applicationContext,
            flutterPluginBinding.binaryMessenger
        )
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        heartrateHostApiImpl.close()
    }
}