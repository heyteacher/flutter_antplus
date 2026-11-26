import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_antplus/flutter_antplus.dart';
import 'package:flutter_heyteacher_locale/flutter_heyteacher_locale.dart'
    show FormatterHelper;
import 'package:flutter_heyteacher_views/flutter_heyteacher_views.dart';

/// Shows the Ant+ Bike Power Settings:
///
/// - Crank length (mm)
/// - Advanced capabilities:
///   - 4Hz
///   - 8Hz
///   - PCO
///   - Power
///   - Rider Position
///   - Torque Barycenter
///
/// Each advanced capability can be enabled or disabled if it is supported.
///
/// Two callbacks can be provided:
///
/// - [_crankLengthCallback]: callback invokend whenthe crank length is set
/// - [_is8hertzCallback]: callback invokend when the 8Hz is set
///
class AntPlusBikePowerSettings extends StatefulWidget {
  /// Creates [AntPlusBikePowerSettings]
  ///
  /// Two callbacks can be provided:
  ///
  /// - [_crankLengthCallback] callback invokend whenthe crank length is set
  /// - [_is8hertzCallback] callback invokend when the 8Hz is set
  const AntPlusBikePowerSettings({
    super.key,
    this._crankLengthCallback,
    this._is8hertzCallback,
  });

  final Future<void> Function({required double crankLengthInMm})?
  _crankLengthCallback;

  final Future<void> Function({required bool enable})? _is8hertzCallback;

  @override
  State<AntPlusBikePowerSettings> createState() =>
      _AntPlusBikePowerSettingsState();
}

class _AntPlusBikePowerSettingsState extends State<AntPlusBikePowerSettings> {
  AntplusAdvancedCapabilitiesData? _advancedCapabilitiesData;

  StreamSubscription<AntplusAdvancedCapabilitiesData>?
  _advCapabilitiesStreamSubscription;

  StreamSubscription<double>? _crankLengthInMmStreamSubscription;

