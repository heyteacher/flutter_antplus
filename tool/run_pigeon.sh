set -x   
set -e

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
