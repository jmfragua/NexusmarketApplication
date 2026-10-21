import { Link } from 'react-router-dom'
import type { ProductResponse } from '@/api/types'
import { Badge } from '@/components/ui/Badge'
import { PriceSticker } from './PriceSticker'
import { ProductCover } from './ProductCover'
import { readProduct } from './variant'

export function ProductCard({ product, index = 0 }: { product: ProductResponse; index?: number }) {
  const view = readProduct(product)
  const options = view.variants.length
  return (
    <li className="motion-safe:animate-rise" style={{ animationDelay: `${Math.min(index, 8) * 45}ms` }}>
      <Link
        to={`/productos/${encodeURIComponent(product.id)}`}
        className="group flex h-full flex-col rounded-[var(--radius-card)] bg-card p-2.5 ring-1 ring-line transition-[box-shadow,transform] duration-200 hover:ring-2 hover:ring-ink motion-safe:hover:-translate-y-0.5"
      >
        <div className="relative">
          <div className="aspect-[4/3] overflow-hidden min-[480px]:aspect-[4/3.4] rounded-[0.9rem] bg-paper">
            <ProductCover
              productId={product.id}
              title={view.title}
              image={view.image}
              className="transition-transform duration-500 motion-safe:group-hover:scale-[1.03]"
            />
          </div>
          {product.productType === 'DIGITAL' && (
            <Badge tone="lime" className="absolute top-2.5 left-2.5 ring-1 ring-ink/10">
              Digital · entrega inmediata
            </Badge>
          )}
          <PriceSticker
            amount={view.priceFrom}
            currency={view.currency}
            from={view.hasPriceRange}
            seed={product.id}
            className="absolute right-3 -bottom-5"
          />
        </div>
        <div className="flex flex-1 flex-col gap-0.5 px-1.5 pt-4 pb-1.5">
          {view.brand && <p className="pr-28 text-xs font-semibold tracking-wide text-ink-faint">{view.brand}</p>}
          <h3 className="text-[1.08rem] leading-snug font-bold group-hover:text-cobalt">{view.title}</h3>
          <p className="mt-auto pt-1 text-sm text-ink-soft">{options > 1 ? `${options} opciones` : 'una opción'}</p>
        </div>
      </Link>
    </li>
  )
}
