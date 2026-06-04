// Copyright 2013 The Flutter Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

import 'package:pigeon/pigeon.dart';

@ConfigurePigeon(
  PigeonOptions(
    // dart
    dartOut: 'lib/src/pigeons/logging_event_channel.g.dart',
    dartOptions: DartOptions(ignoreLints: false),
    dartPackageName: 'logging',
    // kotkin
    kotlinOut:
        'android/src/main/kotlin/me/heyteacher/flutter_antplus/logging/pigeons/LoggingEventChannel.g.kt',
    kotlinOptions: KotlinOptions(
      includeErrorClass: false,
      package: 'me.heyteacher.flutter_antplus.logging.pigeons',
    ),
  ),
)
enum LogEvent { verbose, debug, info, warning, error }

class LogData {
  const LogData({
    required this.level,
    required this.tag,
    required this.message,
  });
  final LogEvent level;
  final String tag;
  final String message;
}

@EventChannelApi()
//
// ignore: one_member_abstracts
abstract class EventChannelMethods {
  LogData onLogData();
}
