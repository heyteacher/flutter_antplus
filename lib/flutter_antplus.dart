import 'package:flutter_antplus/flutter_antplus_platform_interface.dart';

/// An implementation of [FlutterAntplusPlatform] that uses method channels.
class FlutterAntplus {
  /// Starts scan devices.
  Future<String?> scan() {
    return FlutterAntplusPlatform.instance.scan();
  }
}
