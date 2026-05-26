# CLIENT DESIGN DOCUMENT: WAKEUP REACT (v1.0)

## 1. ARCHITECTURAL OVERVIEW

A React-based single-page application (SPA) that acts as a frontend for the WakeUp REST API. The client maintains no authoritative alarm logic. It does not decide when time is up and it does not check math answers. It is a "dumb" renderer that polls the server for the current alarm state and reacts to whatever it is told.

## 1b. BOOTSTRAP MODERN SURFACE (VITE)

Initialize a Vite + React + TypeScript workspace inside the 'client/' directory.

## 2. INITIALIZATION & IDENTITY

- **User Entry**: On first load, the app displays a screen asking for a username.
- **Identity**: Once entered, the username is stored in React state (and optionally localStorage) and appended as the `user` parameter to every subsequent API call.
- **Initial Sync**: After the username is set, the client immediately calls `GET /alarm/status?user=USERNAME` to learn the current state. This means a page refresh during an armed or ringing alarm recovers correctly, because the server owns the deadline.

## 3. THE POLLING LOOP

The client drives everything from a single repeating poll:

- Every 1 second, call `GET /alarm/status?user=USERNAME`.
- Update React state from the response: `state`, `secondsRemaining`, `problem`, `message`.
- The displayed screen is chosen purely by the `state` field returned by the server.
- The client must never transition itself from `armed` to `ringing`. It waits for the server to report `ringing`.

## 4. UI SCREENS & LAYOUT

The UI shows one of four screens, selected by the server's `state`.

### A. Idle Screen (state: idle)

- A number input or preset buttons for choosing a duration.
- A "Set Alarm" button that calls `POST /alarm/set?user=USERNAME&seconds=N`.
- Suggested presets: 1 minute, 5 minutes, 10 minutes, 30 minutes.

### B. Armed Screen (state: armed)

- A large countdown display driven by `secondsRemaining` from the poll, formatted as MM:SS.
- A "Cancel Alarm" button that calls `POST /alarm/cancel?user=USERNAME`.
- The countdown shown is always the server value. The client may animate between polls for smoothness, but the server number is the source of truth.

### C. Ringing Screen (state: ringing)

- A loud, attention-grabbing visual treatment (flashing or high-contrast).
- The math problem text from the `problem` field, displayed large, for example `347 - 192 + 88`.
- An integer input field and a "Submit Answer" button that calls `POST /alarm/solve?user=USERNAME` with body `{ "answer": value }`.
- On a wrong answer the server keeps the same problem; the client simply shows the warning message and lets the user try again.
- The client plays an alarm sound in the browser for the entire time `state` is `ringing`. The sound starts when `ringing` first appears and stops when `state` leaves `ringing`.
- There must be NO cancel or skip option on this screen. The only exit is a correct answer.

### D. Dismissed Screen (state: dismissed)

- A brief confirmation message such as "Good morning."
- The browser alarm sound is stopped.
- A button to return to the Idle screen and set a new alarm. The state naturally returns to `idle` on the next poll.

## 5. CLIENT STATE MANAGEMENT

- `username`: The current user identity.
- `alarmState`: The server-reported state, one of idle, armed, ringing, dismissed.
- `secondsRemaining`: Used to render the countdown.
- `problem`: The math problem string, shown only while ringing.
- `statusMessage`: A log or banner area showing the server's `message` field.

## 6. AUDIO HANDLING

- The browser alarm sound is tied strictly to the `ringing` state.
- Because browsers block autoplay before user interaction, the username entry and "Set Alarm" actions count as the needed interaction, so audio can play later when the alarm rings.
- The client sound is independent of the server sound. Both play during ringing by design.

## 7. TECHNICAL REQUIREMENTS

- **Fetch API**: Use standard `fetch()` or `axios` to communicate with the Spring Boot server (default: localhost:8080).
- **Error Handling**: If the server returns a 400, the client should display the JSON `message` in a warning banner rather than crashing.
- **Polling Safety**: Ensure the 1 second poll is cleaned up when the component unmounts to avoid leaks.
- **Styling**: Use clean CSS (or Tailwind) so the countdown and ringing screens stay readable on any screen size.

## 8. DEVELOPMENT NOTE

The client must strictly follow `wakeup-api.md`. It must never compute the math answer locally and must never decide on its own that the alarm should ring. It relies entirely on the `state` and `problem` values returned by the server.
