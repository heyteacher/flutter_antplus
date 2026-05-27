import 'dart:async';

import 'package:flutter/services.dart';

import 'package:flutter_antplus/flutter_antplus_platform_interface.dart';
import 'package:logging/logging.dart';

/// An implementation of [FlutterAntplusPlatform] that uses method channels.
class MethodChannelFlutterAntplus extends FlutterAntplusPlatform {
  /// Constructs a [MethodChannelFlutterAntplus].
  MethodChannelFlutterAntplus() {
    _methodChannel.setMethodCallHandler(_methodCallHandler);
  }
  final _logger = Logger('MethodChannelFlutterAntplus');

  final _methodChannel = const MethodChannel('flutter_antplus');

  /// Dispose.
  void dispose() {
    _logger.finer('<dispose>:');
    _methodChannel.setMethodCallHandler(null);
    unawaited(_onScanResultController.close());
    unawaited(_onHeartRateDataController.close());
  }

  final _onScanResultController =
      StreamController<Map<dynamic, dynamic>>.broadcast();

  @override
  Stream<Map<dynamic, dynamic>> get onScanResultStream =>
      _onScanResultController.stream;

  final _onHeartRateDataController =
      StreamController<Map<dynamic, dynamic>>.broadcast();

  @override
  Stream<Map<dynamic, dynamic>> get onHeartRateDataStream =>
      _onHeartRateDataController.stream;

  @override
  Future<String?> startScan() async {
    _logger.finer('<startScan>:');
    final resultCode = await _methodChannel.invokeMethod<String>('startScan');
    _logger.finer('(startScan): resultCode $resultCode');
    return resultCode;
  }

  @override
  Future<String?> stopScan() async {
    _logger.finer('<stopScan>:');
    final resultCode = await _methodChannel.invokeMethod<String>('stopScan');
    _logger.finer('(stopScan): resultCode $resultCode');
    return resultCode;
  }

  @override
  Future<String?> connect(int deviceNumber) async {
    _logger.finer('<connect>: deviceNumber $deviceNumber.');
    final resultCode = await _methodChannel.invokeMethod<String>('connect', {
      'deviceNumber': deviceNumber,
    });
    _logger.finer(
      '(connect): deviceNumber $deviceNumber. resultCode $resultCode',
    );
    return resultCode;
  }

  @override
  Future<String?> disconnect() async {
    _logger.finer('<disconnect>:');
    final resultCode = await _methodChannel.invokeMethod<String>('disconnect');
    _logger.finer(
      '(disconnect): resultCode $resultCode',
    );
    return resultCode;
  }

  Future<void> _methodCallHandler(MethodCall call) async {
    _logger.finer('<_methodCallHandler>: call.method ${call.method}');
    // handle method call
    switch (call.method) {
      case 'onScanResult':
        return _onScanResultController.add(
          call.arguments as Map<dynamic, dynamic>,
        );
      case 'onHeartRateData':
        return _onHeartRateDataController.add(
          call.arguments as Map<dynamic, dynamic>,
        );
    }
  }
}
