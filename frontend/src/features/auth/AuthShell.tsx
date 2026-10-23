import type { CSSProperties, ReactNode } from 'react'
import { ProductCover } from '@/features/catalog/ProductCover'

// Decorative covers for the panel: six different compositions and palettes, each shown whole in its
// own 4:3 tile (the ratio they are drawn in), so nothing is cropped away.
const MOSAIC = ['acceso-3', 'acceso-2', 'acceso-16', 'acceso-17', 'acceso-8', 'acceso-14']

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
 * Two columns, form and panel, centred together between the header and the footer. The panel is
 * as tall as its content (headline, covers, stickers), so it never shows an empty stretch, and it
 * is centred against the form, however tall that is.
 */
export function AuthShell({ title, description, children, aside }: AuthShellProps) {
  return (
    <div className="auth-shell w-full">
      <div className="mx-auto grid w-full max-w-6xl gap-10 px-4 py-10 sm:px-6 md:grid-cols-[1fr_0.95fr] md:items-center md:gap-12 md:py-12">
        <div className="motion-safe:animate-rise">
          <h1 className="text-[clamp(2.2rem,5vw,3.4rem)] font-extrabold">{title}</h1>
          <p className="mt-3 max-w-md text-lg text-ink-soft">{description}</p>
          <div className="mt-7 max-w-md">{children}</div>
        </div>
        <aside className="relative hidden self-center overflow-hidden rounded-[1.8rem] bg-ink p-7 text-white md:block" aria-hidden="true">
          <div className="grain absolute inset-0 opacity-30" />
          <div className="relative grid gap-5">
            {aside && <div className="font-display text-[2rem] leading-[1.08] font-bold tracking-[-0.01em]">{aside}</div>}
            <div className="grid grid-cols-3 gap-3">
              {MOSAIC.map((id) => (
                <div key={id} className="aspect-[4/3] overflow-hidden rounded-2xl ring-2 ring-white/15">
                  <ProductCover productId={id} title="" image={null} />
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
