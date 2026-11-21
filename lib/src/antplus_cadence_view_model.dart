import 'dart:async';

import 'package:flutter_antplus/flutter_antplus.dart';
import 'package:flutter_antplus/src/pigeons/cadence.g.dart';
import 'package:flutter_antplus/src/pigeons/cadence_event_channel.g.dart'
    show onCadenceData;
import 'package:flutter_antplus/src/pigeons/device_event_channel.g.dart'
    show
        AntplusDevice,
        AntplusDeviceState,
        AntplusDeviceType,
        onBatteryStatusData,
        onDeviceStateChange,
        onScanResult;

/// A view model managing the connection and data stream for an
/// ANT+ Cadence sensor.
///
/// Implements [AntplusDeviceViewModel] to handle scanning, connection,
/// and provides the [onCadenceDataStream] to read cadence values.
class AntplusCadenceViewModel implements AntplusDeviceViewModel {
  AntplusCadenceViewModel._();

  final CadenceHostApi _hostApi = CadenceHostApi();

  static AntplusCadenceViewModel? _instance;

  /// The singleton instance of [AntplusCadenceViewModel].
  // ignore: prefer_constructors_over_static_methods
  static AntplusCadenceViewModel get instance =>
      _instance ??= AntplusCadenceViewModel._();

  /// A stream of discovered ANT+ cadence devices.
  @override
  Stream<AntplusDevice> get onScanResultStream =>
      onScanResult(instanceName: AntplusDeviceType.cadence.name.toUpperCase());

  /// A stream of the connection state for the cadence device.
  @override
  Stream<AntplusDeviceState> get onDeviceStateChangeStream =>
      onDeviceStateChange(
        instanceName: AntplusDeviceType.cadence.name.toUpperCase(),
      );

  /// A stream of the battery status of the bike power sensor.
  @override
  Stream<AntplusBatteryStatus> get onBatteryStatusDataStream =>
      onBatteryStatusData(
        instanceName: AntplusDeviceType.cadence.name.toUpperCase(),
      );

  /// A stream of the real-time cadence value in revolutions per minute (rpm).
  Stream<int> get onCadenceDataStream => onCadenceData();

  /// Starts scanning for nearby ANT+ cadence sensors.
  @override
  Future<void> startScan() => _hostApi.startScan();

  /// Stops the active scan for cadence sensors.
  @override
  Future<void> stopScan() => _hostApi.stopScan();

  /// Connects to a cadence sensor with the specified [deviceNumber].
  @override
  Future<void> connect(int deviceNumber) => _hostApi.connect(deviceNumber);

  /// Disconnects from the current cadence sensor.
  @override
  Future<void> disconnect() => _hostApi.disconnect();
}
