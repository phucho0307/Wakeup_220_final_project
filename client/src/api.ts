export type AlarmState = "idle" | "armed" | "ringing" | "dismissed";

export interface StatusResponse {
  user: string;
  state: AlarmState;
  secondsRemaining: number;
  problem: string | null;
  message: string;
}

export interface SolveResponse {
  user: string;
  state: AlarmState;
  correct: boolean;
  problem?: string | null;
  message: string;
}

export interface ErrorResponse {
  status: "error";
  message: string;
}

const BASE_URL = "http://localhost:8080";

async function parseJson<T>(res: Response): Promise<T> {
  const text = await res.text();
  const data = text ? JSON.parse(text) : {};
  if (!res.ok) {
    const message = (data && data.message) || `HTTP ${res.status}`;
    throw new Error(message);
  }
  return data as T;
}

export async function getStatus(user: string): Promise<StatusResponse> {
  const res = await fetch(
    `${BASE_URL}/alarm/status?user=${encodeURIComponent(user)}`
  );
  return parseJson<StatusResponse>(res);
}

export async function setAlarm(user: string, seconds: number): Promise<StatusResponse> {
  const res = await fetch(
    `${BASE_URL}/alarm/set?user=${encodeURIComponent(user)}&seconds=${seconds}`,
    { method: "POST" }
  );
  return parseJson<StatusResponse>(res);
}

export async function solveAlarm(user: string, answer: number): Promise<SolveResponse> {
  const res = await fetch(
    `${BASE_URL}/alarm/solve?user=${encodeURIComponent(user)}`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ answer }),
    }
  );
  return parseJson<SolveResponse>(res);
}

export async function cancelAlarm(user: string): Promise<StatusResponse> {
  const res = await fetch(
    `${BASE_URL}/alarm/cancel?user=${encodeURIComponent(user)}`,
    { method: "POST" }
  );
  return parseJson<StatusResponse>(res);
}
