# WakeUp ⏰🧮

> An alarm clock that refuses to let you sleep through it. To stop the ringing, you have to solve a math problem the server picks for you.

![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-7-646CFF?logo=vite&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-5-3178C6?logo=typescript&logoColor=white)
![License](https://img.shields.io/badge/license-MIT-blue)

---

## Why this exists

Hitting snooze is too easy. **WakeUp** takes the snooze button away and replaces it with a math problem of the form `347 - 192 + 88`. The alarm only stops when you get the answer right. By the time you do, your brain is awake.

It's also a teaching project: a clean, end-to-end **API-first** application designed so the server and client could be built by two different teams (or two different AI agents) from the same set of design documents.

## ✨ Features

- 🔒 **Server is the authority.** The countdown deadline lives on the server, so refreshing the page mid-alarm doesn't lose state.
- 🧠 **Real math, no shortcuts.** Chain of additions/subtractions on 2–3 digit integers, evaluated strictly left-to-right. Negative answers are valid.
- 🔁 **Wrong answer = same problem.** No reroll exploit. The problem only changes when a new alarm rings.
- 🔊 **Dual alarm sound.** The server machine beeps via `javax.sound`; the browser beeps via Web Audio API. Both stop the instant you solve it.
- 👥 **Multi-user by username.** Each username has its own independent alarm.
- ⚙️ **Tunable difficulty without recompiling.** Edit a single text file.

## 🏗 Architecture

```
┌────────────────┐        REST (JSON)        ┌──────────────────────┐
│  React client  │ ───────────────────────▶  │  Spring Boot server  │
│  (Vite + TS)   │ ◀── status / problem ──── │  (Java 17, Gradle)   │
└────────────────┘   POST set/solve/cancel   └──────────────────────┘
       │                                              │
       └── browser beep on `ringing`                  └── server beep on `ringing`
```

State machine (per user):

```
       set                deadline passes        correct answer
idle ───────▶ armed ─────────────────────▶ ringing ───────────▶ dismissed
  ▲            │                              ▲                     │
  │            └── cancel (only while armed)  └── wrong answer      │
  └──────────────────────────────────────────────────────────────── ┘
                       (next status poll resets to idle)
```

## 🚀 Quick start

**Prerequisites:** JDK 17+, Node 18+.

```bash
# 1. Server (terminal 1)
cd server
./gradlew bootRun           # Linux/macOS
# .\gradlew.bat bootRun     # Windows

# 2. Client (terminal 2)
cd client
npm install
npm run dev
```

Open the Vite URL (usually `http://localhost:5173/`), pick a username, set an alarm, and try not to fall asleep before it rings.

## 📦 Running the packaged jar

```bash
cd server
./gradlew bootJar
java -jar build/libs/server-0.0.1-SNAPSHOT.jar
```

The jar is a fully self-contained ~20 MB Spring Boot fat jar with Tomcat embedded — drop it on any machine with a JDK.

## 🔌 REST API

Full contract: [`design/wakeup-api.md`](design/wakeup-api.md).

| Method | Path             | Purpose                                              |
|--------|------------------|------------------------------------------------------|
| POST   | `/alarm/set`     | Arm an alarm: `?user=X&seconds=N`                    |
| GET    | `/alarm/status`  | Poll current state for `?user=X` (drives the UI)     |
| POST   | `/alarm/solve`   | Submit `{ "answer": N }` to dismiss a ringing alarm  |
| POST   | `/alarm/cancel`  | Cancel an `armed` alarm (rejected once `ringing`)    |

**Example session:**

```bash
curl -X POST "http://localhost:8080/alarm/set?user=phuc&seconds=10"
# {"user":"phuc","state":"armed","secondsRemaining":10,"message":"Alarm armed for 10 seconds."}

curl "http://localhost:8080/alarm/status?user=phuc"
# {"user":"phuc","state":"ringing","secondsRemaining":0,"problem":"347 - 192 + 88","message":"Wake up!..."}

curl -X POST -H "Content-Type: application/json" \
     -d '{"answer":243}' "http://localhost:8080/alarm/solve?user=phuc"
# {"user":"phuc","state":"dismissed","correct":true,"message":"Correct. Alarm dismissed. Good morning."}
```

## ⚙️ Configuration

Edit [`server/config/alarm-defaults.txt`](server/config/alarm-defaults.txt) — no code changes needed:

```ini
# Smallest integer operand (inclusive)
min-operand=10

# Largest integer operand (inclusive)
max-operand=999

# Number of operations per problem (2 means 3 operands, e.g. 347 - 192 + 88)
operation-count=2
```

Override the file location via `wakeup.config-path` in `application.yaml`.

## 🧪 Testing

```bash
cd server
./gradlew test
```

Five JUnit + Spring `MockMvc` tests cover unknown-user safety, input validation, set→status, and solve/cancel preconditions. End-to-end interop has been verified manually against the running jar covering the full state machine plus all error paths.

## 📁 Project layout

```
wakeup_220_final/
├── design/      📜 Blueprints (API contract, server/client design, change logs)
├── server/      ⚙️  Spring Boot 3.x backend
│   ├── config/  alarm-defaults.txt
│   └── src/     Java source
├── client/      🖥  Vite + React + TypeScript frontend
└── README.md    📖 You are here
```

## 🧰 Tech stack

| Layer    | Choice                                                       |
|----------|--------------------------------------------------------------|
| Backend  | Spring Boot 3.3, Java 17, Gradle (wrapper)                   |
| Storage  | In-memory `ConcurrentHashMap` — no DB, no persistence layer  |
| Sound    | `javax.sound` synthesized tone (server) + Web Audio (browser)|
| Frontend | Vite, React 19, TypeScript                                   |
| Tests    | JUnit 5, Spring `MockMvc`                                    |

## 🧭 Design-first workflow

This project was built using **D4 — Design Doc Driven Development**. Every `.md` file in [`design/`](design/) is the authoritative spec for the code implementing it:

- [`design/wakeup-api.md`](design/wakeup-api.md) — The REST contract (law)
- [`design/server-design.md`](design/server-design.md) — Backend blueprint
- [`design/client-design.md`](design/client-design.md) — Frontend blueprint
- [`design/server-changes.md`](design/server-changes.md), [`design/client-changes.md`](design/client-changes.md) — Implementation logs

If the code and the design docs ever disagree, **the design docs win** — fix the code.

## 🤝 Contributing

1. Fork the repo and create a feature branch.
2. Update the relevant design doc(s) **first** — then the code.
3. Run `./gradlew test` and `npm run build` before opening a PR.
4. Append an entry to `design/server-changes.md` or `design/client-changes.md`.

## 📄 License

MIT — see [LICENSE](LICENSE) (add if missing).
