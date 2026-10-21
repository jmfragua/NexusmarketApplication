import type { CSSProperties } from 'react'
import { cn } from '@/lib/cn'
import { formatMoney } from '@/lib/money'

interface PriceStickerProps {
  amount: number | null
  currency: string
  /** Prefix "desde" when the variants do not share a price. */
  from?: boolean
  seed?: string
  size?: 'md' | 'lg'
  className?: string
}

const tones = ['coral', 'lime', 'coral', 'cobalt'] as const

function seedNumber(seed: string): number {
  let n = 0
  for (const ch of seed) n = (n * 31 + ch.charCodeAt(0)) >>> 0
  return n
}

export function PriceSticker({ amount, currency, from = false, seed = '', size = 'md', className }: PriceStickerProps) {
  const n = seedNumber(seed)
  const tone = tones[n % tones.length]
  const tilt = `${-10 + (n % 9)}deg`
  const label = amount === null ? 'sin precio' : formatMoney(amount, currency)
  return (
    <span
      className={cn('sticker motion-safe:animate-pop', size === 'lg' ? 'text-2xl' : 'text-[1.05rem]', className)}
      data-tone={tone}
      style={{ '--sticker-tilt': tilt } as CSSProperties}
    >
      {from && amount !== null && <span className="mb-0.5 text-[0.62em] font-semibold tracking-wide opacity-80">desde</span>}
      <span>{label}</span>
    </span>
  )
}
