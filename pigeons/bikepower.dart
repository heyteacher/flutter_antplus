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
  // scanning
  void startScan();
  void stopScan();
  // connection
  void connect(int deviceNumber);
  void disconnect();
  // crank length
  void requestCrankLengthInMm();
  void setCrankLengthInMm({required double crankLengthInMm});
  // advanced capabilities
  void requestAdvancedCapabilities();
  void set8Hertz({required bool enable});
  void setPowerPhase({required bool enable});
  void setPco({required bool enable});
  void setRiderPosition({required bool enable});
  void setTorqueBarycenter({required bool enable});
}
