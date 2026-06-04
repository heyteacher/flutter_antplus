// Copyright 2013 The Flutter Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

import 'package:pigeon/pigeon.dart';

@ConfigurePigeon(
  PigeonOptions(
    // dart
    dartOut: 'lib/src/pigeons/bikepower_event_channel.g.dart',
    dartOptions: DartOptions(ignoreLints: false),
    dartPackageName: 'bikepower',
    // kotkin
    kotlinOut:
        'android/src/main/kotlin/me/heyteacher/flutter_antplus/bikepower/pigeons/BikepowerEventChannel.g.kt',
    kotlinOptions: KotlinOptions(
      includeErrorClass: false,
      package: 'me.heyteacher.flutter_antplus.bikepower.pigeons',
    ),
  ),
)
enum DataSource {
  powerOnlyData,
  wheelTorqueData,
  crankTorqueData,
  ctfData,
  coastOrStopDetected,
  initialValuePowerOnlyData,
  initialValueWheelTorqueData,
  initialValueCrankTorqueData,
  initialValueCtfData,
  invalid,
  invalidCtfCalReq,
  unrecognized,
}

class PedalSmoothnessData {
  const PedalSmoothnessData({
    required this.separatePedalSmoothnessSupport,
    required this.leftOrCombinedPedalSmoothness,
    required this.rightPedalSmoothness,
  });

  final bool separatePedalSmoothnessSupport;
  final double? leftOrCombinedPedalSmoothness;
  final double rightPedalSmoothness;
}

class TorqueEffectivenessData {
  const TorqueEffectivenessData({
    required this.leftTorqueEffectiveness,
    required this.rightTorqueEffectiveness,
  });

  final double? leftTorqueEffectiveness;
  final double rightTorqueEffectiveness;
}

enum BatteryStatus { newStatus, good, ok, low, critical, invalid, unrecognized }

@EventChannelApi()
abstract class EventChannelMethods {
  int onPowerData();
  int onCadenceData();
  int onBalanceData();
  PedalSmoothnessData onPedalSmoothnessData();
  TorqueEffectivenessData onTorqueEffectivenessData();
  BatteryStatus onBatteryStatusData();
}
