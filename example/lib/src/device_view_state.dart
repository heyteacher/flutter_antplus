import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_antplus/flutter_antplus.dart';
import 'package:flutter_heyteacher_views/flutter_heyteacher_views.dart';

/// An abstract base state class for managing scanning and connection states
/// across different ANT+ device views.
///
/// Coordinates starting/stopping scans, selecting/connecting to devices,
/// tracking connection/subscription states, and rendering shared UI structures
/// like device status badges and data tables.
abstract class DeviceViewState<T extends StatefulWidget> extends State<T>
    with AutomaticKeepAliveClientMixin<T> {
  final Map<int, AntplusDevice> _scanDevices = {};
  AntplusDeviceState? _deviceState;
  AntplusDevice? _connectedDevice;
  bool _scanning = false;

  StreamSubscription<AntplusDevice>? _onScanResultStreamSubscription;
  StreamSubscription<AntplusDeviceState>?
  _onDeviceStateChangeStreamSubscription;
  StreamSubscription<AntplusRequestAccessResult>?
  _onRequestAccessResultStreamSubscription;

  /// Initialize state
  @protected
  AntplusDeviceViewModel get deviceViewModel;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.scheduleFrameCallback(init);
  }

  /// Initialize state
  Future<void> init(dynamic _) async {
    _onScanResultStreamSubscription = deviceViewModel.onScanResultStream.listen(
      (event) => setState(() => _scanDevices[event.number] = event),
    );
    _onRequestAccessResultStreamSubscription = deviceViewModel
        .onRequestAccessResultStream
        .listen(
          (requestAccessResult) =>
              mounted &&
                  requestAccessResult !=
                      AntplusRequestAccessResult.userCancelled
              ? showSnackBar(
                  context: context,
                  message: requestAccessResult.name,
                  error:
                      requestAccessResult != AntplusRequestAccessResult.success,
                  persist:
                      requestAccessResult != AntplusRequestAccessResult.success,
                )
              : null,
        );
    _onDeviceStateChangeStreamSubscription = deviceViewModel
        .onDeviceStateChangeStream
        .listen((event) => setState(() => _deviceState = event));
  }

  @override
  void dispose() {
    unawaited(_onScanResultStreamSubscription?.cancel());
    unawaited(_onDeviceStateChangeStreamSubscription?.cancel());
    unawaited(_onRequestAccessResultStreamSubscription?.cancel());
    super.dispose();
  }

  /// Starts scan.
  Future<void> startScan() async {
    await deviceViewModel.startScan();
    setState(() {
      _scanning = true;
    });
  }

  /// Stop scan.
  Future<void> stopScan() async {
    await deviceViewModel.stopScan();
    setState(() {
      _scanning = false;
    });
  }

  /// Connect to device with [deviceNumber].
  Future<void> connect(int deviceNumber) async {
    await stopScan();
    await deviceViewModel.connect(deviceNumber);
    setState(() {
      _connectedDevice = _scanDevices[deviceNumber];
      _scanDevices.remove(deviceNumber);
    });
  }

  /// Disconnect to device .
  Future<void> disconnect() async {
    await deviceViewModel.disconnect();
    setState(() {
      _scanDevices[_connectedDevice!.number] = _connectedDevice!;
      _connectedDevice = null;
      _deviceState = null;
    });
  }

  /// The table rows
  List<TableRow> get tableRows;

  /// Creates a table with [tableRows].
  @protected
  Table table({required List<TableRow> tableRows}) => Table(
    border: TableBorder.all(color: Colors.white24),
    defaultVerticalAlignment: TableCellVerticalAlignment.middle,

    columnWidths: const {
      0: FlexColumnWidth(2),
      1: FlexColumnWidth(),
    },
    children: tableRows,
  );

  /// Creates a table row with [label] and [value] follower by [unit].
  @protected
  TableRow tableRow(
    String label,
    dynamic value, {
    String unit = '',
  }) => TableRow(
    children: [
      Text(
        label,
        textAlign: TextAlign.center,
        style: Theme.of(context).textTheme.bodyMedium,
      ),
      Text(
        '${value ?? '-'} $unit'.trim(),
        textAlign: TextAlign.center,
        style: Theme.of(
          context,
        ).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.bold),
      ),
    ],
  );

  /// Creates a button with [onPressed], [label] and [color].
  @protected
  OutlinedButton button({
    required VoidCallback onPressed,
    required String label,
    required Color color,
  }) => OutlinedButton(
    onPressed: onPressed,
    style: OutlinedButton.styleFrom(
      foregroundColor: color,
      side: BorderSide(
        color: color,
      ),
      textStyle: Theme.of(
        context,
      ).textTheme.labelSmall,
      padding: const EdgeInsets.all(2),
      tapTargetSize: MaterialTapTargetSize.shrinkWrap,
      visualDensity: VisualDensity.compact,
    ),
    child: Padding(
      padding: const EdgeInsets.symmetric(horizontal: 4),
      child: Text(label),
    ),
  );

  /// Creates a message bottom sheet with [statusMessage].
  @protected
  Column messageBottomSheet(String? statusMessage) => Column(
    mainAxisSize: MainAxisSize.min,
    children: [
      const Divider(),
      Padding(
        padding: const EdgeInsets.all(8),
        child: Text(statusMessage ?? ''),
      ),
    ],
  );

  /// Creates a device status badge with [deviceState].
  @protected
  Badge? deviceStatusBadge(String? deviceState) => switch (deviceState) {
    null => null,
    'tracking' => Badge(
      label: BlinkingText(
        deviceState,
        style: TextStyle(
          fontWeight: FontWeight.bold,
          color: ThemeViewModel.instance.colorScheme.onPrimary,
        ),
      ),
      backgroundColor: ThemeViewModel.instance.greenColor,
    ),
    _ => Badge(
      label: AnimateText(deviceState),
      textStyle: TextStyle(
        fontWeight: FontWeight.bold,
        color: ThemeViewModel.instance.colorScheme.onPrimary,
      ),
      backgroundColor:
          deviceState == 'processingRequest' || deviceState == 'searching'
          ? ThemeViewModel.instance.orangeColor
          : ThemeViewModel.instance.redColor,
    ),
  };

  @override
  Widget build(BuildContext context) {
    super.build(context);
    return Scaffold(
      floatingActionButtonLocation: FloatingActionButtonLocation.centerFloat,
      floatingActionButton: Padding(
        padding: const EdgeInsets.only(bottom: 40),
        child: FloatingActionTextIconButtom(
          text: 'Scan',
          backgroundColor: _scanning
              ? ThemeViewModel.instance.redColor
              : ThemeViewModel.instance.greenColor,
          iconData: _scanning ? Icons.stop_circle : Icons.play_circle,
          onPressed: _scanning ? stopScan : startScan,
        ),
      ),
      body: ListView(
        children: [
          ..._scanDevices.values.map(
            (device) => Column(
              children: [
                ListTile(
                  leading: deviceStatusBadge(_deviceState?.name),
                  title: AnimateText('${device.number} - ${device.name}'),
                  trailing: button(
                    onPressed: () => connect(device.number),
                    label: 'Connect',
                    color: ThemeViewModel.instance.greenColor,
                  ),
                ),
                const Divider(),
              ],
            ),
          ),
          if (_connectedDevice != null)
            Column(
              children: [
                ColoredBox(
                  color: ThemeViewModel.instance.colorScheme.surfaceContainer,
                  child: ListTile(
                    leading: deviceStatusBadge(_deviceState?.name),
                    title: AnimateText(
                      '${_connectedDevice!.number} - ${_connectedDevice!.name}',
                    ),
                    trailing: button(
                      onPressed: disconnect,
                      label: 'Disconnect',
                      color: ThemeViewModel.instance.redColor,
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.only(top: 16),
                  child: table(
                    tableRows: tableRows,
                  ),
                ),
              ],
            ),
        ],
      ),
    );
  }

  @override
  bool get wantKeepAlive => true;
}
