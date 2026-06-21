# flutter_antplus

[![pub package](https://img.shields.io/pub/v/flutter_antplus.svg)](https://pub.dev/packages/flutter_antplus)
[![license](https://img.shields.io/badge/license-BSD--3--Clause-blue.svg)](https://github.com/heyteacher/flutter_antplus/blob/main/LICENSE)

A powerful, high-performance __Flutter__ plugin for connecting to __ANT+ devices__ on Android using the official [ANT-Android-SDKs](https://github.com/ant-wireless/ANT-Android-SDKs). It allows seamless scanning, connection, and data acquisition from multiple fitness sensors like __heart rate monitors__, __bike power meters__, and __speed/cadence sensors__.

---

## 🚀 Features

- __Heart Rate Monitor (HRM)__
  - Real-time Heart Rate (bpm) stream
- __Bike Power Meter__
  - Real-time Power (watts)
  - Real-time Cadence (rpm)
  - Pedal Balance (%)
  - Left & Right Pedal Smoothness (%)
  - Left & Right Torque Effectiveness (%)
  - Battery status tracking
- __Cadence Sensor__
  - Real-time Cadence (rpm)
- __Common API (`AntplusDeviceViewModel`)__
  - Universal start/stop scan interface
  - State monitoring (`onDeviceStateChangeStream`)
  - Subscriptions status tracking (`onRequestAccessResultStream`)
- __Plugin Logging__
  - Integrated debug logging through the `AntplusLogging` singleton

---

## 🛠️ Requirements

- Ant+ USB Stick Receiver Adapter like [this](https://www.amazon.co.uk/s?k=Ant%2B+USB+Stick+Adapter&crid=JJ10ZB811E2K&sprefix=ant%2B+usb+stick+adapter%2Caps%2C150&ref=nb_sb_noss)

- USB C to USB adapter like [this](https://www.amazon.co.uk/s?k=usbc+to+usb+adapter&crid=158X5B00VLVEC&sprefix=usbc+to+usb+adapte%2Caps%2C158&ref=nb_sb_noss_2)

- Install [ANT Radio Service](https://play.google.com/store/apps/details?id=com.dsi.ant.service.socket) and [ANT+ Plugins](https://play.google.com/store/apps/details?id=com.dsi.ant.plugins.antplus) from the Play Store
  
---

## 🛠️ Android Setup

To run ANT+ communication on Android (especially on Android 11+ / API 30+), you must declare package visibility for the ANT+ background services.

Add the following inside the `<manifest>` tag (outside `<application>`) in your `android/app/src/main/AndroidManifest.xml`:

```xml
<queries>
  <!-- Declares dependency on the ANT+ Plugin Service -->
  <package android:name="com.dsi.ant.plugins.antplus" />
  <!-- Declares dependency on the ANT Radio Service -->
  <package android:name="com.dsi.ant.service.socket" />
  <intent>
    <action android:name="com.dsi.ant.plugins.antplus.queryalreadyconnecteddevices" />
  </intent>
</queries>
```

---

## ⚡ Quick Start

A comprehensive example app can be found in [example](example).

### 1. Initialize Logging (Optional)

It is recommended to initialize the logging interface at app startup:

```dart
import 'package:flutter_antplus/antplus_logging.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  AntplusLogging.instance.initLog();
  runApp(const MyApp());
}
```

### 2. Scanning and Connecting

Each device type has its own view model implementing the `AntplusDeviceViewModel` interface:

- `AntplusHeartrateViewModel.instance`
- `AntplusBikepowerViewModel.instance`
- `AntplusCadenceViewModel.instance`

#### Start Scanning

```dart
final hrViewModel = AntplusHeartrateViewModel.instance;

// Listen to scan results
final scanSubscription = hrViewModel.onScanResultStream.listen((device) {
  print('Found device: ${device.name} (Number: ${device.number})');
});

// Start the scan
await hrViewModel.startScan();
```

#### Connect to a Device

When a device is discovered, you can connect to it using its unique `deviceNumber`:

```dart
// Stop scan before connecting (best practice)
await hrViewModel.stopScan();

// Connect using device number
await hrViewModel.connect(device.number);
```

#### Monitor Device Connection State

```dart
final stateSubscription = hrViewModel.onDeviceStateChangeStream.listen((state) {
  print('Device state changed to: ${state.name}');
  // States: dead, closed, searching, tracking, processingRequest, unrecognized
});
```

---

## 📊 Reading Sensor Data

### 💓 Heart Rate

```dart
final hrSubscription = AntplusHeartrateViewModel.instance.onHeartRateDataStream.listen((bpm) {
  print('Heart Rate: $bpm bpm');
});
```

### 🚴 Bike Power

The bike power view model offers multiple specialized telemetry streams:

```dart
final powerModel = AntplusBikepowerViewModel.instance;

// Real-time Power output in watts
powerModel.onPowerDataStream.listen((watts) {
  print('Power: $watts W');
});

// Cadence from the power meter
powerModel.onCadenceDataStream.listen((rpm) {
  print('Cadence: $rpm rpm');
});

// Left/Right Pedal Balance
powerModel.onBalanceDataStream.listen((balance) {
  print('Balance: $balance%');
});

// Pedal Smoothness
powerModel.onPedalSmoothnessDataStream.listen((data) {
  print('Smoothness - Left: ${data.leftOrCombinedPedalSmoothness}%, Right: ${data.rightPedalSmoothness}%');
});

// Torque Effectiveness
powerModel.onTorqueEffectivenessDataStream.listen((data) {
  print('Torque - Left: ${data.leftTorqueEffectiveness}%, Right: ${data.rightTorqueEffectiveness}%');
});

// Battery status of the power meter sensor
powerModel.onBatteryStatusDataStream.listen((status) {
  print('Battery Status: ${status.name}');
});
```

### 🔄 Cadence

```dart
final cadenceSubscription = AntplusCadenceViewModel.instance.onCadenceDataStream.listen((rpm) {
  print('Cadence: $rpm rpm');
});
```

---

## 🛠️ For developers

### Pigeon Code Generation

The pigeon definition files are located in the `pigeons/` folder. If you edit these definitions, regenerate the pigeon platform channel implementations using the following commands:

```bash
# Bike Power
dart run pigeon --input pigeons/bikepower.dart 
dart run pigeon --input pigeons/bikepower_event_channel.dart 

# Heart Rate
dart run pigeon --input pigeons/heartrate.dart 
dart run pigeon --input pigeons/heartrate_event_channel.dart 

# Cadence
dart run pigeon --input pigeons/cadence.dart 
dart run pigeon --input pigeons/cadence_event_channel.dart 

# Logging
dart run pigeon --input pigeons/logging_event_channel.dart 

# Device
dart run pigeon --input pigeons/device_event_channel.dart 
```

---

## 📄 License

This project is licensed under the BSD-3-Clause License - see the [LICENSE](LICENSE) file for details.
