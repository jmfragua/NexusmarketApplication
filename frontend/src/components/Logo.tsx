import { Link } from 'react-router-dom'
import { cn } from '@/lib/cn'

export function Logo({ to = '/', className, inverted = false, compact = false }: { to?: string; className?: string; inverted?: boolean; compact?: boolean }) {
  return (
    <Link to={to} className={cn('inline-flex items-center gap-2 font-display text-xl font-extrabold tracking-tight', className)} aria-label="NexusMarket, ir al inicio">
      <svg viewBox="0 0 32 32" className="size-8" aria-hidden="true">
        <rect width="32" height="32" rx="9" fill={inverted ? '#d4ff3a' : '#1f3fd1'} />
        <path d="M9 23V9h3.2l7 9.1V9H23v14h-3.1l-7.1-9.2V23z" fill={inverted ? '#0b1233' : '#d4ff3a'} />
      </svg>
      <span className={cn(inverted ? 'text-white' : 'text-ink', compact && 'max-[419px]:sr-only')}>
        nexus<span className={inverted ? 'text-lime' : 'text-cobalt'}>market</span>
      </span>
    </Link>
  )
}
