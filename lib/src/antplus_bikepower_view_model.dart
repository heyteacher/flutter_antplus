import 'dart:async';

import 'package:flutter_antplus/src/antplus_device_view_model.dart';
import 'package:flutter_antplus/src/pigeons/bikepower.g.dart';
import 'package:flutter_antplus/src/pigeons/bikepower_event_channel.g.dart'
    show
        AntplusBatteryStatus,
        AntplusPedalSmoothnessData,
        AntplusTorqueEffectivenessData,
        onBalanceData,
        onBatteryStatusData,
        onCadenceData,
        onPedalSmoothnessData,
        onPowerData,
        onTorqueEffectivenessData;
import 'package:flutter_antplus/src/pigeons/device_event_channel.g.dart'
    show
        AntplusDevice,
        AntplusDeviceState,
        AntplusDeviceType,
        AntplusRequestAccessResult,
        onDeviceStateChange,
        onRequestAccessResult,
        onScanResult;

/// The Bike power view model
class AntplusBikepowerViewModel implements AntplusDeviceViewModel {
  AntplusBikepowerViewModel._();

  final BikepowerHostApi _hostApi = BikepowerHostApi();

  static AntplusBikepowerViewModel? _instance;

  /// The singleton instance of [AntplusBikepowerViewModel].
  // ignore: prefer_constructors_over_static_methods
  static AntplusBikepowerViewModel get instance =>
      _instance ??= AntplusBikepowerViewModel._();

  /// on scan result stream
  @override
  Stream<AntplusDevice> get onScanResultStream => onScanResult(
    instanceName: AntplusDeviceType.bikepower.name.toUpperCase(),
  );

  /// on scan result stream
  @override
  Stream<AntplusRequestAccessResult> get onRequestAccessResultStream =>
      onRequestAccessResult(
        instanceName: AntplusDeviceType.bikepower.name.toUpperCase(),
      );

  /// on device state change stream
  @override
  Stream<AntplusDeviceState> get onDeviceStateChangeStream =>
      onDeviceStateChange(
        instanceName: AntplusDeviceType.bikepower.name.toUpperCase(),
      );

  /// on data stream
  Stream<int> get onPowerDataStream => onPowerData();

  /// on data stream
  Stream<int> get onBalanceDataStream => onBalanceData();

  /// on data stream
  Stream<AntplusPedalSmoothnessData> get onPedalSmoothnessDataStream =>
      onPedalSmoothnessData();

  /// on data stream
  Stream<AntplusTorqueEffectivenessData> get onTorqueEffectivenessDataStream =>
      onTorqueEffectivenessData();

  /// on data stream
  Stream<int> get onCadenceDataStream => onCadenceData();

  /// on data stream
  Stream<AntplusBatteryStatus> get onBatteryStatusDataStream =>
      onBatteryStatusData();

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
