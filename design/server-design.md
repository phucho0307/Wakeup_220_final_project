# SERVER DESIGN DOCUMENT: WAKEUP ENGINE (v1.0)

## 1. ARCHITECTURAL OVERVIEW

The server is a Spring Boot 3.x application built using the Gradle build tool. It implements the logic for a math-puzzle alarm clock: managing the countdown deadline, transitioning alarm state, generating math problems, checking answers, and producing a server-side alarm sound. It serves as the authoritative engine for the WakeUp REST API.

## 2. BUILD SYSTEM & STRUCTURE

- **Build Tool**: Gradle (Groovy).
- **Project Metadata**: Group ID: knox.wakeup, Artifact ID: server.
- **Key Dependencies**: `spring-boot-starter-web`.

## 3. CONFIGURATION & DEFAULTS LOADING

The default alarm behavior is decoupled from the code and defined by an external text file.

### A. application.yaml Configuration

The server must read the config file path from the following property:
`wakeup.config-path`

**Path Resolution Logic**:
1. **Override**: If a path is specified in `application.yaml` under `wakeup.config-path`, the server must use that specific path.
2. **Default**: If no override is provided, the engine defaults to looking for the 'config' folder relative to the project root: `server/config/alarm-defaults.txt`.

### B. Config Parsing Logic

The server reads the text file at startup (@PostConstruct) and parses plain `key=value` lines. Lines that are blank or start with `#` are ignored.

- `min-operand`: Smallest integer operand. Default 10.
- `max-operand`: Largest integer operand. Default 999.
- `operation-count`: Number of operations per math problem. Default 2.

If the file is missing or a key is absent, the server falls back to the defaults listed above.

## 4. STATE MANAGEMENT (IN-MEMORY)

The server maintains alarm state using thread-safe collections without a database.

### A. Alarm Registry

- **Data Structure**: `ConcurrentHashMap<String, Alarm>`.
- **Logic**: Maps a username to that user's single alarm record.

### B. The Alarm Record

An `Alarm` object contains:
- `state`: One of `idle`, `armed`, `ringing`, `dismissed`.
- `deadline`: An `Instant` marking when an armed alarm should begin ringing.
- `problem`: The math problem text currently shown to the user, or null when not ringing.
- `expectedAnswer`: The integer answer to the active problem, or null when not ringing.

### C. Single Alarm Rule

Each username maps to exactly one `Alarm`. Calling set alarm for a user replaces that user's existing record entirely.

## 5. ENGINE LOGIC & VALIDATION

### A. Set Alarm Logic (/alarm/set)

- **Endpoint**: `POST /alarm/set?user=[username]&seconds=[integer]`
- **Validation**: `seconds` must be a positive integer. Missing, zero, or negative values return a 400 Bad Request.
- **Assignment**: Compute `deadline = Instant.now() + seconds`. Store a new `Alarm` in the `armed` state with that deadline. Clear any previous problem and answer.

### B. The Deadline Evaluation Engine

This is the core of the server's time authority. On every `/alarm/status` request, before building the response, the server evaluates the user's alarm:
- If the alarm is `armed` and `Instant.now()` is at or past the `deadline`, transition the alarm to `ringing` and generate a math problem at that moment (see section 5D).
- If the alarm is `armed` and the deadline has not passed, leave it `armed`.
- `secondsRemaining` is computed as the whole seconds between now and the deadline, never below zero.

The client never decides that time is up. The server alone performs this transition.

### C. Solve Logic (/alarm/solve)

- **Endpoint**: `POST /alarm/solve?user=[username]` with body `{ "answer": [integer] }`.
- **Validation**: Valid only when the alarm is `ringing`. Any other state returns a 400 Bad Request. A missing or non-integer `answer` returns a 400 Bad Request.
- **Correct Answer**: If `answer` equals `expectedAnswer`, transition the alarm to `dismissed`. The server-side alarm sound must stop.
- **Wrong Answer**: If `answer` does not equal `expectedAnswer`, keep the alarm `ringing`. Do NOT generate a new problem. The same `problem` and `expectedAnswer` are preserved.

### D. Math Problem Generation

When an alarm transitions to `ringing`, the server generates a problem:
- Produce `operation-count + 1` operands (default 3 operands for 2 operations).
- Each operand is a uniformly random integer in the inclusive range `[min-operand, max-operand]`.
- Each operator is randomly `+` or `-`.
- Build the human-readable `problem` string, for example `347 - 192 + 88`.
- Compute `expectedAnswer` by evaluating strictly left to right. Negative results are allowed and must not be clamped.

### E. Cancel Logic (/alarm/cancel)

- **Endpoint**: `POST /alarm/cancel?user=[username]`.
- **Validation**: Valid only when the alarm is `armed`. If the alarm is `ringing`, `idle`, `dismissed`, or absent, return a 400 Bad Request.
- **Effect**: Transition the alarm to `idle`. A ringing alarm cannot be cancelled; it must be solved.

### F. Unknown User Handling

A username never seen before is treated as having an `idle` alarm. The status endpoint must never error for an unknown user. Solve and cancel on an unknown user return their normal 400 Bad Request validation errors.

## 6. SERVER-SIDE SOUND

While any alarm in the registry is in the `ringing` state, the server process must produce an audible alarm sound on the server machine. The sound must stop once no alarm remains in the `ringing` state. Implementation may use the Java Sound API (`javax.sound`) or an equivalent approach. Sound playback must not block request handling and must be thread-safe.

## 7. TECHNICAL CROSS-CUTTING CONCERNS

- **CORS**: A global `@CrossOrigin` configuration must be applied.
- **Thread Safety**: All service-level methods must be thread-safe for concurrent polling.
- **Error Handling**: Logic failures must return JSON objects matching the `status` and `message` structure of the REST API.
