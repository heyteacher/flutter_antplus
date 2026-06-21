import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_antplus/flutter_antplus.dart';
import 'package:flutter_antplus_example/src/device_view_state.dart';

/// A widget that displays real-time cadence data from an ANT+ Cadence sensor.
///
/// Subscribes to the cadence telemetry stream (rpm) and renders the current
/// value in a structured table layout.
class CadenceView extends StatefulWidget {
  /// Creates a [CadenceView].
  const CadenceView({super.key});

  @override
  State<CadenceView> createState() => _CadenceViewState();
}

class _CadenceViewState extends DeviceViewState<CadenceView> {
  int? _rpm;
  StreamSubscription<int>? _onCadenceDataStreamSubscription;

  @override
  void dispose() {
    unawaited(_onCadenceDataStreamSubscription?.cancel());
    super.dispose();
  }

  @override
  Future<void> init(_) async {
    await super.init(null);
    _onCadenceDataStreamSubscription = AntplusCadenceViewModel
        .instance
        .onCadenceDataStream
        .listen((rpm) => setState(() => _rpm = rpm));
  }

  @override
  Future<void> disconnect() async {
    _rpm = null;
    await super.disconnect();
  }

  @override
  List<TableRow> get tableRows => [
    tableRow('Cadence', _rpm, unit: 'rpm'),
  ];

  @override
  AntplusDeviceViewModel get deviceViewModel =>
      AntplusCadenceViewModel.instance;
}
