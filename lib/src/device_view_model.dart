import 'dart:async';

import 'package:flutter_antplus/src/pigeons/device_event_channel.g.dart';

/// The Device view model interface
abstract class DeviceViewModel {
  /// on scan result stream
  Stream<Device> get onScanResultStream;

  /// on scan result stream
  Stream<RequestAccessResult> get onRequestAccessResultStream;

  /// on device state change stream
  Stream<DeviceState> get onDeviceStateChangeStream;

  /// Starts scan.
  Future<void> startScan();

  /// Stop scan .
  Future<void> stopScan();

  /// Connect to device with [deviceNumber].
  Future<void> connect(int deviceNumber);

  /// Disconnect to device.
  Future<void> disconnect();
}