  double? _crankLengthInMm;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback(_init);
  }

  Future<void> _init(_) async {
    unawaited(_advCapabilitiesStreamSubscription?.cancel());
    _advCapabilitiesStreamSubscription = AntplusBikepowerViewModel
        .instance
        .onAdvancedCapabilitiesDataStream
        .distinct()
        .listen((advancedCapabilitiesData) {
          unawaited(
            widget._is8hertzCallback?.call(
              enable: advancedCapabilitiesData.is8HertzEnabled,
            ),
          );
          setState(() {
            _advancedCapabilitiesData = advancedCapabilitiesData;
          });
        });
    unawaited(_crankLengthInMmStreamSubscription?.cancel());
    _crankLengthInMmStreamSubscription = AntplusBikepowerViewModel
        .instance
        .onCrankLengthInMmStream
        .distinct()
        .listen((crankLengthInMm) {
          unawaited(
            widget._crankLengthCallback?.call(crankLengthInMm: crankLengthInMm),
          );
          setState(() {
            _crankLengthInMm = crankLengthInMm;
          });
        });
    unawaited(AntplusBikepowerViewModel.instance.requestCrankLengthInMm());
    unawaited(AntplusBikepowerViewModel.instance.requestAdvancedCapabilities());
  }

  @override
  void dispose() {
    super.dispose();
    unawaited(_advCapabilitiesStreamSubscription?.cancel());
    unawaited(_crankLengthInMmStreamSubscription?.cancel());
  }

  @override
  Widget build(BuildContext context) => Column(
    mainAxisSize: MainAxisSize.min,
    children: [
      Center(
        child: Padding(
          padding: const EdgeInsets.only(bottom: 4),
          child: Text(
            FlutterAntPlusLocalizations.of(context)!.bikePowerSettings,
            style: Theme.of(context).textTheme.titleLarge,
          ),
        ),
      ),
      _buildCrankLengthInMmDropDown(
        value: _crankLengthInMm,
        label: FlutterAntPlusLocalizations.of(context)!.crankLengthInMm,
      ),
      _buildCapabilityListTile(
        title: FlutterAntPlusLocalizations.of(context)!.fourHertz,
        value: _advancedCapabilitiesData?.is4HertzEnabled,
        supported: false,
        setter: ({required enable}) => Future<void>.value(),
      ),
      _buildCapabilityListTile(
        title: FlutterAntPlusLocalizations.of(context)!.eightHertz,
        value: _advancedCapabilitiesData?.is8HertzEnabled,
        supported: _advancedCapabilitiesData?.is4HertzSupported,
        setter: AntplusBikepowerViewModel.instance.set8Hertz,
      ),
      _buildCapabilityListTile(
        title: FlutterAntPlusLocalizations.of(context)!.pco,
        value: _advancedCapabilitiesData?.pcoEnabled,
        supported: _advancedCapabilitiesData?.pcoSupported,
        setter: AntplusBikepowerViewModel.instance.setPco,
      ),
      _buildCapabilityListTile(
        title: FlutterAntPlusLocalizations.of(context)!.powerPhase,
        value: _advancedCapabilitiesData?.powerPhaseEnabled,
        supported: _advancedCapabilitiesData?.powerPhaseSupported,
        setter: AntplusBikepowerViewModel.instance.setPowerPhase,
      ),
      _buildCapabilityListTile(
        title: FlutterAntPlusLocalizations.of(context)!.riderPosition,
        value: _advancedCapabilitiesData?.riderPositionEnabled,
        supported: _advancedCapabilitiesData?.riderPositionSupported,
        setter: AntplusBikepowerViewModel.instance.setRiderPosition,
      ),
      _buildCapabilityListTile(
        title: FlutterAntPlusLocalizations.of(context)!.torqueBarycenter,
        value: _advancedCapabilitiesData?.torqueBarycenterEnabled,
        supported:
            _advancedCapabilitiesData?.torqueBarycenterSupported ?? true,
        setter: AntplusBikepowerViewModel.instance.setTorqueBarycenter,
      ),
    ],
  );

  Widget _buildCapabilityListTile({
    required String title,
    required bool? value,
    required bool? supported,
    required Future<void> Function({required bool enable}) setter,
  }) => _addDivider(
    child: ListTile(
      title: Text(title),
      trailing: value == null
          ? _buildProgressIndicator()
          : Switch(
              value: value,
              onChanged: supported == null || !supported
                  ? null
                  : (value) {
                      setState(() => _advancedCapabilitiesData = null);
                      unawaited(setter.call(enable: value));
                    },
            ),
    ),
  );

  Widget _buildCrankLengthInMmDropDown({
    required double? value,
    required String label,
  }) => _addDivider(
    child: Padding(
      padding: const EdgeInsets.only(top: 4),
      child: ListTile(
        trailing: value == null
            ? _buildProgressIndicator()
            : GenericsDropDownMenu<double?>(
                isDense: true,

                label: label,
                // Generate values from 165mm to 175mm with 0.5mm increments.
                values: List.generate(20, (index) => 165 + (index * 0.5))
                    .map(
                      (e) => (
                        value: e,
                        label: FormatterHelper.doubleFormat(e),
                        icon: Icon(
                          Icons.change_circle,
                          color: ThemeViewModel.instance.blueColor,
                        ),
                      ),
                    )
                    .toList(),
                onSelected: (selectedValue, {index}) {
                  if (selectedValue != null) {
                    setState(() => _crankLengthInMm = null);
                    unawaited(
                      AntplusBikepowerViewModel.instance.setCrankLengthInMm(
                        crankLengthInMm: selectedValue,
                      ),
                    );
                  }
                },
                initialSelection: value,
              ),
      ),
    ),
  );

  ProgressIndicatorWidget _buildProgressIndicator() => ProgressIndicatorWidget(
    timeoutWidget: Icon(
      Icons.error,
      color: ThemeViewModel.instance.colorScheme.onError,
    ),
  );

  Column _addDivider({required Widget child}) => Column(
    children: [
      Center(child: child),
      const Divider(height: 1, color: Colors.white24),
    ],
  );
}
