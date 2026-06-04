import 'dart:async';

import 'package:flutter_antplus/src/device_view_model.dart';
import 'package:flutter_antplus/src/pigeons/bikepower.g.dart';
import 'package:flutter_antplus/src/pigeons/bikepower_event_channel.g.dart'
    show
        BatteryStatus,
        PedalSmoothnessData,
        TorqueEffectivenessData,
        onBalanceData,
        onBatteryStatusData,
        onCadenceData,
        onPedalSmoothnessData,
        onPowerData,
        onTorqueEffectivenessData;
import 'package:flutter_antplus/src/pigeons/device_event_channel.g.dart'
    show
        Device,
        DeviceState,
        DeviceType,
        RequestAccessResult,
        onDeviceStateChange,
        onRequestAccessResult,
        onScanResult;

/// The Bike power view model
class BikepowerViewModel implements DeviceViewModel {
  BikepowerViewModel._();

  final BikepowerHostApi _hostApi = BikepowerHostApi();

  static BikepowerViewModel? _instance;

  /// The singleton instance of [BikepowerViewModel].
  // ignore: prefer_constructors_over_static_methods
  static BikepowerViewModel get instance =>
      _instance ??= BikepowerViewModel._();

  /// on scan result stream
  @override
  Stream<Device> get onScanResultStream =>
      onScanResult(instanceName: DeviceType.bikepower.name.toUpperCase());

  /// on scan result stream
  @override
  Stream<RequestAccessResult> get onRequestAccessResultStream =>
      onRequestAccessResult(
        instanceName: DeviceType.bikepower.name.toUpperCase(),
      );

  /// on device state change stream
  @override
  Stream<DeviceState> get onDeviceStateChangeStream => onDeviceStateChange(
    instanceName: DeviceType.bikepower.name.toUpperCase(),
  );

  /// on data stream
  Stream<int> get onPowerDataStream => onPowerData();

  /// on data stream
  Stream<int> get onBalanceDataStream => onBalanceData();

  /// on data stream
  Stream<PedalSmoothnessData> get onPedalSmoothnessDataStream =>
      onPedalSmoothnessData();

  /// on data stream
  Stream<TorqueEffectivenessData> get onTorqueEffectivenessDataStream =>
      onTorqueEffectivenessData();

  /// on data stream
  Stream<int> get onCadenceDataStream => onCadenceData();

  /// on data stream
  Stream<BatteryStatus> get onBatteryStatusDataStream => onBatteryStatusData();

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
