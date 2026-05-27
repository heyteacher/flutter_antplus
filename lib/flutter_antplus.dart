import 'package:flutter_antplus/flutter_antplus_platform_interface.dart';

/// An implementation of [FlutterAntplusPlatform] that uses method channels.
class FlutterAntplus {
  /// Starts scan.
  Future<String?> startScan() => FlutterAntplusPlatform.instance.startScan();

  /// Stop scan .
  Future<String?> stopScan() => FlutterAntplusPlatform.instance.stopScan();

  /// Connect to device with [deviceNumber].
  Future<String?> connect(int deviceNumber) =>
      FlutterAntplusPlatform.instance.connect(deviceNumber);

  /// Disconnect to device .
  Future<String?> disconnect() => FlutterAntplusPlatform.instance.disconnect();

  /// On scan result stream.
  Stream<Map<dynamic, dynamic>> get onScanResultStream =>
      FlutterAntplusPlatform.instance.onScanResultStream;

  /// On scan result stream.
  Stream<Map<dynamic, dynamic>> get onHeartRateDataStream =>
      FlutterAntplusPlatform.instance.onHeartRateDataStream;
}
