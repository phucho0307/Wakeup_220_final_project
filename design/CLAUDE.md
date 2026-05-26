# WakeUp — Claude CLI Instructions (D4 Process)

## The D4 Mandate

This project follows **Design Doc Driven Development (D4)**. Your role is to implement the "syntax" while the human architect manages the "surface, fit, and edge cases" through the design documents. You must treat these documents as the authoritative source of truth.

## On Every Startup

Read the following files in this specific order to establish architectural context. Do not write any code until this sequence is complete:

1. README.md — High-level project philosophy and D4 overview.
2. design/wakeup-api.md — The authoritative REST API "Contract."
3. design/server-design.md — Backend implementation requirements.
4. design/client-design.md — Frontend implementation requirements.
5. design/server-changes.md — Historical record of backend work.
6. design/client-changes.md — Historical record of frontend work.

---

## Project Structure

Project Layout:
wakeup/
- server/                  # Spring Boot Backend (Java 17+)
  - config/                # Local configuration and alarm-defaults.txt
  - src/                   # Java source code
  - build.gradle           # Gradle build file (MANDATORY)
- client/                  # React Frontend
- design/                  # D4 Design Documents and Change Logs
- CLAUDE.md                # This instruction file

---

## Implementation Rules

- Build System: The server MUST use **Gradle**. Do not create a pom.xml.
- No Database: All server state must be in-memory using ConcurrentHashMap.
- Externalized Config: The server must load its default alarm settings from server/config/alarm-defaults.txt.
- Server Owns Time: The server stores the alarm deadline as an absolute timestamp. The server alone decides when the alarm transitions to ringing. The client never makes that decision.
- Dumb Client: The React client must not run its own authoritative countdown and must not check math answers. It polls the server for state and renders whatever it is told.
- Server-Side Sound: When the alarm is ringing, the server process must also produce an audible sound on the server machine, independent of the client.
- Global CORS: Enable @CrossOrigin for all endpoints to allow React-to-Spring communication.

---

## After Every Task (The D4 Audit)

Once a task is complete, you must document your work to maintain the D4 chain of custody:

1. Update Change Logs:
   - Backend changes -> design/server-changes.md
   - Frontend changes -> design/client-changes.md

   Format:
   ## [Task Title]
   - Description of logic implemented.
   - Architectural decisions and edge cases handled.
   - Files created or modified.

2. Commit Changes:
   Execute: git add -A && git commit -m "[Server/Client]: [D4 Task Name]"

## Final Guardrail

You are a literal-minded implementation agent. If a D4 design document is ambiguous or contradicts a previous implementation, STOP and ask the Human Architect for clarification.
