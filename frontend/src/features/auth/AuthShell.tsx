import type { CSSProperties, ReactNode } from 'react'
import { ProductCover } from '@/features/catalog/ProductCover'

// Six real photos of the catalogue, kept in public/productos/. The id is only the seed of the
// generated poster a tile falls back to when its photo cannot load.
const MOSAIC = [
  { id: 'acceso-3', image: '/productos/morral.jpg' },
  { id: 'acceso-2', image: '/productos/audifonos.jpg' },
  { id: 'acceso-16', image: '/productos/cafe.jpg' },
  { id: 'acceso-17', image: '/productos/tenis.jpg' },
  { id: 'acceso-8', image: '/productos/lampara.jpg' },
  { id: 'acceso-14', image: '/productos/reloj.jpg' },
]

const STICKERS: Array<{ label: string; tone: string; tilt: string }> = [
  { label: 'pago único', tone: 'lime', tilt: '-4deg' },
  { label: 'envío rastreado', tone: 'coral', tilt: '3deg' },
  { label: 'muchas tiendas', tone: 'cobalt', tilt: '-2deg' },
]

interface AuthShellProps {
  title: string
  description: ReactNode
  children: ReactNode
  /** The line shown at the top of the dark panel. */
  aside?: ReactNode
}

/**
 * Two columns, form and panel, centred together between the header and the footer. The columns are
 * sized to their content (the form is never wider than 28rem), so the gap between them is the grid
 * gap and nothing else. The panel is as tall as its content, starts level with the headline and
 * sticks under the header while a long form scrolls.
 */
export function AuthShell({ title, description, children, aside }: AuthShellProps) {
  return (
    <div className="auth-shell w-full">
      <div className="mx-auto grid w-full max-w-6xl gap-10 px-4 py-10 sm:px-6 md:grid-cols-[minmax(0,28rem)_minmax(0,32rem)] md:items-start md:justify-center md:gap-16 md:py-12 lg:gap-24">
        <div className="motion-safe:animate-rise">
          <h1 className="text-[clamp(2.2rem,5vw,3.4rem)] font-extrabold">{title}</h1>
          <p className="mt-3 max-w-md text-lg text-ink-soft">{description}</p>
          <div className="mt-7 max-w-md">{children}</div>
        </div>
        <aside className="relative hidden self-start overflow-hidden rounded-[1.8rem] bg-ink p-7 text-white md:sticky md:top-24 md:block" aria-hidden="true">
          <div className="grain absolute inset-0 opacity-30" />
          <div className="relative grid gap-5">
            {aside && <div className="font-display text-[2rem] leading-[1.08] font-bold tracking-[-0.01em]">{aside}</div>}
            <div className="grid grid-cols-3 gap-3">
              {MOSAIC.map((tile) => (
                <div key={tile.id} className="aspect-[4/3] overflow-hidden rounded-2xl bg-white/10 ring-2 ring-white/15">
                  <ProductCover productId={tile.id} title="" image={tile.image} alt="" />
                </div>
              ))}
            </div>
            <div className="flex flex-wrap gap-x-3 gap-y-4 pt-1">
              {STICKERS.map((sticker) => (
                <span key={sticker.label} className="sticker text-base" data-tone={sticker.tone} style={{ '--sticker-tilt': sticker.tilt } as CSSProperties}>
                  {sticker.label}
                </span>
              ))}
            </div>
          </div>
        </aside>
      </div>
    </div>
  )
}
