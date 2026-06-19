// Copyright 2013 The Flutter Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

import 'package:pigeon/pigeon.dart';

@ConfigurePigeon(
  PigeonOptions(
    // dart
    dartOut: 'lib/src/pigeons/device_event_channel.g.dart',
    dartOptions: DartOptions(ignoreLints: false),
    dartPackageName: 'device',
    // kotkin
    kotlinOut:
        'android/src/main/kotlin/me/heyteacher/flutter_antplus/device/pigeons/DeviceEventChannel.g.kt',
    kotlinOptions: KotlinOptions(
      includeErrorClass: false,
      package: 'me.heyteacher.flutter_antplus.device.pigeons',
    ),
  ),
)
enum AntplusDeviceType {
  heartrate,
  bikepower,
  cadence,
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
    required this.type,
  });

  final int number;
  final String name;
  final AntplusDeviceType type;
}

enum AntplusRequestAccessResult {
  success,
  userCancelled,
  channelNotAvailable,
  otherFailure,
  dependencyNotInstalled,
  deviceAlreadyInUse,
  searchTimeout,
  alreadySubscribed,
  badParams,
  adapterNotDetected,
  unrecognized,
}

@EventChannelApi()
abstract class EventChannelMethods {
  AntplusDevice onScanResult();
  AntplusRequestAccessResult onRequestAccessResult();
  AntplusDeviceState onDeviceStateChange();
}
