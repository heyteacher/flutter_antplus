import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_antplus/heartrate_antplus.dart';
import 'package:flutter_heyteacher_locale/flutter_heyteacher_locale.dart';
import 'package:flutter_heyteacher_logger/flutter_heyteacher_logger.dart'
    show LoggerViewModel;
import 'package:flutter_heyteacher_views/flutter_heyteacher_views.dart';

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
  final Map<int, AntplusDevice> _scanDevices = {};
  AntplusDevice? _device;
  int? _bpm;
  AntplusDeviceState? _deviceState;
  final _heartrateAntplus = HeartrateAntplus();

  StreamSubscription<AntplusDevice>? _onScanResultStreamSubscription;
  StreamSubscription<int>? _onHeartRateDataStreamSubscription;
  StreamSubscription<AntplusDeviceState>?
  _onDeviceStateChangeStreamSubscription;
  @override
  void initState() {
    super.initState();
    unawaited(_init());
  }

  @override
  void dispose() {
    unawaited(_onScanResultStreamSubscription?.cancel());
    unawaited(_onHeartRateDataStreamSubscription?.cancel());
    unawaited(_onDeviceStateChangeStreamSubscription?.cancel());
    super.dispose();
  }

  Future<void> _init() async {
    unawaited(_onScanResultStreamSubscription?.cancel());
    _onScanResultStreamSubscription = _heartrateAntplus.onScanResultStream
        .listen(
          (device) => setState(() => _scanDevices[device.number] = device),
        );
    unawaited(_onHeartRateDataStreamSubscription?.cancel());
    _onHeartRateDataStreamSubscription = _heartrateAntplus.onHeartRateDataStream
        .listen(
          (bpm) => setState(() => _bpm = bpm),
        );
    unawaited(_onDeviceStateChangeStreamSubscription?.cancel());
    _onDeviceStateChangeStreamSubscription = _heartrateAntplus
        .onDeviceStateChangeStream
        .listen(
          (deviceState) => setState(() => _deviceState = deviceState),
        );
  }

  Future<void> _startScan() async {
    try {
      await _heartrateAntplus.startScan();
      setState(() {
        _statusMessage = 'Start scan...';
        _scanning = true;
      });
    } on PlatformException {
      setState(() => _statusMessage = 'Start scan failed');
    }
  }

  Future<void> _stopScan() async {
    try {
      await _heartrateAntplus.stopScan();
      setState(() {
        _statusMessage = 'Stop scan';
        _scanning = false;
      });
    } on PlatformException {
      setState(() => _statusMessage = 'Stop scan failed');
    }
  }

  Future<void> _connect(int deviceNumber) async {
    try {
      await _stopScan();
      await _heartrateAntplus.connect(deviceNumber);
      setState(() {
        _device = _scanDevices[deviceNumber];
        _scanDevices.remove(deviceNumber);
        _statusMessage = 'Connect device $deviceNumber...';
      });
    } on PlatformException {
      setState(() => _statusMessage = 'Connection $deviceNumber Failed');
    }
  }

  Future<void> _disconnect() async {
    try {
      await _heartrateAntplus.disconnect();
      setState(() {
        _scanDevices[_device!.number] = _device!;
        _statusMessage = 'Disconnect device ${_device!.number}';
        _device = null;
        _bpm = null;
        _deviceState = null;
      });
    } on PlatformException {
      setState(
        () => _statusMessage = 'Disconnection ${_device!.number} Failed',
      );
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: const Text('Flutter Ant+ Example'),
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
        children: [
          ..._scanDevices.values.map(
            (device) => Column(
              children: [
                ListTile(
                  title: Text(
                    '${device.number} - ${device.name}',
                  ),
                  trailing: OutlinedButton(
                    onPressed: () => _connect(device.number),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: ThemeViewModel.instance.greenColor,
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
          ),
          if (_device != null)
            Column(
              children: [
                const Text('Device Connected'),
                ListTile(
                  leading: _deviceState != null
                      ? Badge(
                          label: Text(_deviceState!.name),
                        )
                      : null,
                  title: Text(
                    '${_device!.number} - ${_device!.name}',
                  ),
                  subtitle: Text(_bpm != null ? '$_bpm bpm' : 'no data'),
                  trailing: OutlinedButton(
                    onPressed: _disconnect,
                    style: OutlinedButton.styleFrom(
                      foregroundColor: ThemeViewModel.instance.redColor,
                      side: BorderSide(
                        color: ThemeViewModel.instance.redColor,
                      ),
                    ),
                    child: const Text('Disconnect'),
                  ),
                ),
                const Divider(),
              ],
            ),
        ],
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
