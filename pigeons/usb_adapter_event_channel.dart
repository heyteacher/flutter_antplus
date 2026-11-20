// Copyright 2013 The Flutter Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

import 'package:pigeon/pigeon.dart';

@ConfigurePigeon(
  PigeonOptions(
    // dart
    dartOut: 'lib/src/pigeons/usb_adapter_event_channel.g.dart',
    dartOptions: DartOptions(ignoreLints: false),
    dartPackageName: 'usb_adapter',
    // kotkin
    kotlinOut:
        'android/src/main/kotlin/me/heyteacher/flutter_antplus/usb_adapter/pigeons/UsbAdapterEventChannel.g.kt',
    kotlinOptions: KotlinOptions(
      includeErrorClass: false,
      package: 'me.heyteacher.flutter_antplus.usb_adapter.pigeons',
    ),
  ),
)
enum AntplusUsbAdapterState { attached, detached, permissionNotGranted }

@EventChannelApi()
abstract class EventChannelMethods {
  AntplusUsbAdapterState onUsbAdapterStateChange();
}
