import 'package:flutter_antplus/flutter_antplus.dart';
import 'package:flutter_antplus/flutter_antplus_method_channel.dart';
import 'package:flutter_antplus/flutter_antplus_platform_interface.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockFlutterAntplusPlatform
    with MockPlatformInterfaceMixin
    implements FlutterAntplusPlatform {
  @override
  Future<String?> scan() => Future.value('42');
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

    expect(await flutterAntplusPlugin.scan(), '42');
  });
}
