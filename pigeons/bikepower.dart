import 'package:pigeon/pigeon.dart';

@ConfigurePigeon(
  PigeonOptions(
    // dart
    dartOptions: DartOptions(),
    dartOut: 'lib/src/pigeons/bikepower.g.dart',
    dartPackageName: 'bikepower',
    // kotkin
    kotlinOptions: KotlinOptions(
      package: 'me.heyteacher.flutter_antplus.bikepower.pigeons',
    ),
    kotlinOut:
        'android/src/main/kotlin/me/heyteacher/flutter_antplus/bikepower/pigeons/BikepowerPigeon.g.kt',
  ),
)
@HostApi()
abstract class BikepowerHostApi {
  void startScan();
  void stopScan();
  void connect(int deviceNumber);
  void disconnect();
}
