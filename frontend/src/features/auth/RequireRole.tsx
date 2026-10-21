import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import type { Role } from '@/api/types'
import { roleHome, useSession } from './session'

/**
 * Each user holds exactly one role and may not operate outside it (RG-02, RG-03): a route of
 * another role is never rendered, the user is sent back to their own home instead.
 */
export function RequireRole({ allow, children }: { allow: Role; children: ReactNode }) {
  const { session } = useSession()
  const location = useLocation()
  if (!session) {
    return <Navigate to="/ingresar" replace state={{ from: location.pathname + location.search }} />
  }
  if (session.role !== allow) return <Navigate to={roleHome[session.role]} replace />
  return <>{children}</>
}

/** Login and sign-up make no sense with an open session. */
export function GuestOnly({ children }: { children: ReactNode }) {
  const { session } = useSession()
  if (session) return <Navigate to={roleHome[session.role]} replace />
  return <>{children}</>
}
