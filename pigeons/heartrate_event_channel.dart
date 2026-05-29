// Copyright 2013 The Flutter Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

import 'package:pigeon/pigeon.dart';

@ConfigurePigeon(
  PigeonOptions(
    // dart
    dartOut: 'lib/src/pigeons/heyteacher_event_channel.g.dart',
    dartOptions: DartOptions(ignoreLints: false),
    dartPackageName: 'heartrate',
    // kotkin
    kotlinOut:
        'android/src/main/kotlin/me/heyteacher/flutter_antplus/heartrate/pigeons/HeartrateEventChannel.g.kt',
    kotlinOptions: KotlinOptions(includeErrorClass: false),
  ),
)
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

@EventChannelApi()
abstract class EventChannelMethods {
  AntplusDevice onScanResult();
  int onHeartRateData();
  AntplusDeviceState onDeviceStateChange();
}
