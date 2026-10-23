import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import type { ProductStatus } from '@/api/types'
import { ButtonLink } from '@/components/ui/Button'
import { PageHeader } from '@/components/ui/PageHeader'
import { EmptyState, ErrorState, LoadingRows } from '@/components/ui/States'
import { PriceSticker } from '@/features/catalog/PriceSticker'
import { ProductCover } from '@/features/catalog/ProductCover'
import { readProduct } from '@/features/catalog/variant'
import { cn } from '@/lib/cn'
import { productStatusLabel, productTypeLabel } from '@/lib/labels'
import { ProductLifecycle, ProductStatusBadge } from './ProductLifecycle'
import { useMyProducts } from './queries'

const filters: Array<ProductStatus | 'ALL'> = ['ALL', 'PUBLISHED', 'SUSPENDED', 'DISCONTINUED']

export function SellerProductsPage() {
  const { data, isPending, error, refetch } = useMyProducts()
  const [status, setStatus] = useState<ProductStatus | 'ALL'>('ALL')
  const products = useMemo(() => (data?.content ?? []).filter((p) => status === 'ALL' || p.status === status), [data, status])

  return (
    <>
      <PageHeader
        eyebrow="Vendedor"
        title="mis productos"
        description="todo tu catálogo, en cualquier estado. Solo lo publicado y con variantes aparece en la tienda."
        actions={<ButtonLink to="/vendedor/productos/nuevo">Registrar producto</ButtonLink>}
      />

      <fieldset className="mb-5 flex flex-wrap gap-1.5">
        <legend className="sr-only">Filtrar por estado</legend>
        {filters.map((f) => {
          const count = f === 'ALL' ? data?.content.length : data?.content.filter((p) => p.status === f).length
          return (
            <button
              key={f}
              type="button"
              aria-pressed={status === f}
              onClick={() => setStatus(f)}
              className={cn('h-9 rounded-full px-4 text-sm font-semibold ring-1 ring-inset transition-colors', status === f ? 'bg-ink text-white ring-ink' : 'bg-card ring-line-strong hover:ring-ink')}
            >
              {f === 'ALL' ? 'Todos' : productStatusLabel[f]}
              {count !== undefined && <span className="ml-1.5 opacity-70">{count}</span>}
            </button>
          )
        })}
      </fieldset>

      {isPending ? (
        <LoadingRows label="cargando tus productos" />
      ) : error ? (
        <ErrorState error={error} onRetry={() => void refetch()} />
      ) : products.length === 0 ? (
        <EmptyState
          title={status === 'ALL' ? 'aún no registras productos' : 'no hay productos en este estado'}
          description="registra tu primer producto con su nombre, precio y opciones; quedará publicado de inmediato."
          action={<ButtonLink to="/vendedor/productos/nuevo">Registrar producto</ButtonLink>}
        />
      ) : (
        <ul className="grid gap-3">
          {products.map((product) => {
            const view = readProduct(product)
            return (
              <li key={product.id} className="grid grid-cols-[4.5rem_1fr] items-center gap-4 rounded-[var(--radius-card)] bg-card p-3 ring-1 ring-line sm:grid-cols-[5rem_1fr_auto] sm:p-4">
                <div className="relative">
                  <div className="aspect-square overflow-hidden rounded-xl bg-paper" aria-hidden="true">
                    <ProductCover productId={product.id} title={view.title} image={view.image} />
                  </div>
                </div>
                <div className="min-w-0">
                  <Link to={`/vendedor/productos/${encodeURIComponent(product.id)}`} className="text-lg font-bold hover:text-cobalt">
                    {view.title}
                  </Link>
                  <div className="mt-1 flex flex-wrap items-center gap-2 text-sm text-ink-soft">
                    <ProductStatusBadge product={product} />
                    <span>{productTypeLabel[product.productType]}</span>
                    <span aria-hidden="true">·</span>
                    <span>
                      {product.variants.length} {product.variants.length === 1 ? 'variante' : 'variantes'}
                    </span>
                  </div>
                </div>
                <div className="col-span-2 flex flex-wrap items-center justify-between gap-3 sm:col-span-1 sm:justify-end">
                  {view.priceFrom !== null && <PriceSticker amount={view.priceFrom} currency={view.currency} from={view.hasPriceRange} type={product.productType} seed={product.id} className="text-sm" />}
                  <ProductLifecycle product={product} />
                </div>
              </li>
            )
          })}
        </ul>
      )}
    </>
  )
}
