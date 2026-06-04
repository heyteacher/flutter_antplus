import 'dart:async';

import 'package:flutter_antplus/src/pigeons/logging_event_channel.g.dart'
    show LogData, LogEvent, onLogData;
import 'package:logging/logging.dart';

export 'src/pigeons/logging_event_channel.g.dart' show LogData, LogEvent;

/// The flutter Ant+ plugin
class LoggingAntplus {
  LoggingAntplus._();

  StreamSubscription<LogData>? _onLogDataStreamSubscription;

  static LoggingAntplus? _instance;

  /// The singleton instance of [LoggingAntplus].
  // ignore: prefer_constructors_over_static_methods
  static LoggingAntplus get instance => _instance ??= LoggingAntplus._();

  /// Initialize log data stream listener
  void initLog() {
    unawaited(_onLogDataStreamSubscription?.cancel());
    _onLogDataStreamSubscription = onLogData().listen(
      (event) => switch (event.level) {
        LogEvent.verbose => Logger(event.tag).finest(event.message),
        LogEvent.debug => Logger(event.tag).finer(event.message),
        LogEvent.info => Logger(event.tag).info(event.message),
        LogEvent.warning => Logger(event.tag).warning(event.message),
        LogEvent.error => Logger(event.tag).severe(event.message),
      },
    );
  }

  /// dispose object
  void dispose() {
    unawaited(_onLogDataStreamSubscription?.cancel());
  }
}
