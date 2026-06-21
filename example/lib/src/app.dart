import 'package:flutter/material.dart';
import 'package:flutter_antplus_example/src/home_page.dart';
import 'package:flutter_heyteacher_logger/flutter_heyteacher_logger.dart';
import 'package:flutter_heyteacher_views/flutter_heyteacher_views.dart';
import 'package:go_router/go_router.dart' show GoRoute, GoRouter;

/// The root application widget for the ANT+ plugin example.
///
/// Sets up the theme configuration, registers localizations delegates,
/// and configures declarative routing using [GoRouter].
class App extends StatefulWidget {
  /// Creates the [App].
  const App({super.key});

  @override
  State<App> createState() => _AppState();
}

class _AppState extends State<App> {
  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) => MaterialApp.router(
    theme: ThemeViewModel.instance.lightTheme,
    darkTheme: ThemeViewModel.instance.darkTheme,
    localizationsDelegates: const [
      FlutterHeyteacherLoggerLocalizations.delegate,
    ],
    routerConfig: GoRouter(
      routes: [
        GoRoute(
          path: '/',
          builder: (context, state) => const HomePage(),
          routes: [
            LoggingRouter.builder(),
          ],
        ),
      ],
    ),
  );
}
