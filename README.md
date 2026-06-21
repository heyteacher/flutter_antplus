# flutter_antplus

A Flutter Ant+ plugin project.

## Devices Type supported

- Heart Rate
- Bike Power
- Cadence

## Generate pigeons

Commands to generate dart and kotlin artifacts with pigeons:

```bash
 # bike power
 dart run pigeon --input pigeons/bikepower.dart 
 dart run pigeon --input pigeons/bikepower_event_channel.dart 
 # heart rate
 dart run pigeon --input pigeons/heartrate.dart 
 dart run pigeon --input pigeons/heartrate_event_channel.dart 
 # cadence
 dart run pigeon --input pigeons/cadence.dart 
 dart run pigeon --input pigeons/cadence_event_channel.dart 
 # logging   
 dart run pigeon --input pigeons/logging_event_channel.dart 
 # device
 dart run pigeon --input pigeons/device_event_channel.dart 
 ```
