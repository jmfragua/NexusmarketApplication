import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { toast } from 'sonner'
import { configureApiClient, setAuthToken } from '@/api/client'
import type { AuthResponse } from '@/api/types'
import { loadSession, persistSession, SessionContext, sessionFrom, type Session } from './session'

function initialSession(): Session | null {
  const stored = loadSession()
  setAuthToken(stored?.token ?? null)
  return stored
}

export function SessionProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [session, setSession] = useState<Session | null>(initialSession)

  const signOut = useCallback(
    (reason: 'expired' | 'manual' = 'manual') => {
      setAuthToken(null)
      persistSession(null)
      setSession(null)
      // Parallel requests can all be rejected at once; the fixed id shows the notice only once.
      if (reason === 'expired') toast.error('tu sesión expiró, vuelve a ingresar para continuar', { id: 'session-expired' })
      // Cleared after React commits the signed-out tree: clearing first would make the still
      // mounted protected pages refetch, without a token, before they unmount.
      window.setTimeout(() => queryClient.clear(), 0)
    },
    [queryClient],
  )

  const signIn = useCallback(
    (auth: AuthResponse) => {
      const next = sessionFrom(auth)
      queryClient.clear()
      setAuthToken(next.token)
      persistSession(next)
      setSession(next)
      return next
    },
    [queryClient],
  )

  useEffect(() => {
    configureApiClient({ onUnauthorized: () => signOut('expired') })
  }, [signOut])

  useEffect(() => {
    if (!session?.expiresAt) return
    // An already expired session never gets here (loadSession drops it), so waiting is enough.
    // setTimeout overflows past ~24.8 days; tokens here last minutes, but stay safe.
    const wait = Math.max(0, Math.min(session.expiresAt - Date.now(), 2_147_000_000))
    const timer = window.setTimeout(() => signOut('expired'), wait)
    return () => window.clearTimeout(timer)
  }, [session, signOut])

  const value = useMemo(() => ({ session, signIn, signOut }), [session, signIn, signOut])
  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>
}
