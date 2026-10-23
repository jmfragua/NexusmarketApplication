import type { CSSProperties } from 'react'
import { cn } from '@/lib/cn'
import { formatMoney } from '@/lib/money'
import { stickerTone } from './cover'

interface PriceStickerProps {
  amount: number | null
  currency: string
  /** The variants do not share a price: "desde" is printed above the sticker, never inside it. */
  from?: boolean
  /** The product id: it decides the tilt and a tone that stands out on the cover below. */
  seed?: string
  size?: 'md' | 'lg'
  className?: string
}

function seedNumber(seed: string): number {
  let n = 0
  for (const ch of seed) n = (n * 31 + ch.charCodeAt(0)) >>> 0
  return n
}

/**
 * The price tag. A product with no price yet gets a neutral outlined tag that says so, so it can
 * never be mistaken for an offer; "desde" is a small label above the price, never below 12px.
 * `className` positions the whole block (label and tag).
 */
export function PriceSticker({ amount, currency, from = false, seed = '', size = 'md', className }: PriceStickerProps) {
  const priced = amount !== null
  const tilt = priced ? `${-9 + (seedNumber(seed) % 8)}deg` : '-2deg'
  return (
    <span className={cn('inline-flex flex-col items-end gap-1.5', className)}>
      {priced && from && <span className="rounded-full bg-card px-2.5 py-0.5 text-xs font-semibold text-ink ring-1 ring-ink/20">desde</span>}
      <span
        className={cn('sticker motion-safe:animate-pop', priced ? (size === 'lg' ? 'text-2xl' : 'text-[1.05rem]') : size === 'lg' ? 'text-base' : 'text-sm')}
        data-tone={priced ? stickerTone(seed) : 'neutral'}
        style={{ '--sticker-tilt': tilt } as CSSProperties}
      >
        {priced ? formatMoney(amount, currency) : 'precio por definir'}
      </span>
    </span>
  )
}
