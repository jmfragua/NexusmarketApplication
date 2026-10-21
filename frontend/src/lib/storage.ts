// localStorage can be missing or throw (private mode, blocked site data): every access is guarded
// and the app keeps working from memory alone.

export function readJson<T>(key: string, isValid: (value: unknown) => value is T): T | null {
  try {
    const raw = window.localStorage.getItem(key)
    if (raw === null) return null
    const parsed: unknown = JSON.parse(raw)
    return isValid(parsed) ? parsed : null
  } catch {
    return null
  }
}

export function writeJson(key: string, value: unknown): void {
  try {
    window.localStorage.setItem(key, JSON.stringify(value))
  } catch {
    // Storage unavailable: the value lives in memory for this tab only.
  }
}

export function removeKey(key: string): void {
  try {
    window.localStorage.removeItem(key)
  } catch {
    // Nothing to clean when storage is unavailable.
  }
}
