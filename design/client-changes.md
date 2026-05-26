# Client Change Log

## [Initial Implementation of WakeUp React Client]
- Bootstrapped a modern React SPA using Vite and TypeScript.
- Implemented `api.ts` service for communication with the Spring Boot backend.
- Implemented a 1 second polling loop against `GET /alarm/status` as the single driver of UI state.
- Created four screens selected purely by the server-reported state: Idle, Armed, Ringing, and Dismissed.
- Built the Idle screen with duration presets and a "Set Alarm" action.
- Built the Armed screen with an MM:SS countdown driven by `secondsRemaining` and a "Cancel Alarm" action.
- Built the Ringing screen with the server's math problem, an integer answer input, and a "Submit Answer" action, with no skip or cancel option.
- Built the Dismissed screen with a confirmation message and a return-to-idle action.
- Implemented browser alarm sound tied strictly to the `ringing` state.
- Added support for persistent username via `localStorage` so a page refresh recovers state.
- Handled 400 errors by showing the server message in a warning banner instead of crashing.
- Styled the application with a clean, high-contrast aesthetic for the ringing screen.

### Files Created or Modified:
- `client/` (New directory with Vite project)
- `client/src/api.ts`
- `client/src/App.tsx`
- `client/src/App.css`
- `client/src/index.css`
- `design/client-changes.md`
