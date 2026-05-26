import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'package:flutter_antplus/flutter_antplus_platform_interface.dart';

/// An implementation of [FlutterAntplusPlatform] that uses method channels.
class MethodChannelFlutterAntplus extends FlutterAntplusPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('flutter_antplus');

  @override
  Future<String?> scan() async {
    final resultCode = await methodChannel.invokeMethod<String>('scan');
    return resultCode;
  }
}
