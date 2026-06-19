import 'dart:async';

import 'package:flutter_antplus/src/pigeons/device_event_channel.g.dart';

/// The Device view model interface
abstract class AntplusDeviceViewModel {
  /// on scan result stream
  Stream<AntplusDevice> get onScanResultStream;

  /// on scan result stream
  Stream<AntplusRequestAccessResult> get onRequestAccessResultStream;

  /// on device state change stream
  Stream<AntplusDeviceState> get onDeviceStateChangeStream;

  /// Starts scan.
  Future<void> startScan();

  /// Stop scan .
  Future<void> stopScan();

  /// Connect to device with [deviceNumber].
  Future<void> connect(int deviceNumber);

  /// Disconnect to device.
  Future<void> disconnect();
}
