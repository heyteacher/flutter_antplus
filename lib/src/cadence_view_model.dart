import 'dart:async';

import 'package:flutter_antplus/src/device_view_model.dart';
import 'package:flutter_antplus/src/pigeons/cadence.g.dart';
import 'package:flutter_antplus/src/pigeons/cadence_event_channel.g.dart'
    show onCadenceData;
import 'package:flutter_antplus/src/pigeons/device_event_channel.g.dart'
    show
        Device,
        DeviceState,
        DeviceType,
        RequestAccessResult,
        onDeviceStateChange,
        onRequestAccessResult,
        onScanResult;

/// The Heart Rate view model
class CadenceViewModel implements DeviceViewModel {
  CadenceViewModel._();

  final CadenceHostApi _hostApi = CadenceHostApi();

  static CadenceViewModel? _instance;

  /// The singleton instance of [CadenceViewModel].
  // ignore: prefer_constructors_over_static_methods
  static CadenceViewModel get instance => _instance ??= CadenceViewModel._();

  /// on scan result stream
  @override
  Stream<Device> get onScanResultStream =>
      onScanResult(instanceName: DeviceType.cadence.name.toUpperCase());

  /// on scan result stream
  @override
  Stream<RequestAccessResult> get onRequestAccessResultStream =>
      onRequestAccessResult(
        instanceName: DeviceType.cadence.name.toUpperCase(),
      );

  /// on device state change stream
  @override
  Stream<DeviceState> get onDeviceStateChangeStream => onDeviceStateChange(
    instanceName: DeviceType.cadence.name.toUpperCase(),
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
