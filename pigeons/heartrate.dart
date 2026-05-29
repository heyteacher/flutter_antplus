import 'package:pigeon/pigeon.dart';

@ConfigurePigeon(
  PigeonOptions(
    // dart
    dartOptions: DartOptions(),
    dartOut: 'lib/src/pigeons/heartrate.g.dart',
    dartPackageName: 'heartrate',
    // kotkin
    kotlinOptions: KotlinOptions(
      package: 'me.heyteacher.flutter_antplus.heartrate.pigeons',
    ),
    kotlinOut:
        'android/src/main/kotlin/me/heyteacher/flutter_antplus/heartrate/pigeons/HeartratePigeon.g.kt',
  ),
)
@HostApi()
abstract class HeartrateHostApi {
  void startScan();
  void stopScan();
  void connect(int deviceNumber);
  void disconnect();
}
