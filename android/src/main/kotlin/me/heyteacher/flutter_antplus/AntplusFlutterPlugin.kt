@file:Suppress("unused")

package me.heyteacher.flutter_antplus

import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import me.heyteacher.flutter_antplus.bikepower.BikePowerHostApiImpl
import me.heyteacher.flutter_antplus.cadence.CadenceHostApiImpl
import me.heyteacher.flutter_antplus.usb_adapter.OnUsbAdapterStateChangeStreamHandlerImpl
import me.heyteacher.flutter_antplus.usb_adapter.UsbAntplusAdapterManager
import me.heyteacher.flutter_antplus.heartrate.HeartrateHostApiImpl
import me.heyteacher.flutter_antplus.logging.OnLogDataStreamHandlerImpl
import me.heyteacher.flutter_antplus.logging.pigeons.OnLogDataStreamHandler
import me.heyteacher.flutter_antplus.usb_adapter.pigeons.OnUsbAdapterStateChangeStreamHandler

/**
 * The main Android plugin implementation for the ANT+ Flutter plugin.
 *
 * This class coordinates the plugin lifecycle, instantiates the host API implementations
 * for Heart Rate, Bike Power, and Cadence sensors, and manages the shared native log listeners.
 */
class AntplusFlutterPlugin : FlutterPlugin {
    private var onLogDataStreamHandler: OnLogDataStreamHandlerImpl = OnLogDataStreamHandlerImpl()
    private var onAdapterStateChangeStreamHandler: OnUsbAdapterStateChangeStreamHandlerImpl =
        OnUsbAdapterStateChangeStreamHandlerImpl()
    private lateinit var heartrateHostApiImpl: HeartrateHostApiImpl
    private lateinit var bikepowerHostApiImpl: BikePowerHostApiImpl
    private lateinit var cadenceHostApiImpl: CadenceHostApiImpl

    /**
     * Initializes the plugin resources and registers the Pigeon Host APIs
     * when the Flutter engine attaches to the Android application.
     */
    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {

        try {// register listeners
            OnLogDataStreamHandler.register(
                flutterPluginBinding.binaryMessenger,
                onLogDataStreamHandler
            )
            OnUsbAdapterStateChangeStreamHandler.register(
                flutterPluginBinding.binaryMessenger,
                onAdapterStateChangeStreamHandler
            )

            // instantiate host api implementation
            heartrateHostApiImpl = HeartrateHostApiImpl(
                flutterPluginBinding.binaryMessenger,
                flutterPluginBinding.applicationContext,
                onLogDataStreamHandler,
                onAdapterStateChangeStreamHandler
            )
            bikepowerHostApiImpl = BikePowerHostApiImpl(
                flutterPluginBinding.binaryMessenger,
                flutterPluginBinding.applicationContext,
                onLogDataStreamHandler,
                onAdapterStateChangeStreamHandler
            )
            cadenceHostApiImpl = CadenceHostApiImpl(
                flutterPluginBinding.binaryMessenger,
                flutterPluginBinding.applicationContext,
                onLogDataStreamHandler,
                onAdapterStateChangeStreamHandler
            )
            // initialize usb device manager listeners
            UsbAntplusAdapterManager.getInstance().initializeListeners(
                onLogDataStreamHandler,
                onAdapterStateChangeStreamHandler
            )
        } catch (t: Throwable) {
            Log.e("AntplusFlutterPlugin", "(onAttachedToEngine): error", t)
            throw t
        }
    }

    /**
     * Releases active subscriptions, connections, and closes all host APIs
     * when the Flutter engine detaches from the Android application.
     */
    override fun onDetachedFromEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        onLogDataStreamHandler.onEventsDone()
        onAdapterStateChangeStreamHandler.onEventsDone()
        UsbAntplusAdapterManager.getInstance().close()
        heartrateHostApiImpl.close()
        bikepowerHostApiImpl.close()
        cadenceHostApiImpl.close()
    }
}