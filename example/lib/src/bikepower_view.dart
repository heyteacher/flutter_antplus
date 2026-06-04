import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_antplus/flutter_antplus.dart';
import 'package:flutter_antplus_example/src/device_view_state.dart';

/// The home page widget.
@visibleForTesting
class BikepowerView extends StatefulWidget {
  /// Creates a [BikepowerView].
  const BikepowerView({super.key});

  @override
  State<BikepowerView> createState() => _BikepowerViewState();
}

class _BikepowerViewState extends DeviceViewState<BikepowerView> {
  int? _watt;
  int? _cadence;
  int? _balance;
  PedalSmoothnessData? _pedalSmoothnessData;
  TorqueEffectivenessData? _torqueEffectivenessData;
  BatteryStatus? _batteryStatus;
  StreamSubscription<int>? _onPowerDataStreamSubscription;
  StreamSubscription<int>? _onCadenceDataStreamSubscription;
  StreamSubscription<int>? _onBalanceDataStreamSubscription;
  StreamSubscription<PedalSmoothnessData>?
  _onPedalSmoothnessDataStreamSubscription;
  StreamSubscription<TorqueEffectivenessData>?
  _onTorqueEffectivenessDataStreamSubscription;
  StreamSubscription<BatteryStatus>? _onBatteryStatusDataStreamSubscription;

  @override
  DeviceViewModel get deviceViewModel => BikepowerViewModel.instance;

  @override
  void dispose() {
    unawaited(_onPowerDataStreamSubscription?.cancel());
    unawaited(_onCadenceDataStreamSubscription?.cancel());
    unawaited(_onBalanceDataStreamSubscription?.cancel());
    unawaited(_onPedalSmoothnessDataStreamSubscription?.cancel());
    unawaited(_onTorqueEffectivenessDataStreamSubscription?.cancel());
    unawaited(_onBatteryStatusDataStreamSubscription?.cancel());
    super.dispose();
  }

  @override
  Future<void> init(_) async {
    await super.init(null);
    _onPowerDataStreamSubscription = BikepowerViewModel
        .instance
        .onPowerDataStream
        .listen(
          (watt) => setState(() => _watt = watt),
        );
    _onBalanceDataStreamSubscription = BikepowerViewModel
        .instance
        .onBalanceDataStream
        .listen(
          (balance) => setState(() => _balance = balance),
        );
    _onBatteryStatusDataStreamSubscription = BikepowerViewModel
        .instance
        .onBatteryStatusDataStream
        .listen(
          (event) => setState(() => _batteryStatus = event),
        );
    _onCadenceDataStreamSubscription = BikepowerViewModel
        .instance
        .onCadenceDataStream
        .listen(
          (event) => setState(() => _cadence = event),
        );
    _onPedalSmoothnessDataStreamSubscription = BikepowerViewModel
        .instance
        .onPedalSmoothnessDataStream
        .listen(
          (event) => setState(() => _pedalSmoothnessData = event),
        );
    _onTorqueEffectivenessDataStreamSubscription = BikepowerViewModel
        .instance
        .onTorqueEffectivenessDataStream
        .listen(
          (event) => setState(() => _torqueEffectivenessData = event),
        );
  }

  @override
  Future<void> disconnect() async {
    _watt = null;
    _balance = null;
    _cadence = null;
    _pedalSmoothnessData = null;
    _torqueEffectivenessData = null;
    _batteryStatus = null;
    await super.disconnect();
  }

  @override
  List<TableRow> get tableRows => [
    tableRow('Power', _watt, unit: 'w'),
    tableRow('Balance', _balance, unit: '%'),
    tableRow('Cadence', _cadence, unit: 'rpm'),
    tableRow(
      'Separate Pedal Smoothness Support',
      _pedalSmoothnessData?.separatePedalSmoothnessSupport,
    ),
    tableRow(
      'Left Or Combined Pedal Smoothness',
      _pedalSmoothnessData?.leftOrCombinedPedalSmoothness,
      unit: '%',
    ),
    tableRow(
      'Right Pedal Smoothness',
      _pedalSmoothnessData?.rightPedalSmoothness,
      unit: '%',
    ),
    tableRow(
      'Left Torque Effectiveness',
      _torqueEffectivenessData?.leftTorqueEffectiveness,
      unit: '%',
    ),
    tableRow(
      'Right Torque Effectiveness',
      _torqueEffectivenessData?.rightTorqueEffectiveness,
      unit: '%',
    ),
    tableRow('batteryStatus', _batteryStatus?.name),
  ];
}
