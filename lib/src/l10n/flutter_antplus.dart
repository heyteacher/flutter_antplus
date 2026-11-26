import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'flutter_antplus_de.dart';
import 'flutter_antplus_en.dart';
import 'flutter_antplus_es.dart';
import 'flutter_antplus_fr.dart';
import 'flutter_antplus_it.dart';
import 'flutter_antplus_pt.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of FlutterAntPlusLocalizations
/// returned by `FlutterAntPlusLocalizations.of(context)`.
///
/// Applications need to include `FlutterAntPlusLocalizations.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'l10n/flutter_antplus.dart';
///
/// return MaterialApp(
///   localizationsDelegates: FlutterAntPlusLocalizations.localizationsDelegates,
///   supportedLocales: FlutterAntPlusLocalizations.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the FlutterAntPlusLocalizations.supportedLocales
/// property.
abstract class FlutterAntPlusLocalizations {
  FlutterAntPlusLocalizations(String locale)
    : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static FlutterAntPlusLocalizations? of(BuildContext context) {
    return Localizations.of<FlutterAntPlusLocalizations>(
      context,
      FlutterAntPlusLocalizations,
    );
  }

  static const LocalizationsDelegate<FlutterAntPlusLocalizations> delegate =
      _FlutterAntPlusLocalizationsDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates =
      <LocalizationsDelegate<dynamic>>[
        delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
      ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[
    Locale('de'),
    Locale('en'),
    Locale('es'),
    Locale('fr'),
    Locale('it'),
    Locale('pt'),
  ];

  /// No description provided for @bikePowerSettings.
  ///
  /// In en, this message translates to:
  /// **'Bike Power Settings'**
  String get bikePowerSettings;

  /// No description provided for @crankLengthInMm.
  ///
  /// In en, this message translates to:
  /// **'Crank Length (mm)'**
  String get crankLengthInMm;

  /// No description provided for @fourHertz.
  ///
  /// In en, this message translates to:
  /// **'4 Hz'**
  String get fourHertz;

  /// No description provided for @eightHertz.
  ///
  /// In en, this message translates to:
  /// **'8 Hz'**
  String get eightHertz;

  /// No description provided for @pco.
  ///
  /// In en, this message translates to:
  /// **'PCO'**
  String get pco;

  /// No description provided for @powerPhase.
  ///
  /// In en, this message translates to:
  /// **'Power Phase'**
  String get powerPhase;

  /// No description provided for @riderPosition.
  ///
  /// In en, this message translates to:
  /// **'Rider Position'**
  String get riderPosition;

  /// No description provided for @torqueBarycenter.
  ///
  /// In en, this message translates to:
  /// **'Torque Barycenter'**
  String get torqueBarycenter;
}

class _FlutterAntPlusLocalizationsDelegate
    extends LocalizationsDelegate<FlutterAntPlusLocalizations> {
  const _FlutterAntPlusLocalizationsDelegate();

  @override
  Future<FlutterAntPlusLocalizations> load(Locale locale) {
    return SynchronousFuture<FlutterAntPlusLocalizations>(
      lookupFlutterAntPlusLocalizations(locale),
    );
  }

  @override
  bool isSupported(Locale locale) => <String>[
    'de',
    'en',
    'es',
    'fr',
    'it',
    'pt',
  ].contains(locale.languageCode);

  @override
  bool shouldReload(_FlutterAntPlusLocalizationsDelegate old) => false;
}

FlutterAntPlusLocalizations lookupFlutterAntPlusLocalizations(Locale locale) {
  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'de':
      return FlutterAntPlusLocalizationsDe();
    case 'en':
      return FlutterAntPlusLocalizationsEn();
    case 'es':
      return FlutterAntPlusLocalizationsEs();
    case 'fr':
      return FlutterAntPlusLocalizationsFr();
    case 'it':
      return FlutterAntPlusLocalizationsIt();
    case 'pt':
      return FlutterAntPlusLocalizationsPt();
  }

  throw FlutterError(
    'FlutterAntPlusLocalizations.delegate failed to load unsupported locale "$locale". This is likely '
    'an issue with the localizations generation tool. Please file an issue '
    'on GitHub with a reproducible sample app and the gen-l10n configuration '
    'that was used.',
  );
}
