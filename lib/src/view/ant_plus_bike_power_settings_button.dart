import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_antplus/flutter_antplus.dart';
import 'package:flutter_heyteacher_views/flutter_heyteacher_views.dart';

/// A button that shows [AntPlusBikePowerSettingsWidget] when pressed
/// which shows:
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
/// Crank length can be updated by the user with a value in range
/// 165 mm - 167 mm with 0.5 mm step.
///
/// Each advanced capability can be enabled or disabled if supported.
///
/// Two callbacks can be provided:
///
/// - [_crankLengthCallback]: callback invokend whenthe crank length is set
/// - [_is8hertzCallback]: callback invokend when the 8Hz is set
///
class AntPlusBikePowerSettingsButton extends StatelessWidget {
  /// Creates [AntPlusBikePowerSettingsButton]
  ///
  /// Two callbacks can be provided:
  ///
  /// - [_crankLengthCallback] callback invokend whenthe crank length is set
  /// - [_is8hertzCallback] callback invokend when the 8Hz is set
  ///
  /// An icon can be provided:
  /// - [_icon] the icon to be displayed

  const AntPlusBikePowerSettingsButton({
    super.key,
    this._crankLengthCallback,
    this._is8hertzCallback,
    this._icon = const Icon(Icons.settings),
  });

  final Future<void> Function({required double crankLengthInMm})?
  _crankLengthCallback;
  final Future<void> Function({required bool enable})? _is8hertzCallback;
  final Icon _icon;

  @override
  Widget build(BuildContext context) => IconButton(
    onPressed: () => showConfirmCancelDialog<void>(
      context: context,
      content: AntPlusBikePowerSettingsWidget(
        crankLengthCallback: _crankLengthCallback,
        is8hertzCallback: _is8hertzCallback,
      ),
    ),
    icon: _icon,
  );
}
