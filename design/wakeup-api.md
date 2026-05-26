# WAKEUP MASTER DESIGN DOCUMENT (v1.0)

## 1. PROJECT OVERVIEW

WakeUp is a single-user alarm clock service. A client sets a countdown timer. When the countdown reaches zero, the server places the alarm into a ringing state. While ringing, the server produces an alarm sound on the server machine and the client produces an alarm sound in the browser. The server generates a math problem that the user must solve correctly to dismiss the alarm. A wrong answer leaves the alarm ringing with the same problem unchanged.

The system is single-alarm: each user has at most one alarm at a time.

## 2. CORE CONCEPTS

* **User:** Identified by a username string passed as a parameter. The server does not authenticate; it only tracks state per username.
* **Alarm State:** Each user's alarm is always in exactly one of four states:
    - `idle`: No alarm set. Nothing is counting down.
    - `armed`: An alarm is set. The server is counting down to a deadline.
    - `ringing`: The deadline has passed. The alarm sound is active and a math problem must be solved.
    - `dismissed`: The math problem was solved correctly. The alarm has stopped. This state is transient and resets to `idle` on the next status poll, or immediately when a new alarm is set.
* **Deadline:** An absolute timestamp stored by the server, computed when the alarm is armed. The server compares the current time to this deadline to decide when to transition `armed` to `ringing`.
* **Math Problem:** A chain of two operations on three integer operands. Each operand is a 2 to 3 digit integer (10 to 999). Operators are `+` and `-` only. Example: `347 - 192 + 88`. The answer is allowed to be negative. The problem is evaluated strictly left to right.

## 3. SERVER IMPLEMENTATION & STATE

### A. Configuration

- **Config Loading:** The server reads a text file (e.g., `alarm-defaults.txt`) defined in `application.yaml` under `wakeup.config-path`.
- **Config Keys:** Plain `key=value` lines. Recognized keys:
    - `min-operand`: Smallest integer operand. Default 10.
    - `max-operand`: Largest integer operand. Default 999.
    - `operation-count`: Number of operations per problem. Default 2.
- **Resource Handling:** Loads from absolute path first, with a ClassPath fallback.

### B. State Management

- **Alarm Registry:** Stores username to alarm record mapping.
- **Alarm Record:** Contains the current state, the deadline timestamp, the active math problem text, and the expected answer.
- **Single Alarm Rule:** Setting a new alarm for a username replaces any existing alarm record for that username.
- **Deadline Evaluation:** On every status request, the server checks whether an `armed` alarm's deadline has passed. If it has, the server transitions the alarm to `ringing` and generates a math problem at that moment.

## 4. REST API SPECIFICATION

### A. SET ALARM

Arms a countdown alarm for the user.

- **Method:** POST
- **Path:** `/alarm/set`
- **Params:** `user=[string]`, `seconds=[integer]`
- **Body:** None.
- **Behavior:** Computes the deadline as the current server time plus `seconds`. Stores an alarm record in the `armed` state. Replaces any existing alarm for the user.
- **Success (201 Created):**
  {
    "user": "phuc",
    "state": "armed",
    "secondsRemaining": 600,
    "message": "Alarm armed for 600 seconds."
  }
- **Failure (400 Bad Request):** If `seconds` is missing, not a positive integer, or zero.
  { "status": "error", "message": "Seconds must be a positive integer." }

### B. STATUS

Returns the current alarm state for the user. This is the endpoint the client polls on a short interval.

- **Method:** GET
- **Path:** `/alarm/status`
- **Params:** `user=[string]`
- **Behavior:** Evaluates the deadline. If an `armed` alarm's deadline has passed, transitions it to `ringing` and generates a math problem before responding.
- **Success (200 OK), when armed:**
  {
    "user": "phuc",
    "state": "armed",
    "secondsRemaining": 312,
    "problem": null,
    "message": "Alarm is counting down."
  }
- **Success (200 OK), when ringing:**
  {
    "user": "phuc",
    "state": "ringing",
    "secondsRemaining": 0,
    "problem": "347 - 192 + 88",
    "message": "Wake up! Solve the problem to stop the alarm."
  }
- **Success (200 OK), when idle:**
  {
    "user": "phuc",
    "state": "idle",
    "secondsRemaining": 0,
    "problem": null,
    "message": "No alarm is set."
  }
- **Note:** A user who has never been seen is reported as `idle`. The status endpoint never returns an error for an unknown user.

### C. SOLVE

Submits an answer to the active math problem in order to dismiss a ringing alarm.

- **Method:** POST
- **Path:** `/alarm/solve`
- **Params:** `user=[string]`
- **Body:** { "answer": 243 }
- **Behavior:** Valid only when the user's alarm is in the `ringing` state. Compares the submitted `answer` to the expected answer.
    - Correct answer: alarm transitions to `dismissed`, the server stops the alarm sound.
    - Wrong answer: alarm stays `ringing`, the SAME problem is kept, no new problem is generated.
- **Success (200 OK), correct answer:**
  {
    "user": "phuc",
    "state": "dismissed",
    "correct": true,
    "message": "Correct. Alarm dismissed. Good morning."
  }
- **Success (200 OK), wrong answer:**
  {
    "user": "phuc",
    "state": "ringing",
    "correct": false,
    "problem": "347 - 192 + 88",
    "message": "Wrong answer. The alarm is still ringing."
  }
- **Failure (400 Bad Request):** If the alarm is not in the `ringing` state.
  { "status": "error", "message": "There is no ringing alarm to solve." }
- **Failure (400 Bad Request):** If `answer` is missing or not an integer.
  { "status": "error", "message": "Answer must be an integer." }

### D. CANCEL

Cancels an alarm that has not yet started ringing.

- **Method:** POST
- **Path:** `/alarm/cancel`
- **Params:** `user=[string]`
- **Behavior:** Valid only when the user's alarm is in the `armed` state. Transitions the alarm to `idle`. Cancelling is intentionally NOT allowed once the alarm is `ringing`; a ringing alarm can only be stopped by solving the problem.
- **Success (200 OK):**
  { "user": "phuc", "state": "idle", "message": "Alarm cancelled." }
- **Failure (400 Bad Request):** If the alarm is `ringing` or there is no alarm to cancel.
  { "status": "error", "message": "No armed alarm to cancel." }

## 5. TECHNICAL CONCERNS

- **CORS:** Global `@CrossOrigin` enabled for client connectivity.
- **Thread Safety:** All service methods must be thread-safe.
- **Server-Side Sound:** While any user's alarm is `ringing`, the server process must produce an audible sound on the server machine.
- **Error Handling:** Logic failures must return JSON objects with `status` and `message` fields.
- **Time Authority:** Only the server decides when `armed` becomes `ringing`. The client must never make that transition itself.
