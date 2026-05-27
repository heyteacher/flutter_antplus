import 'package:flutter_antplus/flutter_antplus.dart';
import 'package:flutter_antplus/flutter_antplus_method_channel.dart';
import 'package:flutter_antplus/flutter_antplus_platform_interface.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockFlutterAntplusPlatform
    with MockPlatformInterfaceMixin
    implements FlutterAntplusPlatform {
  @override
  Future<String?> startScan() => Future.value('scanning');
  @override
  Stream<Map<String, dynamic>> get onScanResultStream => const Stream.empty();

  @override
  Future<String?> connect(int deviceNumber) => Future.value('connecting...');

  @override
  Stream<Map<dynamic, dynamic>> get onHeartRateDataStream =>
      const Stream.empty();

  @override
  Future<String?> disconnect() => Future.value('disconnected');

  @override
  Future<String?> stopScan() => Future.value('stop scan');
}

void main() {
  final initialPlatform = FlutterAntplusPlatform.instance;

  test('$MethodChannelFlutterAntplus is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelFlutterAntplus>());
  });

  test('scan', () async {
    final flutterAntplusPlugin = FlutterAntplus();
    final fakePlatform = MockFlutterAntplusPlatform();
    FlutterAntplusPlatform.instance = fakePlatform;

    expect(await flutterAntplusPlugin.startScan(), '42');
  });
}
