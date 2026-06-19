import 'dart:async';

import 'package:flutter_antplus/src/antplus_device_view_model.dart';
import 'package:flutter_antplus/src/pigeons/cadence.g.dart';
import 'package:flutter_antplus/src/pigeons/cadence_event_channel.g.dart'
    show onCadenceData;
import 'package:flutter_antplus/src/pigeons/device_event_channel.g.dart'
    show
        AntplusDevice,
        AntplusDeviceState,
        AntplusDeviceType,
        AntplusRequestAccessResult,
        onDeviceStateChange,
        onRequestAccessResult,
        onScanResult;

/// The Heart Rate view model
class AntplusCadenceViewModel implements AntplusDeviceViewModel {
  AntplusCadenceViewModel._();

  final CadenceHostApi _hostApi = CadenceHostApi();

  static AntplusCadenceViewModel? _instance;

  /// The singleton instance of [AntplusCadenceViewModel].
  // ignore: prefer_constructors_over_static_methods
  static AntplusCadenceViewModel get instance =>
      _instance ??= AntplusCadenceViewModel._();

  /// on scan result stream
  @override
  Stream<AntplusDevice> get onScanResultStream =>
      onScanResult(instanceName: AntplusDeviceType.cadence.name.toUpperCase());

  /// on scan result stream
  @override
  Stream<AntplusRequestAccessResult> get onRequestAccessResultStream =>
      onRequestAccessResult(
        instanceName: AntplusDeviceType.cadence.name.toUpperCase(),
      );

  /// on device state change stream
  @override
  Stream<AntplusDeviceState> get onDeviceStateChangeStream =>
      onDeviceStateChange(
        instanceName: AntplusDeviceType.cadence.name.toUpperCase(),
      );

  /// on data stream
  Stream<int> get onCadenceDataStream => onCadenceData();

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
