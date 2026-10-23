import type { ReactNode } from 'react'
import { ApiError } from '@/api/client'
import { describeError } from '@/api/errors'
import { cn } from '@/lib/cn'
import { Button } from './Button'

export function Skeleton({ className }: { className?: string }) {
  return <div className={cn('skeleton rounded-xl', className)} aria-hidden="true" />
}

export function LoadingRows({ rows = 4, label = 'cargando' }: { rows?: number; label?: string }) {
  return (
    <output aria-live="polite" className="grid gap-3">
      <span className="sr-only">{label}…</span>
      {Array.from({ length: rows }, (_, i) => (
        <Skeleton key={i} className="h-16" />
      ))}
    </output>
  )
}

interface EmptyStateProps {
  title: string
  description?: ReactNode
  action?: ReactNode
  art?: ReactNode
  className?: string
}

export function EmptyState({ title, description, action, art, className }: EmptyStateProps) {
  return (
    <div className={cn('grid justify-items-center gap-3 rounded-[var(--radius-card)] border border-dashed border-line-strong bg-card/60 px-6 py-12 text-center', className)}>
      {art ?? (
        <svg width="64" height="64" viewBox="0 0 64 64" aria-hidden="true">
          <circle cx="32" cy="32" r="28" fill="var(--color-lime)" />
          <path d="M20 26h24l-3 18H23z" fill="none" stroke="var(--color-ink)" strokeWidth="3" strokeLinejoin="round" />
          <path d="M26 26a6 6 0 0 1 12 0" fill="none" stroke="var(--color-ink)" strokeWidth="3" />
        </svg>
      )}
      <h2 className="text-xl">{title}</h2>
      {description && <p className="max-w-md text-ink-soft">{description}</p>}
      {action && <div className="mt-2 flex flex-wrap justify-center gap-2">{action}</div>}
    </div>
  )
}

export function ErrorState({ error, onRetry, title }: { error: unknown; onRetry?: () => void; title?: string }) {
  const network = error instanceof ApiError && error.isNetwork
  return (
    <div role="alert" className="grid justify-items-start gap-2 rounded-[var(--radius-card)] border border-coral/40 bg-coral-wash/60 p-5">
      <h2 className="text-lg">{title ?? (network ? 'no hay conexión con el servidor' : 'no pudimos cargar esta información')}</h2>
      <p className="text-ink-soft">
        {network ? 'revisa que el backend esté encendido y vuelve a intentarlo.' : describeError(error)}
      </p>
      {onRetry && (
        <Button variant="outline" size="sm" onClick={onRetry} className="mt-1">
          Reintentar
        </Button>
      )}
    </div>
  )
}

export function Notice({ tone = 'amber', title, children }: { tone?: 'amber' | 'cobalt' | 'coral'; title: string; children?: ReactNode }) {
  const styles = {
    amber: 'bg-amber-wash text-amber-ink border-amber-ink/20',
    cobalt: 'bg-cobalt-wash text-cobalt-deep border-cobalt/25',
    coral: 'bg-coral-wash text-coral-deep border-coral/35',
  }[tone]
  return (
    <div className={cn('rounded-2xl border px-4 py-3 text-sm', styles)} role="note">
      <p className="font-semibold">{title}</p>
      {children && <div className="mt-0.5">{children}</div>}
    </div>
  )
}
