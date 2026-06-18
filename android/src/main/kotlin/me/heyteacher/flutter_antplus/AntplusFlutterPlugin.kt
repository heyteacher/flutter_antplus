@file:Suppress("unused")
package me.heyteacher.flutter_antplus

import io.flutter.embedding.engine.plugins.FlutterPlugin
import me.heyteacher.flutter_antplus.bikepower.BikePowerHostApiImpl
import me.heyteacher.flutter_antplus.cadence.CadenceHostApiImpl
import me.heyteacher.flutter_antplus.heartrate.HeartrateHostApiImpl
import me.heyteacher.flutter_antplus.logging.OnLogDataListener
import me.heyteacher.flutter_antplus.logging.pigeons.OnLogDataStreamHandler

class AntplusFlutterPlugin : FlutterPlugin {
    private var onLogDataListener: OnLogDataListener = OnLogDataListener()
    private lateinit var heartrateHostApiImpl: HeartrateHostApiImpl
    private lateinit var bikepowerHostApiImpl: BikePowerHostApiImpl
    private lateinit var cadenceHostApiImpl: CadenceHostApiImpl

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
        cadenceHostApiImpl = CadenceHostApiImpl(
            flutterPluginBinding.applicationContext,
            flutterPluginBinding.binaryMessenger,
            onLogDataListener
        )
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        onLogDataListener.onEventsDone()
        heartrateHostApiImpl.close()
        bikepowerHostApiImpl.close()
        cadenceHostApiImpl.close()
    }
}