import { Link } from 'react-router-dom'
import type { ProductResponse } from '@/api/types'
import { cn } from '@/lib/cn'
import { formatMoney } from '@/lib/money'
import { heroTiles, type HeroTile } from './heroTiles'
import { ProductCover } from './ProductCover'
import { readProduct } from './variant'

// The collage of the desktop hero: a fan of three cards side by side, the middle one raised. They
// only touch at their edges (a sliver of each card), so every name and price is fully visible.
const collage = [
  { tile: 'md:left-0 md:top-[26px]', art: 'md:-rotate-[2.5deg]' },
  { tile: 'md:left-[33%] md:top-0', art: 'md:rotate-[1.5deg]' },
  { tile: 'md:left-[66%] md:top-[38px]', art: 'md:rotate-[2.5deg]' },
]

function TileCard({ tile, className, artClassName }: { tile: HeroTile; className: string; artClassName: string }) {
  const view = tile.product ? readProduct(tile.product) : null
  const price = view?.priceFrom ?? null
  // Only the picture is tilted: rotated text is resampled and looks blurry, so the card and its
  // caption stay upright.
  const body = (
    <span className="block rounded-[1.1rem] bg-white p-2 shadow-[5px_6px_0_0_var(--color-ink)] transition-transform duration-200 motion-safe:group-hover:-translate-y-1">
      <span className={cn('block aspect-[4/3] overflow-hidden rounded-[0.8rem] bg-paper [backface-visibility:hidden]', artClassName)}>
        <ProductCover productId={tile.id} title={view?.title ?? ''} image={view?.image ?? null} alt="" />
      </span>
      <span className="block px-1 pt-2 pb-0.5 md:pt-2.5">
        {view ? (
          <>
            <span className="line-clamp-2 min-h-[2.1em] text-[0.8125rem] leading-tight font-bold text-ink md:text-[0.95rem]">{view.title}</span>
            <span className="mt-0.5 hidden truncate text-sm font-semibold text-ink md:block">
              {price === null ? 'precio por definir' : `${view.hasPriceRange ? 'desde ' : ''}${formatMoney(price, view.currency)}`}
            </span>
          </>
        ) : (
          <span className="block h-[2.1em] text-[0.8125rem] md:h-[3.6rem]" aria-hidden="true" />
        )}
      </span>
    </span>
  )
  return (
    <li className={cn('motion-safe:animate-rise md:absolute md:w-[34%]', className)}>
      {tile.product ? (
        <Link to={`/productos/${encodeURIComponent(tile.product.id)}`} className="group block rounded-[1.1rem]">
          {body}
        </Link>
      ) : (
        <span className="block" aria-hidden="true">
          {body}
        </span>
      )}
    </li>
  )
}

export function CatalogHero({ featured }: { featured: ProductResponse[] }) {
  const tiles = heroTiles(featured)
  return (
    <section className="relative overflow-hidden bg-cobalt text-white" aria-labelledby="hero-title">
      <div className="grain pointer-events-none absolute inset-0 opacity-25" aria-hidden="true" />
      <div className="relative mx-auto grid max-w-7xl items-center gap-8 px-4 pt-9 pb-14 sm:px-6 md:min-h-[420px] md:grid-cols-[1.1fr_1fr] md:gap-6 md:pt-6 md:pb-9">
        <div className="motion-safe:animate-rise">
          <h1 id="hero-title" className="text-[clamp(2.4rem,5vw,4rem)] leading-[0.98] font-extrabold tracking-[-0.012em] text-white">
            muchos vendedores,{' '}
            <span className="relative inline-block">
              <span className="relative z-10 text-ink">un solo carrito.</span>
              <span className="absolute inset-x-[-0.12em] bottom-[0.04em] top-[0.2em] -z-0 -rotate-1 rounded-[0.2em] bg-lime" aria-hidden="true" />
            </span>
          </h1>
          <p className="mt-5 max-w-lg text-lg text-white/90">
            compra productos físicos y digitales de distintas tiendas, paga una vez y sigue cada pedido hasta que llegue.
          </p>
          <div className="mt-6 flex flex-wrap gap-3">
            <a href="#catalogo" className="inline-flex h-13 items-center rounded-full bg-lime px-7 font-semibold text-ink ring-1 ring-ink/10 transition-transform hover:-translate-y-0.5">
              Explorar el catálogo
            </a>
            <Link to="/registro" className="inline-flex h-13 items-center rounded-full px-6 font-semibold text-white ring-1 ring-white/50 transition-colors hover:bg-white/10">
              Crear una cuenta
            </Link>
          </div>
        </div>

        <ul aria-label="Productos destacados" className="hidden grid-cols-3 gap-3 sm:grid md:relative md:mx-auto md:block md:h-[262px] md:w-full md:max-w-[580px]">
          {tiles.map((tile, i) => (
            <TileCard key={tile.id} tile={tile} className={collage[i]?.tile ?? ''} artClassName={collage[i]?.art ?? ''} />
          ))}
        </ul>
      </div>
      <svg className="block h-6 w-full text-paper" viewBox="0 0 1200 24" preserveAspectRatio="none" aria-hidden="true">
        <path d="M0 24h1200V10c-60 0-60 10-120 10S1020 10 960 10 900 20 840 20 780 10 720 10 660 20 600 20 540 10 480 10 420 20 360 20 300 10 240 10 180 20 120 20 60 10 0 10z" fill="currentColor" />
      </svg>
    </section>
  )
}
