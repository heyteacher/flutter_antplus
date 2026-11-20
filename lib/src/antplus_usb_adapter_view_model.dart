import 'dart:async';

import 'package:flutter_antplus/src/pigeons/usb_adapter_event_channel.g.dart';

/// An abstract interface representing a common view model for managing
/// ANT+ adapter.
///
/// Provides a streams adapter state changes (attached, detached, or permission
/// not granted)
class AntplusAdapterViewModel {
  AntplusAdapterViewModel._();

  static AntplusAdapterViewModel? _instance;

  /// The singleton instance of [AntplusAdapterViewModel].
  // ignore: prefer_constructors_over_static_methods
  static AntplusAdapterViewModel get instance =>
      _instance ??= AntplusAdapterViewModel._();

  /// A stream of access request results.
  ///
  /// Emits [AntplusUsbAdapterState] which indicates whether access
  /// to the ANT+ channel was granted or failed (e.g. user cancelled,
  /// already in use, etc.).
  Stream<AntplusUsbAdapterState> get usbAdapterStateChangeStream =>
      onUsbAdapterStateChange();
}
