import 'dart:async';

import 'package:flutter_antplus/src/pigeons/device_event_channel.g.dart';

/// An abstract interface representing a common view model for managing
/// ANT+ devices.
///
/// Provides methods to scan, connect, and disconnect, as well as streams
/// to monitor discovery, access requests, and connection states.
class AntplusRequestAccessViewModel {
  AntplusRequestAccessViewModel._();

  static AntplusRequestAccessViewModel? _instance;

  /// The singleton instance of [AntplusRequestAccessViewModel].
  // ignore: prefer_constructors_over_static_methods
  static AntplusRequestAccessViewModel get instance =>
      _instance ??= AntplusRequestAccessViewModel._();

  /// A stream of access request results.
  ///
  /// Emits [AntplusRequestAccessResult] which indicates whether access
  /// to the ANT+ channel was granted or failed (e.g. user cancelled,
  /// already in use, etc.).
  Stream<AntplusRequestAccessResult> get onRequestAccessStream =>
      onRequestAccess();
}
