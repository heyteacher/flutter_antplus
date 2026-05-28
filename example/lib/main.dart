import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_antplus/flutter_antplus.dart';
import 'package:flutter_heyteacher_locale/flutter_heyteacher_locale.dart';
import 'package:flutter_heyteacher_logger/flutter_heyteacher_logger.dart'
    show LoggerViewModel;
import 'package:flutter_heyteacher_views/flutter_heyteacher_views.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart'
    show FaIcon, FontAwesomeIcons;

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await LocaleViewModel.instance.initLocale();
  await LoggerViewModel.instance.initialize();
  await ThemeViewModel.instance.setThemeMode(ThemeMode.dark);
  runApp(const _MyApp());
}

class _MyApp extends StatelessWidget {
  /// Creates the [_MyApp].
  const _MyApp();

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) => StreamBuilder(
    stream: ThemeViewModel.instance.themeStream,
    builder: (context, asyncSnapshot) => MaterialApp(
      theme: ThemeViewModel.instance.lightTheme,
      darkTheme: ThemeViewModel.instance.darkTheme,
      themeMode: asyncSnapshot.data?.themeMode ?? ThemeMode.system,
      home: const HomePage(),
    ),
  );
}

/// The home page widget.
@visibleForTesting
class HomePage extends StatefulWidget {
  /// Creates a [HomePage].
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  String? _statusMessage;
  bool _scanning = false;
  final Map<dynamic, Map<dynamic, dynamic>> _devices = {};
  Map<dynamic, dynamic> _heartRateData = {};
  final _flutterAntplusPlugin = FlutterAntplus();

  StreamSubscription<Map<dynamic, dynamic>>? _onScanResultStreamSubscription;

  @override
  void initState() {
    super.initState();
    unawaited(_init());
  }

  @override
  void dispose() {
    unawaited(_onScanResultStreamSubscription?.cancel());
    super.dispose();
  }

  Future<void> _init() async {
    unawaited(_onScanResultStreamSubscription?.cancel());
    _onScanResultStreamSubscription = _flutterAntplusPlugin.onScanResultStream
        .listen(
          (device) => setState(() => _devices[device['deviceNumber']] = device),
        );
    _flutterAntplusPlugin.onHeartRateDataStream.listen(
      (heartRateData) => setState(() => _heartRateData = heartRateData),
    );
  }

  Future<void> _startScan() async {
    try {
      _statusMessage = await _flutterAntplusPlugin.startScan();
      _scanning = true;
    } on PlatformException {
      _statusMessage = 'Start scan failed';
    }
    setState(() {});
  }

  Future<void> _stopScan() async {
    try {
      _statusMessage = await _flutterAntplusPlugin.stopScan();
      _scanning = false;
    } on PlatformException {
      _statusMessage = 'Start scan failed';
    }
    setState(() {});
  }

  Future<void> _connect(int deviceNumber) async {
    try {
      _statusMessage = await _flutterAntplusPlugin.connect(deviceNumber);
    } on PlatformException {
      _statusMessage = 'Connection to $deviceNumber Failed';
    }
    setState(() {});
  }

  Future<void> _disconnect(int deviceNumber) async {
    try {
      _statusMessage = await _flutterAntplusPlugin.disconnect();
      _heartRateData = {};
    } on PlatformException {
      _statusMessage = 'Disconnection $deviceNumber Failed';
    }
    setState(() {});
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: const Text('Flutter Ant+ Example'),
      actions: const [ThemeModeButton()],
    ),
    floatingActionButtonLocation: FloatingActionButtonLocation.centerFloat,
    floatingActionButton: Padding(
      padding: const EdgeInsets.only(bottom: 40),
      child: FloatingActionTextIconButtom(
        text: 'Scan',
        backgroundColor: _scanning
            ? ThemeViewModel.instance.redColor
            : ThemeViewModel.instance.greenColor,
        iconData: _scanning ? Icons.stop_circle : Icons.play_circle,
        onPressed: _scanning ? _stopScan : _startScan,
      ),
    ),
    body: Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8),
      child: ListView(
        children: _devices.values
            .map(
              (device) => Column(
                children: [
                  ListTile(
                    leading: const FaIcon(FontAwesomeIcons.heartPulse),
                    title: Text(
                      '${device['deviceNumber']} ${device['displayName']}',
                    ),
                    subtitle: Text(
                      device['alreadyConnected'] == true &&
                              _heartRateData['heartRate'] != null
                          ? "${_heartRateData['heartRate']} bpm"
                          : 'no data',
                    ),
                    trailing: device['alreadyConnected'] == true
                        ? OutlinedButton(
                            onPressed: () =>
                                _disconnect(device['deviceNumber'] as int),
                            style: OutlinedButton.styleFrom(
                              foregroundColor: ThemeViewModel.instance.redColor,
                              side: BorderSide(
                                color: ThemeViewModel.instance.redColor,
                              ),
                            ),
                            child: const Text('Disconnect'),
                          )
                        : OutlinedButton(
                            onPressed: () =>
                                _connect(device['deviceNumber'] as int),
                            style: OutlinedButton.styleFrom(
                              foregroundColor:
                                  ThemeViewModel.instance.greenColor,
                              side: BorderSide(
                                color: ThemeViewModel.instance.greenColor,
                              ),
                            ),
                            child: const Text('Connect'),
                          ),
                  ),
                  const Divider(),
                ],
              ),
            )
            .toList(),
      ),
    ),
    bottomSheet: Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        const Divider(),
        Padding(
          padding: const EdgeInsets.all(8),
          child: Text(_statusMessage ?? ''),
        ),
      ],
    ),
  );
}
