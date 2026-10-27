import { Link } from 'react-router-dom'
import type { ProductResponse } from '@/api/types'
import { sameStore, storeHref } from './catalogFilters'
import { ProductCard } from './ProductCard'
import { useCatalog } from './queries'
import { readProduct } from './variant'

const crumbLink = 'rounded-sm font-semibold text-ink-soft underline-offset-4 hover:text-ink hover:underline'

/** Catálogo › marca › product. */
export function Breadcrumbs({ brand, title }: { brand: string | null; title: string }) {
  return (
    <nav aria-label="Ruta de navegación">
      <ol className="flex flex-wrap items-center gap-x-2 gap-y-1 text-sm">
        <li>
          <Link to="/" className={crumbLink}>
            Catálogo
          </Link>
        </li>
        {brand && (
          <li className="flex items-center gap-2">
            <span aria-hidden="true" className="text-ink-faint">
              ›
            </span>
            <Link to={storeHref(brand)} className={crumbLink}>
              {brand}
            </Link>
          </li>
        )}
        <li className="flex min-w-0 items-center gap-2">
          <span aria-hidden="true" className="text-ink-faint">
            ›
          </span>
          <span aria-current="page" className="truncate font-semibold text-ink">
            {title}
          </span>
        </li>
      </ol>
    </nav>
  )
}

const icon = 'size-5 shrink-0 text-cobalt'

function TruckIcon() {
  return (
    <svg viewBox="0 0 24 24" className={icon} fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" aria-hidden="true">
      <path d="M3 6.5h11v9H3zM14 9.5h4l3 3v3h-7z" />
      <circle cx="7" cy="17.5" r="1.8" fill="var(--color-card)" />
      <circle cx="17.5" cy="17.5" r="1.8" fill="var(--color-card)" />
    </svg>
  )
}

function BoltIcon() {
  return (
    <svg viewBox="0 0 24 24" className={icon} fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" aria-hidden="true">
      <path d="M13 2.5 5.5 13.5H12l-1 8 7.5-11H12z" />
    </svg>
  )
}

function CardIcon() {
  return (
    <svg viewBox="0 0 24 24" className={icon} fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" strokeLinecap="round" aria-hidden="true">
      <rect x="3" y="5.5" width="18" height="13" rx="2.5" />
      <path d="M3 10h18M7 15h4" />
    </svg>
  )
}

function ReturnIcon() {
  return (
    <svg viewBox="0 0 24 24" className={icon} fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" strokeLinecap="round" aria-hidden="true">
      <path d="M9 7 4.5 11.5 9 16" />
      <path d="M4.5 11.5H15a4.5 4.5 0 0 1 0 9h-2" />
    </svg>
  )
}

/** Three short promises, all true of how the store works. */
export function TrustStrip({ digital }: { digital: boolean }) {
  const items = [
    { icon: digital ? <BoltIcon /> : <TruckIcon />, text: digital ? 'entrega inmediata al pagar' : 'envío rastreado' },
    { icon: <CardIcon />, text: 'pago único para todo tu carrito' },
    { icon: <ReturnIcon />, text: 'devoluciones cuando el pedido esté entregado' },
  ]
  return (
    <ul className="grid gap-2.5 rounded-[var(--radius-card)] bg-cobalt-wash/50 p-4 text-sm sm:grid-cols-3 sm:gap-3" aria-label="Lo que incluye tu compra">
      {items.map((item) => (
        <li key={item.text} className="flex items-center gap-2.5 font-semibold text-ink sm:flex-col sm:items-start sm:gap-1.5">
          {item.icon}
          {item.text}
        </li>
      ))}
    </ul>
  )
}

/**
 * Up to four other products of the same brand, from the catalogue the store already loads; when
 * the brand has no others, four other products of the catalogue instead.
 */
export function MoreFromBrand({ product, brand }: { product: ProductResponse; brand: string | null }) {
  const { data } = useCatalog()
  const others = (data?.products ?? []).filter((p) => p.id !== product.id)
  const sameBrand = brand ? others.filter((p) => sameStore(readProduct(p).brand ?? '', brand)) : []
  const shown = (sameBrand.length > 0 ? sameBrand : others).slice(0, 4)
  if (shown.length === 0) return null
  const title = sameBrand.length > 0 ? `más de ${brand}` : 'también te puede gustar'
  return (
    <section aria-labelledby="mas-productos" className="mt-16">
      <div className="mb-5 flex flex-wrap items-baseline justify-between gap-3">
        <h2 id="mas-productos" className="text-2xl font-extrabold sm:text-3xl">
          {title}
        </h2>
        {sameBrand.length > 0 && brand && (
          <Link to={storeHref(brand)} className="rounded-sm font-semibold text-cobalt underline-offset-4 hover:underline">
            Ver todo de {brand}
          </Link>
        )}
      </div>
      <ul className="grid grid-cols-1 gap-x-5 gap-y-7 min-[480px]:grid-cols-2 lg:grid-cols-4">
        {shown.map((p, i) => (
          <ProductCard key={p.id} product={p} index={i} />
        ))}
      </ul>
    </section>
  )
}
