import 'dart:async';

import 'package:flutter_antplus/src/pigeons/device_event_channel.g.dart';

/// An abstract interface representing a common view model for managing
/// ANT+ devices.
///
/// Provides methods to scan, connect, and disconnect, as well as streams
/// to monitor discovery, access requests, and connection states.
abstract class AntplusDeviceViewModel {
  /// A stream of discovered ANT+ devices during a scan.
  ///
  /// Emits [AntplusDevice] objects representing nearby sensors.
  Stream<AntplusDevice> get onScanResultStream;

  /// A stream of access request results.
  ///
  /// Emits [AntplusRequestAccessResult] which indicates whether access
  /// to the ANT+ channel was granted or failed (e.g. user cancelled,
  /// already in use, etc.).
  Stream<AntplusRequestAccessResult> get onRequestAccessResultStream;

  /// A stream of the current device connection state.
  ///
  /// Emits [AntplusDeviceState] representing whether the device is searching,
  /// tracking, closed, etc.
  Stream<AntplusDeviceState> get onDeviceStateChangeStream;

  /// Initiates a scan for nearby ANT+ devices of the matching device type.
  Future<void> startScan();

  /// Stops an active ANT+ device scan.
  Future<void> stopScan();

  /// Connects to a specific ANT+ device identified by its [deviceNumber].
  ///
  /// Discovers and connects to the device. Once connected, sensor-specific
  /// data streams will begin emitting values.
  Future<void> connect(int deviceNumber);

  /// Disconnects from the currently connected ANT+ device.
  Future<void> disconnect();
}
