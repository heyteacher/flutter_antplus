import 'dart:async';

import 'package:flutter_antplus/src/antplus_device_view_model.dart';
import 'package:flutter_antplus/src/pigeons/device_event_channel.g.dart'
    show
        AntplusDevice,
        AntplusDeviceState,
        AntplusDeviceType,
        AntplusRequestAccessResult,
        onDeviceStateChange,
        onRequestAccessResult,
        onScanResult;
import 'package:flutter_antplus/src/pigeons/heartrate.g.dart';
import 'package:flutter_antplus/src/pigeons/heartrate_event_channel.g.dart'
    show onHeartRateData;

/// A view model managing the connection and data stream for an
/// ANT+ Heart Rate Monitor (HRM).
///
/// Implements [AntplusDeviceViewModel] to handle scanning, connection,
/// and provides the [onHeartRateDataStream] to read heart rate values.
class AntplusHeartrateViewModel implements AntplusDeviceViewModel {
  AntplusHeartrateViewModel._();

  final HeartrateHostApi _hostApi = HeartrateHostApi();

  static AntplusHeartrateViewModel? _instance;

  /// The singleton instance of [AntplusHeartrateViewModel].
  // ignore: prefer_constructors_over_static_methods
  static AntplusHeartrateViewModel get instance =>
      _instance ??= AntplusHeartrateViewModel._();

  /// A stream of discovered ANT+ heart rate devices.
  @override
  Stream<AntplusDevice> get onScanResultStream => onScanResult(
    instanceName: AntplusDeviceType.heartrate.name.toUpperCase(),
  );

  /// A stream of access request results specifically for heart rate sensors.
  @override
  Stream<AntplusRequestAccessResult> get onRequestAccessResultStream =>
      onRequestAccessResult(
        instanceName: AntplusDeviceType.heartrate.name.toUpperCase(),
      );

  /// A stream of the connection state for the heart rate device.
  @override
  Stream<AntplusDeviceState> get onDeviceStateChangeStream =>
      onDeviceStateChange(
        instanceName: AntplusDeviceType.heartrate.name.toUpperCase(),
      );

  /// A stream of the real-time heart rate value in beats per minute (bpm).
  Stream<int> get onHeartRateDataStream => onHeartRateData();

  /// Starts scanning for nearby ANT+ heart rate monitors.
  @override
  Future<void> startScan() => _hostApi.startScan();

  /// Stops the active scan for heart rate monitors.
  @override
  Future<void> stopScan() => _hostApi.stopScan();

  /// Connects to a heart rate monitor with the specified [deviceNumber].
  @override
  Future<void> connect(int deviceNumber) => _hostApi.connect(deviceNumber);

  /// Disconnects from the current heart rate monitor.
  @override
  Future<void> disconnect() => _hostApi.disconnect();
}
