@file:Suppress("unused")
package me.heyteacher.flutter_antplus

import io.flutter.embedding.engine.plugins.FlutterPlugin
import me.heyteacher.flutter_antplus.bikepower.BikePowerHostApiImpl
import me.heyteacher.flutter_antplus.cadence.CadenceHostApiImpl
import me.heyteacher.flutter_antplus.device.OnRequestAccessListener
import me.heyteacher.flutter_antplus.device.pigeons.OnRequestAccessStreamHandler
import me.heyteacher.flutter_antplus.heartrate.HeartrateHostApiImpl
import me.heyteacher.flutter_antplus.logging.OnLogDataListener
import me.heyteacher.flutter_antplus.logging.pigeons.OnLogDataStreamHandler

/**
 * The main Android plugin implementation for the ANT+ Flutter plugin.
 *
 * This class coordinates the plugin lifecycle, instantiates the host API implementations
 * for Heart Rate, Bike Power, and Cadence sensors, and manages the shared native log listeners.
 */
class AntplusFlutterPlugin : FlutterPlugin {
    private var onLogDataListener: OnLogDataListener = OnLogDataListener()

    private var onRequestAccessListener: OnRequestAccessListener = OnRequestAccessListener()

    private lateinit var heartrateHostApiImpl: HeartrateHostApiImpl
    private lateinit var bikepowerHostApiImpl: BikePowerHostApiImpl
    private lateinit var cadenceHostApiImpl: CadenceHostApiImpl

    /**
     * Initializes the plugin resources and registers the Pigeon Host APIs
     * when the Flutter engine attaches to the Android application.
     */
    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        OnLogDataStreamHandler.register(flutterPluginBinding.binaryMessenger, onLogDataListener)
        OnRequestAccessStreamHandler.register(flutterPluginBinding.binaryMessenger, onRequestAccessListener)
        AbstractHostApi.initializeUsbDevice(flutterPluginBinding.applicationContext, onLogDataListener, onRequestAccessListener)
        heartrateHostApiImpl = HeartrateHostApiImpl(flutterPluginBinding.binaryMessenger, flutterPluginBinding.applicationContext)
        bikepowerHostApiImpl = BikePowerHostApiImpl(flutterPluginBinding.binaryMessenger, flutterPluginBinding.applicationContext)
        cadenceHostApiImpl = CadenceHostApiImpl(flutterPluginBinding.binaryMessenger, flutterPluginBinding.applicationContext)
    }

    /**
     * Releases active subscriptions, connections, and closes all host APIs
     * when the Flutter engine detaches from the Android application.
     */
    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        onLogDataListener.onEventsDone()
        heartrateHostApiImpl.close()
        bikepowerHostApiImpl.close()
        cadenceHostApiImpl.close()
    }
}

