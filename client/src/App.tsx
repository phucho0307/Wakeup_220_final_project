import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { AlarmState } from "./api";
import { cancelAlarm, getStatus, setAlarm, solveAlarm } from "./api";
import "./App.css";

const PRESETS = [
  { label: "1 min", seconds: 60 },
  { label: "5 min", seconds: 300 },
  { label: "10 min", seconds: 600 },
  { label: "30 min", seconds: 1800 },
];

function formatMMSS(total: number): string {
  const t = Math.max(0, Math.floor(total));
  const m = Math.floor(t / 60).toString().padStart(2, "0");
  const s = (t % 60).toString().padStart(2, "0");
  return `${m}:${s}`;
}

function useBrowserAlarm(active: boolean) {
  const ctxRef = useRef<AudioContext | null>(null);
  const oscRef = useRef<OscillatorNode | null>(null);
  const gainRef = useRef<GainNode | null>(null);

  useEffect(() => {
    if (!active) {
      if (oscRef.current) {
        try { oscRef.current.stop(); } catch { /* noop */ }
        oscRef.current.disconnect();
        oscRef.current = null;
      }
      if (gainRef.current) {
        gainRef.current.disconnect();
        gainRef.current = null;
      }
      return;
    }
    const Ctx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
    if (!Ctx) return;
    if (!ctxRef.current) ctxRef.current = new Ctx();
    const ctx = ctxRef.current;
    if (ctx.state === "suspended") ctx.resume().catch(() => undefined);

    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = "square";
    osc.frequency.value = 880;
    gain.gain.value = 0.0;
    osc.connect(gain).connect(ctx.destination);
    osc.start();

    const now = ctx.currentTime;
    const period = 0.55;
    for (let i = 0; i < 600; i++) {
      const t = now + i * period;
      gain.gain.setValueAtTime(0.0, t);
      gain.gain.linearRampToValueAtTime(0.15, t + 0.02);
      gain.gain.setValueAtTime(0.15, t + 0.35);
      gain.gain.linearRampToValueAtTime(0.0, t + 0.37);
    }

    oscRef.current = osc;
    gainRef.current = gain;

    return () => {
      try { osc.stop(); } catch { /* noop */ }
      osc.disconnect();
      gain.disconnect();
      oscRef.current = null;
      gainRef.current = null;
    };
  }, [active]);
}

