import { createContext, useContext } from 'react'
import { ROLES, type AuthResponse, type Role } from '@/api/types'
import { readJson, removeKey, writeJson } from '@/lib/storage'

export interface Session {
  token: string
  userId: string
  email: string
  role: Role
  /** Epoch milliseconds read from the token `exp` claim, when present. */
  expiresAt: number | null
}

const STORAGE_KEY = 'nexusmarket.session'

export const roleHome: Record<Role, string> = {
  BUYER: '/',
  SELLER: '/vendedor',
  LOGISTICS_OPERATOR: '/logistica',
  ADMINISTRATOR: '/admin',
  SUPERVISOR: '/supervisor',
}

function decodeExpiry(token: string): number | null {
  const payload = token.split('.')[1]
  if (!payload) return null
  try {
    const json: unknown = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')))
    if (typeof json === 'object' && json !== null && 'exp' in json && typeof json.exp === 'number') {
      return json.exp * 1000
    }
  } catch {
    // An unreadable payload is not fatal: the backend still rejects an expired token with 401.
  }
  return null
}

export function sessionFrom(auth: AuthResponse): Session {
  return {
    token: auth.token,
    userId: auth.userId,
    email: auth.email,
    role: auth.role,
    expiresAt: decodeExpiry(auth.token),
  }
}

function isSession(value: unknown): value is Session {
  if (typeof value !== 'object' || value === null) return false
  const v = value as Record<string, unknown>
  return (
    typeof v.token === 'string' &&
    typeof v.userId === 'string' &&
    typeof v.email === 'string' &&
    typeof v.role === 'string' &&
    (ROLES as readonly string[]).includes(v.role) &&
    (v.expiresAt === null || typeof v.expiresAt === 'number')
  )
}

export function isExpired(session: Session, now: number = Date.now()): boolean {
  return session.expiresAt !== null && session.expiresAt <= now
}

export function loadSession(): Session | null {
  const stored = readJson(STORAGE_KEY, isSession)
  if (!stored || isExpired(stored)) {
    if (stored) removeKey(STORAGE_KEY)
    return null
  }
  return stored
}

export function persistSession(session: Session | null): void {
  if (session) writeJson(STORAGE_KEY, session)
  else removeKey(STORAGE_KEY)
}

export interface SessionContextValue {
  session: Session | null
  signIn: (auth: AuthResponse) => Session
  signOut: (reason?: 'expired' | 'manual') => void
}

export const SessionContext = createContext<SessionContextValue | null>(null)

export function useSession(): SessionContextValue {
  const value = useContext(SessionContext)
  if (!value) throw new Error('useSession must be used inside <SessionProvider>')
  return value
}
