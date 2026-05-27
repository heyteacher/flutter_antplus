import 'package:flutter_antplus/flutter_antplus_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

/// The interface that implementations of flutter_antplus must implement.
abstract class FlutterAntplusPlatform extends PlatformInterface {
  /// Constructs a FlutterAntplusPlatform.
  FlutterAntplusPlatform() : super(token: _token);

  static final Object _token = Object();

  static FlutterAntplusPlatform _instance = MethodChannelFlutterAntplus();

  /// The default instance of [FlutterAntplusPlatform] to use.
  ///
  /// Defaults to [MethodChannelFlutterAntplus].
  static FlutterAntplusPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [FlutterAntplusPlatform] when
  /// they register themselves.
  static set instance(FlutterAntplusPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  /// On scan result stream.
  Stream<Map<dynamic, dynamic>> get onScanResultStream;

  /// On heart rate data stream.
  Stream<Map<dynamic, dynamic>> get onHeartRateDataStream;

  /// Starts scan devices.
  Future<String?> startScan();

  /// Stop scan devices.
  Future<String?> stopScan();

  /// Connect to device with [deviceNumber].
  Future<String?> connect(int deviceNumber);

  /// Disconnect to device .
  Future<String?> disconnect();
}
