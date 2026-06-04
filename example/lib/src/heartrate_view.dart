import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_antplus/flutter_antplus.dart';
import 'package:flutter_antplus_example/src/device_view_state.dart';

/// The Heartrate view
class HeartrateView extends StatefulWidget {
  /// Creates a [HeartrateView].
  const HeartrateView({super.key});

  @override
  State<HeartrateView> createState() => _HeartrateViewState();
}

class _HeartrateViewState extends DeviceViewState<HeartrateView> {
  int? _bpm;
  StreamSubscription<int>? _onHeartRateDataStreamSubscription;

  @override
  void dispose() {
    unawaited(_onHeartRateDataStreamSubscription?.cancel());
    super.dispose();
  }

  @override
  Future<void> init(_) async {
    await super.init(null);
    _onHeartRateDataStreamSubscription = HeartrateViewModel
        .instance
        .onHeartRateDataStream
        .listen((bpm) => setState(() => _bpm = bpm));
  }

  @override
  Future<void> disconnect() async {
    _bpm = null;
    await super.disconnect();
  }

  @override
  List<TableRow> get tableRows => [
    tableRow('Heart Rate', _bpm, unit: 'bpm'),
  ];

  @override
  DeviceViewModel get deviceViewModel => HeartrateViewModel.instance;
}
