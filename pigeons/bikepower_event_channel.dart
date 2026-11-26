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
class AntplusPedalSmoothnessData {
  const AntplusPedalSmoothnessData({
    required this.separatePedalSmoothnessSupport,
    required this.leftOrCombinedPedalSmoothness,
    required this.rightPedalSmoothness,
  });

  final bool separatePedalSmoothnessSupport;
  final double? leftOrCombinedPedalSmoothness;
  final double rightPedalSmoothness;
}

class AntplusTorqueEffectivenessData {
  const AntplusTorqueEffectivenessData({
    required this.leftTorqueEffectiveness,
    required this.rightTorqueEffectiveness,
  });

  final double? leftTorqueEffectiveness;
  final double rightTorqueEffectiveness;
}

class AntplusAdvancedCapabilitiesData {
  const AntplusAdvancedCapabilitiesData({
    required this.is4HertzSupported,
    required this.is4HertzEnabled,
    required this.is8HertzSupported,
    required this.is8HertzEnabled,
    required this.powerPhaseSupported,
    required this.powerPhaseEnabled,
    required this.pcoSupported,
    required this.pcoEnabled,
    required this.riderPositionSupported,
    required this.riderPositionEnabled,
    required this.torqueBarycenterSupported,
    required this.torqueBarycenterEnabled,
  });

  final bool is4HertzSupported;
  final bool is4HertzEnabled;
  final bool is8HertzSupported;
  final bool is8HertzEnabled;
  final bool powerPhaseSupported;
  final bool powerPhaseEnabled;
  final bool pcoSupported;
  final bool pcoEnabled;
  final bool riderPositionSupported;
  final bool riderPositionEnabled;
  final bool torqueBarycenterSupported;
  final bool torqueBarycenterEnabled;
}

@EventChannelApi()
abstract class EventChannelMethods {
  int onPowerData();
  int onCadenceData();
  int onBalanceData();
  AntplusPedalSmoothnessData onPedalSmoothnessData();
  AntplusTorqueEffectivenessData onTorqueEffectivenessData();
  double onCrankLengthInMm();
  AntplusAdvancedCapabilitiesData onAdvancedCapabilitiesData();
}
