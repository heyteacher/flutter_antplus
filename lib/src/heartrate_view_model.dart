import 'dart:async';

import 'package:flutter_antplus/src/device_view_model.dart';
import 'package:flutter_antplus/src/pigeons/device_event_channel.g.dart'
    show
        Device,
        DeviceState,
        DeviceType,
        RequestAccessResult,
        onDeviceStateChange,
        onRequestAccessResult,
        onScanResult;
import 'package:flutter_antplus/src/pigeons/heartrate.g.dart';
import 'package:flutter_antplus/src/pigeons/heartrate_event_channel.g.dart'
    show onHeartRateData;

/// The Heart Rate view model
class HeartrateViewModel implements DeviceViewModel {
  HeartrateViewModel._();

  final HeartrateHostApi _hostApi = HeartrateHostApi();

  static HeartrateViewModel? _instance;

  /// The singleton instance of [HeartrateViewModel].
  // ignore: prefer_constructors_over_static_methods
  static HeartrateViewModel get instance =>
      _instance ??= HeartrateViewModel._();

  /// on scan result stream
  @override
  Stream<Device> get onScanResultStream =>
      onScanResult(instanceName: DeviceType.heartrate.name.toUpperCase());

  /// on scan result stream
  @override
  Stream<RequestAccessResult> get onRequestAccessResultStream =>
      onRequestAccessResult(
        instanceName: DeviceType.heartrate.name.toUpperCase(),
      );

  /// on device state change stream
  @override
  Stream<DeviceState> get onDeviceStateChangeStream => onDeviceStateChange(
    instanceName: DeviceType.heartrate.name.toUpperCase(),
  );

  /// on data stream
  Stream<int> get onHeartRateDataStream => onHeartRateData();

  /// Starts scan.
  @override
  Future<void> startScan() => _hostApi.startScan();

  /// Stop scan .
  @override
  Future<void> stopScan() => _hostApi.stopScan();

  /// Connect to device with [deviceNumber].
  @override
  Future<void> connect(int deviceNumber) => _hostApi.connect(deviceNumber);

  /// Disconnect to device .
  @override
  Future<void> disconnect() => _hostApi.disconnect();
}
