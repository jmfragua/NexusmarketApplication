import type { CSSProperties } from 'react'
import { Link } from 'react-router-dom'
import type { ProductResponse } from '@/api/types'
import { PriceSticker } from './PriceSticker'
import { ProductCover } from './ProductCover'
import { readProduct } from './variant'

// Each tile is its own stacking context (it is rotated), so a sticker can only overlap the tiles
// drawn before it; the first one sits on the free top-left corner instead of under its neighbours.
const stickerSpots = ['-left-4 -top-4', '-right-3 -bottom-4', '-right-3 -bottom-4']

const tiles = [
  'left-[4%] top-[8%] w-[46%] -rotate-6',
  'right-[2%] top-[0%] w-[44%] rotate-[5deg]',
  'left-[26%] bottom-[0%] w-[48%] rotate-[-1.5deg]',
]

export function CatalogHero({ featured }: { featured: ProductResponse[] }) {
  return (
    <section className="relative overflow-hidden bg-cobalt text-white" aria-labelledby="hero-title">
      <div className="grain pointer-events-none absolute inset-0 opacity-25" aria-hidden="true" />
      <div className="relative mx-auto grid max-w-7xl items-center gap-10 px-4 pt-12 pb-16 sm:px-6 md:grid-cols-[1.15fr_1fr] md:pt-16 md:pb-20">
        <div className="motion-safe:animate-rise">
          <p className="mb-5 inline-flex items-center gap-2 rounded-full bg-white/10 px-3 py-1 text-sm font-semibold ring-1 ring-white/20">
            <span className="size-2 rounded-full bg-lime" aria-hidden="true" />
            vendedores independientes, envíos rastreados
          </p>
          <h1 id="hero-title" className="text-[clamp(2.7rem,8.5vw,5.6rem)] leading-[0.92] font-extrabold tracking-[-0.035em] text-white">
            muchos vendedores,{' '}
            <span className="relative inline-block">
              <span className="relative z-10 text-ink">un solo carrito.</span>
              <span className="absolute inset-x-[-0.12em] bottom-[0.06em] top-[0.18em] -z-0 -rotate-1 rounded-[0.2em] bg-lime" aria-hidden="true" />
            </span>
          </h1>
          <p className="mt-6 max-w-lg text-lg text-white/85">
            compra productos físicos y digitales de distintas tiendas, paga una vez y sigue cada pedido hasta que llegue.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <a href="#catalogo" className="inline-flex h-13 items-center rounded-full bg-lime px-7 font-semibold text-ink ring-1 ring-ink/10 transition-transform hover:-translate-y-0.5">
              Explorar el catálogo
            </a>
            <Link to="/registro" className="inline-flex h-13 items-center rounded-full px-6 font-semibold text-white ring-1 ring-white/40 transition-colors hover:bg-white/10">
              Crear una cuenta
            </Link>
          </div>
        </div>

        <div className="relative mx-auto aspect-[1/0.95] w-full max-w-md md:max-w-none" aria-hidden="true">
          {featured.slice(0, 3).map((product, i) => {
            const view = readProduct(product)
            return (
              <div
                key={product.id}
                className={`absolute ${tiles[i] ?? ''} motion-safe:animate-rise`}
                style={{ animationDelay: `${120 + i * 110}ms` } as CSSProperties}
              >
                <div className="overflow-hidden rounded-[1.1rem] bg-white p-1.5 shadow-[6px_8px_0_0_var(--color-ink)]">
                  <div className="aspect-square overflow-hidden rounded-[0.8rem]">
                    <ProductCover productId={product.id} title={view.title} image={view.image} />
                  </div>
                </div>
                <PriceSticker amount={view.priceFrom} currency={view.currency} seed={product.id + i} className={`absolute ${stickerSpots[i] ?? ''}`} />
              </div>
            )
          })}
        </div>
      </div>
      <svg className="block h-6 w-full text-paper" viewBox="0 0 1200 24" preserveAspectRatio="none" aria-hidden="true">
        <path d="M0 24h1200V10c-60 0-60 10-120 10S1020 10 960 10 900 20 840 20 780 10 720 10 660 20 600 20 540 10 480 10 420 20 360 20 300 10 240 10 180 20 120 20 60 10 0 10z" fill="currentColor" />
      </svg>
    </section>
  )
}
