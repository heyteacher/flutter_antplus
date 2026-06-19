import 'dart:async';

import 'package:flutter_antplus/src/pigeons/logging_event_channel.g.dart'
    show AntplusLogData, AntplusLogEvent, onLogData;
import 'package:logging/logging.dart';

export 'src/pigeons/logging_event_channel.g.dart'
    show AntplusLogData, AntplusLogEvent;

/// The flutter Ant+ plugin
class AntplusLogging {
  AntplusLogging._();

  StreamSubscription<AntplusLogData>? _onLogDataStreamSubscription;

  static AntplusLogging? _instance;

  /// The singleton instance of [AntplusLogging].
  // ignore: prefer_constructors_over_static_methods
  static AntplusLogging get instance => _instance ??= AntplusLogging._();

  /// Initialize log data stream listener
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

  /// dispose object
  void dispose() {
    unawaited(_onLogDataStreamSubscription?.cancel());
  }
}
