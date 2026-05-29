import 'dart:async';

import 'package:flutter_antplus/src/heartrate.g.dart';

export 'src/heartrate.g.dart' show AntplusDevice, AntplusDeviceState;

/// The flutter Ant+ plugin
class HeartrateAntplus implements HeartrateFlutterApi {
  /// Creates [HeartrateAntplus]
  HeartrateAntplus() {
    HeartrateFlutterApi.setUp(this);
  }

  final HeartrateHostApi _hostApi = HeartrateHostApi();

  final _onScanResultController = StreamController<AntplusDevice>.broadcast();

  /// on scan result stream
  Stream<AntplusDevice> get onScanResultStream =>
      _onScanResultController.stream;

  final _onHeartRateDataController = StreamController<int>.broadcast();

  /// on data stream
  Stream<int> get onHeartRateDataStream => _onHeartRateDataController.stream;

  final _onDeviceStateChangeController =
      StreamController<AntplusDeviceState>.broadcast();

  /// on device state change stream
  Stream<AntplusDeviceState> get onDeviceStateChangeStream =>
      _onDeviceStateChangeController.stream;

  /// Dispose.
  void dispose() {
    unawaited(_onScanResultController.close());
    unawaited(_onHeartRateDataController.close());
    unawaited(_onDeviceStateChangeController.close());
  }

  /// Starts scan.
  Future<void> startScan() => _hostApi.startScan();

  /// Stop scan .
  Future<void> stopScan() => _hostApi.stopScan();

  /// Connect to device with [deviceNumber].
  Future<void> connect(int deviceNumber) => _hostApi.connect(deviceNumber);

  /// Disconnect to device .
  Future<void> disconnect() => _hostApi.disconnect();

  @override
  void onHeartRateData(int rpm) {
    _onHeartRateDataController.sink.add(rpm);
  }

  @override
  void onScanResult(AntplusDevice device) {
    _onScanResultController.sink.add(device);
  }

  @override
  void onDeviceStateChange(AntplusDeviceState state) {
    _onDeviceStateChangeController.sink.add(state);
  }
}
