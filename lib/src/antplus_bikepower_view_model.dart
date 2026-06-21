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

/// A view model managing the connection and telemetry streams for an
/// ANT+ Bike Power Meter.
///
/// Implements [AntplusDeviceViewModel] to handle scanning, connection,
/// and exposes multiple streams containing power, balance, cadence,
/// pedal smoothness, torque effectiveness, and battery information.
class AntplusBikepowerViewModel implements AntplusDeviceViewModel {
  AntplusBikepowerViewModel._();

  final BikepowerHostApi _hostApi = BikepowerHostApi();

  static AntplusBikepowerViewModel? _instance;

  /// The singleton instance of [AntplusBikepowerViewModel].
  // ignore: prefer_constructors_over_static_methods
  static AntplusBikepowerViewModel get instance =>
      _instance ??= AntplusBikepowerViewModel._();

  /// A stream of discovered ANT+ bike power meters.
  @override
  Stream<AntplusDevice> get onScanResultStream => onScanResult(
    instanceName: AntplusDeviceType.bikepower.name.toUpperCase(),
  );

  /// A stream of access request results specifically for bike power sensors.
  @override
  Stream<AntplusRequestAccessResult> get onRequestAccessResultStream =>
      onRequestAccessResult(
        instanceName: AntplusDeviceType.bikepower.name.toUpperCase(),
      );

  /// A stream of the connection state for the bike power device.
  @override
  Stream<AntplusDeviceState> get onDeviceStateChangeStream =>
      onDeviceStateChange(
        instanceName: AntplusDeviceType.bikepower.name.toUpperCase(),
      );

  /// A stream of the real-time power value in watts (W).
  Stream<int> get onPowerDataStream => onPowerData();

  /// A stream of the pedal power balance percentage.
  Stream<int> get onBalanceDataStream => onBalanceData();

  /// A stream of pedal smoothness data containing left and right
  /// smoothness percentages.
  Stream<AntplusPedalSmoothnessData> get onPedalSmoothnessDataStream =>
      onPedalSmoothnessData();

  /// A stream of torque effectiveness data containing left and right
  /// torque effectiveness percentages.
  Stream<AntplusTorqueEffectivenessData> get onTorqueEffectivenessDataStream =>
      onTorqueEffectivenessData();

  /// A stream of the real-time cadence value in revolutions per minute (rpm)
  /// calculated by the power meter.
  Stream<int> get onCadenceDataStream => onCadenceData();

  /// A stream of the battery status of the bike power sensor.
  Stream<AntplusBatteryStatus> get onBatteryStatusDataStream =>
      onBatteryStatusData();

  /// Starts scanning for nearby ANT+ bike power meters.
  @override
  Future<void> startScan() => _hostApi.startScan();

  /// Stops the active scan for bike power meters.
  @override
  Future<void> stopScan() => _hostApi.stopScan();

  /// Connects to a bike power meter with the specified [deviceNumber].
  @override
  Future<void> connect(int deviceNumber) => _hostApi.connect(deviceNumber);

  /// Disconnects from the current bike power meter.
  @override
  Future<void> disconnect() => _hostApi.disconnect();
}
