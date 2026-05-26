import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_antplus/flutter_antplus.dart';

void main() {
  runApp(const MyApp());
}

/// My application widget.
class MyApp extends StatefulWidget {
  /// Creates a [MyApp].
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _scanResult = 'Unknown';
  final _flutterAntplusPlugin = FlutterAntplus();

  @override
  void initState() {
    super.initState();
    unawaited(_init());
  }

  Future<void> _init() async {
    String scanResult;
    // Platform messages may fail, so we use a try/catch PlatformException.
    // We also handle the message potentially returning null.
    try {
      scanResult = await _flutterAntplusPlugin.scan() ?? 'Waiting...';
    } on PlatformException {
      scanResult = 'Failed';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _scanResult = scanResult;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('Plugin example app')),
        body: Center(child: Text('Scan Result: $_scanResult\n')),
      ),
    );
  }
}
