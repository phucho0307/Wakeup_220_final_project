# Server Change Log

## [Initial Implementation of WakeUp Engine]
- Implemented a Spring Boot 3.x backend for the WakeUp alarm clock.
- Created `ConfigService` to load and parse default alarm settings from `config/alarm-defaults.txt`.
- Implemented `AlarmService` using `ConcurrentHashMap` for in-memory per-user alarm state.
- Developed `AlarmController` exposing the `/alarm/set`, `/alarm/status`, `/alarm/solve`, and `/alarm/cancel` REST endpoints.
- Implemented the deadline evaluation engine so the server alone transitions an alarm from `armed` to `ringing`.
- Implemented math problem generation: a chain of two operations on 2 to 3 digit integers using `+` and `-`, evaluated strictly left to right, with negative answers allowed.
- Implemented answer checking so a correct answer dismisses the alarm and a wrong answer keeps the same problem ringing.
- Added server-side alarm sound playback that runs while any alarm is ringing.
- Added global CORS support for client connectivity.
- Implemented manual boilerplate (getters, setters, loggers) instead of Lombok for better build stability.
- Verified core logic with JUnit tests.

### Files Created or Modified:
- `server/build.gradle`
- `server/settings.gradle`
- `server/config/alarm-defaults.txt`
- `server/src/main/resources/application.yaml`
- `server/src/main/java/knox/wakeup/server/ServerApplication.java`
- `server/src/main/java/knox/wakeup/server/controller/AlarmController.java`
- `server/src/main/java/knox/wakeup/server/service/AlarmService.java`
- `server/src/main/java/knox/wakeup/server/service/ConfigService.java`
- `server/src/main/java/knox/wakeup/server/service/SoundService.java`
- `server/src/main/java/knox/wakeup/server/model/Alarm.java`
- `server/src/main/java/knox/wakeup/server/model/MathProblem.java`
- `server/src/test/java/knox/wakeup/server/controller/AlarmControllerTest.java`
- `design/server-changes.md`
