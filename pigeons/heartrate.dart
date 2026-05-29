import 'package:pigeon/pigeon.dart';

@ConfigurePigeon(
  PigeonOptions(
    dartOut: 'lib/src/heartrate.g.dart',
    dartOptions: DartOptions(),
    javaOut:
        'android/src/main/java/me/heyteacher/flutter_antplus/heartrate/HeartratePigeon.java',
    javaOptions: JavaOptions(
      package: 'me.heyteacher.flutter_antplus.heartrate',
    ),
    dartPackageName: 'heartrate',
  ),
)
@HostApi()
abstract class HeartrateHostApi {
  void startScan();
  void stopScan();
  void connect(int deviceNumber);
  void disconnect();
}

@FlutterApi()
abstract class HeartrateFlutterApi {
  void onScanResult(AntplusDevice device);
  void onHeartRateData(int rpm);
  void onDeviceStateChange(AntplusDeviceState state);
}

enum AntplusDeviceState {
  dead,
  closed,
  searching,
  tracking,
  processingRequest,
  unrecognized,
}

class AntplusDevice {
  AntplusDevice({
    required this.number,
    required this.name,
  });

  final int number;
  final String name;
}
