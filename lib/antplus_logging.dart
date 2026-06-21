import 'dart:async';

import 'package:flutter_antplus/src/pigeons/logging_event_channel.g.dart'
    show AntplusLogData, AntplusLogEvent, onLogData;
import 'package:logging/logging.dart';

export 'src/pigeons/logging_event_channel.g.dart'
    show AntplusLogData, AntplusLogEvent;

/// A utility class for handling logging within the ANT+ plugin.
///
/// This class exposes a singleton instance that listens to log data events
/// emitted by the native ANT+ library side and forwards them to the Dart
/// `logging` package.
class AntplusLogging {
  AntplusLogging._();

  StreamSubscription<AntplusLogData>? _onLogDataStreamSubscription;

  static AntplusLogging? _instance;

  /// The singleton instance of [AntplusLogging].
  // ignore: prefer_constructors_over_static_methods
  static AntplusLogging get instance => _instance ??= AntplusLogging._();

  /// Initializes the log listener.
  ///
  /// Listens to native ANT+ plugin logs and forwards them to a standard
  /// [Logger] instance matching the native log tag, mapped to the
  /// corresponding Dart [Logger] level:
  /// - verbose -> finest
  /// - debug -> finer
  /// - info -> info
  /// - warning -> warning
  /// - error -> severe
  void initLog() {
    unawaited(_onLogDataStreamSubscription?.cancel());
    _onLogDataStreamSubscription = onLogData().listen(
      (event) => switch (event.level) {
        AntplusLogEvent.verbose => Logger(event.tag).finest(event.message),
        AntplusLogEvent.debug => Logger(event.tag).finer(event.message),
        AntplusLogEvent.info => Logger(event.tag).info(event.message),
        AntplusLogEvent.warning => Logger(event.tag).warning(event.message),
        AntplusLogEvent.error => Logger(event.tag).severe(event.message),
      },
    );
  }

  /// Disposes of the logging subscription.
  ///
  /// Cancels the subscription to native log events.
  void dispose() {
    unawaited(_onLogDataStreamSubscription?.cancel());
  }
}
