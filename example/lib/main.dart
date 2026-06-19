import 'package:flutter/material.dart';
import 'package:flutter_antplus/antplus_logging.dart';
import 'package:flutter_antplus_example/src/app.dart';
import 'package:flutter_heyteacher_logger/flutter_heyteacher_logger.dart'
    show LoggerViewModel;

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await LoggerViewModel.instance.initialize();
  AntplusLogging.instance.initLog();
  runApp(const App());
}