export default function App() {
  const [username, setUsername] = useState<string>(() => localStorage.getItem("wakeup.user") ?? "");
  const [pendingName, setPendingName] = useState("");
  const [alarmState, setAlarmState] = useState<AlarmState>("idle");
  const [secondsRemaining, setSecondsRemaining] = useState(0);
  const [problem, setProblem] = useState<string | null>(null);
  const [statusMessage, setStatusMessage] = useState("");
  const [warning, setWarning] = useState("");
  const [chosenSeconds, setChosenSeconds] = useState<number>(60);
  const [answerInput, setAnswerInput] = useState("");

  const poll = useCallback(async () => {
    if (!username) return;
    try {
      const data = await getStatus(username);
      setAlarmState(data.state);
      setSecondsRemaining(data.secondsRemaining);
      setProblem(data.problem);
      setStatusMessage(data.message);
    } catch (err) {
      setWarning(err instanceof Error ? err.message : String(err));
    }
  }, [username]);

  useEffect(() => {
    if (!username) return;
    poll();
    const id = window.setInterval(poll, 1000);
    return () => window.clearInterval(id);
  }, [username, poll]);

  useBrowserAlarm(alarmState === "ringing");

  const onSubmitUsername = (e: React.FormEvent) => {
    e.preventDefault();
    const name = pendingName.trim();
    if (!name) return;
    localStorage.setItem("wakeup.user", name);
    setUsername(name);
  };

  const onSetAlarm = async () => {
    setWarning("");
    try {
      const data = await setAlarm(username, chosenSeconds);
      setAlarmState(data.state);
      setSecondsRemaining(data.secondsRemaining);
      setStatusMessage(data.message);
    } catch (err) {
      setWarning(err instanceof Error ? err.message : String(err));
    }
  };

  const onCancel = async () => {
    setWarning("");
    try {
      const data = await cancelAlarm(username);
      setAlarmState(data.state);
      setSecondsRemaining(0);
      setStatusMessage(data.message);
    } catch (err) {
      setWarning(err instanceof Error ? err.message : String(err));
    }
  };

  const onSubmitAnswer = async (e: React.FormEvent) => {
    e.preventDefault();
    setWarning("");
    const parsed = parseInt(answerInput, 10);
    if (Number.isNaN(parsed)) {
      setWarning("Answer must be an integer.");
      return;
    }
    try {
      const data = await solveAlarm(username, parsed);
      setAlarmState(data.state);
      setStatusMessage(data.message);
      if (data.correct) {
        setProblem(null);
        setAnswerInput("");
      } else {
        setProblem(data.problem ?? problem);
        setWarning(data.message);
      }
    } catch (err) {
      setWarning(err instanceof Error ? err.message : String(err));
    }
  };

  const screen = useMemo(() => {
    if (!username) return "login" as const;
    return alarmState;
  }, [username, alarmState]);

  if (screen === "login") {
    return (
      <div className="app login">
        <h1>WakeUp</h1>
        <p>Enter a username to begin.</p>
        <form onSubmit={onSubmitUsername}>
          <input
            autoFocus
            placeholder="username"
            value={pendingName}
            onChange={(e) => setPendingName(e.target.value)}
          />
          <button type="submit">Start</button>
        </form>
      </div>
    );
  }

  return (
    <div className={`app screen-${screen}`}>
      <header className="topbar">
        <span>WakeUp</span>
        <span className="user-tag">{username}</span>
      </header>

      {warning && <div className="warning">{warning}</div>}

      {screen === "idle" && (
        <section className="idle">
          <h2>Set a new alarm</h2>
          <div className="presets">
            {PRESETS.map((p) => (
              <button
                key={p.seconds}
                className={chosenSeconds === p.seconds ? "preset selected" : "preset"}
                onClick={() => setChosenSeconds(p.seconds)}
              >
                {p.label}
              </button>
            ))}
          </div>
          <label className="custom">
            Custom seconds:
            <input
              type="number"
              min={1}
              value={chosenSeconds}
              onChange={(e) => setChosenSeconds(parseInt(e.target.value || "0", 10))}
            />
          </label>
          <button className="primary" onClick={onSetAlarm} disabled={chosenSeconds <= 0}>
            Set Alarm
          </button>
          <p className="msg">{statusMessage}</p>
        </section>
      )}

      {screen === "armed" && (
        <section className="armed">
          <h2>Counting down</h2>
          <div className="countdown">{formatMMSS(secondsRemaining)}</div>
          <button className="danger" onClick={onCancel}>Cancel Alarm</button>
          <p className="msg">{statusMessage}</p>
        </section>
      )}

      {screen === "ringing" && (
        <section className="ringing">
          <h2 className="blink">WAKE UP</h2>
          <p>Solve to dismiss:</p>
          <div className="problem">{problem ?? "..."}</div>
          <form onSubmit={onSubmitAnswer} className="answer-form">
            <input
              autoFocus
              type="number"
              placeholder="answer"
              value={answerInput}
              onChange={(e) => setAnswerInput(e.target.value)}
            />
            <button className="primary" type="submit">Submit Answer</button>
          </form>
          <p className="msg">{statusMessage}</p>
        </section>
      )}

      {screen === "dismissed" && (
        <section className="dismissed">
          <h2>Good morning.</h2>
          <p>{statusMessage}</p>
          <button className="primary" onClick={poll}>Set a new alarm</button>
        </section>
      )}
    </div>
  );
}
