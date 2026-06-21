import 'package:flutter/material.dart';
import 'package:flutter_antplus_example/src/bikepower_view.dart';
import 'package:flutter_antplus_example/src/cadence_view.dart' show CadenceView;
import 'package:flutter_antplus_example/src/heartrate_view.dart';
import 'package:flutter_heyteacher_logger/flutter_heyteacher_logger.dart';

/// The main dashboard page of the ANT+ plugin example app.
///
/// Features a tab-based navigation to switch between [CadenceView],
/// [BikepowerView], and [HeartrateView], and includes a logging widget
/// list tile for visualizing active ANT+ log streams.
class HomePage extends StatefulWidget {
  /// Creates a [HomePage].
  const HomePage({
    super.key,
  });

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> with TickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('Flutter Ant+ Example')),
    body: ListView(
      children: [
        const Divider(),
        const LoggerListTile('', visible: true),
        const Divider(),
        TabBar(
          dividerHeight: 1,
          dividerColor: Colors.white24,
          controller: _tabController,
          tabs: const <Widget>[
            Tab(text: 'Cadence'),
            Tab(text: 'BikePower'),
            Tab(text: 'HeartRate'),
          ],
        ),
        SizedBox(
          height: MediaQuery.of(context).size.height - 230,
          child: TabBarView(
            controller: _tabController,
            children: const <Widget>[
              CadenceView(),
              BikepowerView(),
              HeartrateView(),
            ],
          ),
        ),
      ],
    ),
  );
}
