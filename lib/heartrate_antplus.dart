import 'dart:async';

import 'package:flutter_antplus/src/pigeons/heartrate.g.dart';
import 'package:flutter_antplus/src/pigeons/heyteacher_event_channel.g.dart'
    show
        AntplusDevice,
        AntplusDeviceState,
        onDeviceStateChange,
        onHeartRateData,
        onScanResult;

export 'src/pigeons/heyteacher_event_channel.g.dart'
    show AntplusDevice, AntplusDeviceState;

/// The flutter Ant+ plugin
class HeartrateAntplus {
  final HeartrateHostApi _hostApi = HeartrateHostApi();

  /// on scan result stream
  Stream<AntplusDevice> get onScanResultStream => onScanResult.call();

  /// on data stream
  Stream<int> get onHeartRateDataStream => onHeartRateData.call();

  /// on device state change stream
  Stream<AntplusDeviceState> get onDeviceStateChangeStream =>
      onDeviceStateChange();

  /// Starts scan.
  Future<void> startScan() => _hostApi.startScan();

  /// Stop scan .
  Future<void> stopScan() => _hostApi.stopScan();

  /// Connect to device with [deviceNumber].
  Future<void> connect(int deviceNumber) => _hostApi.connect(deviceNumber);

  /// Disconnect to device .
  Future<void> disconnect() => _hostApi.disconnect();
}
