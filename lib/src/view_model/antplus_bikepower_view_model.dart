import 'dart:async';

import 'package:flutter_antplus/flutter_antplus.dart';
import 'package:flutter_antplus/src/pigeons/bikepower.g.dart';
import 'package:flutter_antplus/src/pigeons/bikepower_event_channel.g.dart'
    show
        AntplusPedalSmoothnessData,
        AntplusTorqueEffectivenessData,
        onAdvancedCapabilitiesData,
        onBalanceData,
        onCadenceData,
        onCrankLengthInMm,
        onPedalSmoothnessData,
        onPowerData,
        onTorqueEffectivenessData;
import 'package:flutter_antplus/src/pigeons/device_event_channel.g.dart'
    show
        AntplusDevice,
        AntplusDeviceState,
        AntplusDeviceType,
        onBatteryStatusData,
        onDeviceStateChange,
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

  /// A stream of the connection state for the bike power device.
  @override
  Stream<AntplusDeviceState> get onDeviceStateChangeStream =>
      onDeviceStateChange(
        instanceName: AntplusDeviceType.bikepower.name.toUpperCase(),
      );

  /// A stream of the battery status of the bike power sensor.
  @override
  Stream<AntplusBatteryStatus> get onBatteryStatusDataStream =>
      onBatteryStatusData(
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

  /// A stream of crank length data from the bike power meter in millimeters.
  Stream<double> get onCrankLengthInMmStream => onCrankLengthInMm();

  /// A stream of advanced capabilities data from the bike power meter.
  Stream<AntplusAdvancedCapabilitiesData>
  get onAdvancedCapabilitiesDataStream => onAdvancedCapabilitiesData();

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

  /// Sets the crank length in millimeters for the bike power meter.
  Future<void> setCrankLengthInMm({required double crankLengthInMm}) =>
      _hostApi.setCrankLengthInMm(crankLengthInMm: crankLengthInMm);

  /// Requests the crank length in millimeters from the bike power meter.
  ///
  /// Listen [onCrankLengthInMmStream] to receive the crank length value.
  Future<void> requestCrankLengthInMm() => _hostApi.requestCrankLengthInMm();

  /// Requests the advanced capabilities from the bike power meter.
  ///
  /// Listen [onAdvancedCapabilitiesDataStream] to receive the advanced
  /// capabilities value.
  Future<void> requestAdvancedCapabilities() =>
      _hostApi.requestAdvancedCapabilities();

  /// Enable or disable the bike power to send data at 8Hz frequency.
  ///
  /// If [enable] is true, the bike power will send data at 8Hz frequency.
  /// If [enable] is false, the bike power will send data at 4Hz frequency.
  Future<void> set8Hertz({required bool enable}) =>
      _hostApi.set8Hertz(enable: enable);

  /// Enable or disable PCO (Phase Compensation Offset).
  ///
  /// If [enable] is true, PCO is enabled, otherwise is disabled.
  Future<void> setPco({required bool enable}) =>
      _hostApi.setPco(enable: enable);

  /// Enable or disable Power Phase.
  ///
  /// If [enable] is true, Power Phase is enabled, otherwise is disabled.
  Future<void> setPowerPhase({required bool enable}) =>
      _hostApi.setPowerPhase(enable: enable);

  /// Enable or disable Rider Position.
  ///
  /// If [enable] is true, Rider Position is enabled, otherwise is disabled.
  Future<void> setRiderPosition({required bool enable}) =>
      _hostApi.setRiderPosition(enable: enable);

  /// Enable or disable Torque Barycenter.
  ///
  /// If [enable] is true, Torque Barycenter is enabled, otherwise is disabled.
  Future<void> setTorqueBarycenter({required bool enable}) =>
      _hostApi.setTorqueBarycenter(enable: enable);
}
