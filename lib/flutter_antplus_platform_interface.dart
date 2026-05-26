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

  /// Starts scan devices.
  Future<String?> scan() {
    throw UnimplementedError('scan() has not been implemented.');
  }
}
