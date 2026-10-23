import { Link } from 'react-router-dom'
import type { ProductResponse } from '@/api/types'
import { Badge } from '@/components/ui/Badge'
import { PriceSticker } from './PriceSticker'
import { ProductCover } from './ProductCover'
import { readProduct } from './variant'

function TypeIcon({ digital }: { digital: boolean }) {
  return (
    <svg viewBox="0 0 20 20" className="size-4 shrink-0" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinejoin="round" strokeLinecap="round" aria-hidden="true">
      {digital ? <path d="M11 2 4.5 11H10l-1 7 6.5-9H10l1-7Z" /> : <path d="m10 2.5 6.5 3.2v8.6L10 17.5l-6.5-3.2V5.7L10 2.5Zm0 6.7 6.5-3.5M10 9.2v8.3m0-8.3L3.5 5.7" />}
    </svg>
  )
}

export function ProductCard({ product, index = 0 }: { product: ProductResponse; index?: number }) {
  const view = readProduct(product)
  const digital = product.productType === 'DIGITAL'
  const options = view.variants.length
  // The brand and, when there are several variants, how many: secondary data on one line, so the
  // type and shipping row at the bottom lines up across the cards of a row.
  const secondary = [view.brand, options > 1 ? `${options} opciones` : null].filter(Boolean).join(' · ')
  return (
    <li className="motion-safe:animate-rise" style={{ animationDelay: `${Math.min(index, 8) * 45}ms` }}>
      <Link
        to={`/productos/${encodeURIComponent(product.id)}`}
        className="group flex h-full flex-col rounded-[var(--radius-card)] bg-card p-2.5 ring-1 ring-line transition-[box-shadow,transform] duration-200 hover:shadow-[0_18px_30px_-18px_rgba(11,18,51,0.5)] hover:ring-ink/40 focus-visible:shadow-[0_18px_30px_-18px_rgba(11,18,51,0.5)] motion-safe:hover:-translate-y-1 motion-safe:focus-visible:-translate-y-1"
      >
        <div className="relative">
          <div className="aspect-[4/3] overflow-hidden rounded-[0.9rem] bg-paper">
            <ProductCover
              productId={product.id}
              title={view.title}
              image={view.image}
              className="transition-transform duration-500 motion-safe:group-hover:scale-[1.03]"
            />
          </div>
          <PriceSticker
            amount={view.priceFrom}
            currency={view.currency}
            from={view.hasPriceRange}
            seed={product.id}
            className="absolute right-2.5 -bottom-3"
          />
        </div>
        <div className="flex flex-1 flex-col px-1.5 pt-5 pb-1.5">
          <h3 className="text-lg leading-snug font-bold group-hover:text-cobalt">{view.title}</h3>
          {secondary && <p className="mt-0.5 text-sm text-ink-soft">{secondary}</p>}
          <div className="mt-auto flex flex-wrap items-center gap-x-2 gap-y-1 pt-3.5">
            <Badge tone={digital ? 'lime' : 'cobalt'} className="gap-1">
              <TypeIcon digital={digital} />
              {digital ? 'Digital' : 'Físico'}
            </Badge>
            <span className="text-sm text-ink-soft">{digital ? 'Entrega inmediata al pagar' : 'Envío desde bodega'}</span>
          </div>
        </div>
      </Link>
    </li>
  )
}
