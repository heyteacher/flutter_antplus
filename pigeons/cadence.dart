import 'package:pigeon/pigeon.dart';

@ConfigurePigeon(
  PigeonOptions(
    // dart
    dartOptions: DartOptions(),
    dartOut: 'lib/src/pigeons/cadence.g.dart',
    dartPackageName: 'cadence',
    // kotkin
    kotlinOptions: KotlinOptions(
      package: 'me.heyteacher.flutter_antplus.cadence.pigeons',
    ),
    kotlinOut:
        'android/src/main/kotlin/me/heyteacher/flutter_antplus/cadence/pigeons/CadencePigeon.g.kt',
  ),
)
@HostApi()
abstract class CadenceHostApi {
  void startScan();
  void stopScan();
  void connect(int deviceNumber);
  void disconnect();
}
