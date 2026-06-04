@file:Suppress("unused")
package me.heyteacher.flutter_antplus

import io.flutter.embedding.engine.plugins.FlutterPlugin
import me.heyteacher.flutter_antplus.bikepower.BikePowerHostApiImpl
import me.heyteacher.flutter_antplus.heartrate.HeartrateHostApiImpl
import me.heyteacher.flutter_antplus.logging.OnLogDataListener
import me.heyteacher.flutter_antplus.logging.pigeons.OnLogDataStreamHandler

class AntplusFlutterPlugin : FlutterPlugin {

    private lateinit var heartrateHostApiImpl: HeartrateHostApiImpl
    private lateinit var bikepowerHostApiImpl: BikePowerHostApiImpl
    private var onLogDataListener: OnLogDataListener = OnLogDataListener()

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        OnLogDataStreamHandler.register(flutterPluginBinding.binaryMessenger, onLogDataListener)
        heartrateHostApiImpl = HeartrateHostApiImpl(
            flutterPluginBinding.applicationContext,
            flutterPluginBinding.binaryMessenger,
            onLogDataListener
        )
        bikepowerHostApiImpl = BikePowerHostApiImpl(
            flutterPluginBinding.applicationContext,
            flutterPluginBinding.binaryMessenger,
            onLogDataListener
        )
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        heartrateHostApiImpl.close()
        bikepowerHostApiImpl.close()
        onLogDataListener.onEventsDone()
    }
}