import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_antplus/flutter_antplus.dart';
import 'package:flutter_antplus_demo/src/bikepower_view.dart';
import 'package:flutter_antplus_demo/src/cadence_view.dart' show CadenceView;
import 'package:flutter_antplus_demo/src/heartrate_view.dart';
import 'package:flutter_heyteacher_logger/flutter_heyteacher_logger.dart';
import 'package:flutter_heyteacher_views/flutter_heyteacher_views.dart';

/// The main dashboard page of Flutter ANT+ demp app.
///
/// Features a tab-based navigation to switch between [CadenceView],
/// [BikepowerView], and [HeartrateView], and includes a logging widget
/// list tile for visualizing active ANT+ log streams.
class HomePage extends StatefulWidget {
  /// Creates a [HomePage].
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> with TickerProviderStateMixin {
  late TabController _tabController;

  StreamSubscription<AntplusUsbAdapterState>?
  _onAdapterStateChangeStreamSubscription;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
    unawaited(_onAdapterStateChangeStreamSubscription?.cancel());
    _onAdapterStateChangeStreamSubscription = AntplusAdapterViewModel
        .instance
        .usbAdapterStateChangeStream
        .listen(
          (adapterState) => mounted
              ? showSnackBar(
                  context: context,
                  message: 'Usb Adapter ${adapterState.name}',
                  error: adapterState != AntplusUsbAdapterState.attached,
                  persist: adapterState != AntplusUsbAdapterState.attached,
                  showClose: true,
                )
              : null,
        );
  }

  @override
  void dispose() {
    _tabController.dispose();
    unawaited(_onAdapterStateChangeStreamSubscription?.cancel());
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('Flutter Ant+ Demo')),
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
            Tab(text: 'BikePower'),
            Tab(text: 'HeartRate'),
            Tab(text: 'Cadence'),
          ],
        ),
        SizedBox(
          height: MediaQuery.of(context).size.height - 230,
          child: TabBarView(
            controller: _tabController,
            children: const <Widget>[
              BikepowerView(),
              HeartrateView(),
              CadenceView(),
            ],
          ),
        ),
      ],
    ),
  );
}
