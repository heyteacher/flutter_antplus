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

/// The Heart Rate view model
class AntplusHeartrateViewModel implements AntplusDeviceViewModel {
  AntplusHeartrateViewModel._();

  final HeartrateHostApi _hostApi = HeartrateHostApi();

  static AntplusHeartrateViewModel? _instance;

  /// The singleton instance of [AntplusHeartrateViewModel].
  // ignore: prefer_constructors_over_static_methods
  static AntplusHeartrateViewModel get instance =>
      _instance ??= AntplusHeartrateViewModel._();

  /// on scan result stream
  @override
  Stream<AntplusDevice> get onScanResultStream => onScanResult(
    instanceName: AntplusDeviceType.heartrate.name.toUpperCase(),
  );

  /// on scan result stream
  @override
  Stream<AntplusRequestAccessResult> get onRequestAccessResultStream =>
      onRequestAccessResult(
        instanceName: AntplusDeviceType.heartrate.name.toUpperCase(),
      );

  /// on device state change stream
  @override
  Stream<AntplusDeviceState> get onDeviceStateChangeStream =>
      onDeviceStateChange(
        instanceName: AntplusDeviceType.heartrate.name.toUpperCase(),
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
