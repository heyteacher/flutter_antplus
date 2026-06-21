import 'dart:async' show unawaited;

import 'package:flutter/material.dart';
import 'package:flutter_antplus/antplus_logging.dart';
import 'package:flutter_antplus_example/src/app.dart';
import 'package:flutter_heyteacher_logger/flutter_heyteacher_logger.dart'
    show LoggerViewModel;
import 'package:wakelock_plus/wakelock_plus.dart' show WakelockPlus;

/// The main entry point for the ANT+ Flutter plugin example application.
///
/// Initializes dependencies, hooks up the native logging listeners, prevents
/// screen sleep (wakelock) during sensor streaming, and mounts the root
/// app widget.
void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await LoggerViewModel.instance.initialize();
  AntplusLogging.instance.initLog();
  unawaited(WakelockPlus.enable());
  runApp(const App());
}
