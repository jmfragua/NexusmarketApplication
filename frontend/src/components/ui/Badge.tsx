import type { ReactNode } from 'react'
import { cn } from '@/lib/cn'

export type Tone = 'neutral' | 'cobalt' | 'lime' | 'coral' | 'mint' | 'amber'

const tones: Record<Tone, string> = {
  neutral: 'bg-ink/6 text-ink-soft',
  cobalt: 'bg-cobalt-wash text-cobalt-deep',
  lime: 'bg-lime-wash text-ink',
  coral: 'bg-coral-wash text-coral-deep',
  mint: 'bg-mint-wash text-mint',
  amber: 'bg-amber-wash text-amber-ink',
}

export function Badge({ tone = 'neutral', children, className }: { tone?: Tone; children: ReactNode; className?: string }) {
  return (
    <span className={cn('inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold', tones[tone], className)}>
      {children}
    </span>
  )
}
