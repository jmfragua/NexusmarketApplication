import { useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { toast } from 'sonner'
import { ApiError } from '@/api/client'
import { Badge } from '@/components/ui/Badge'
import { Button, ButtonLink } from '@/components/ui/Button'
import { QuantityStepper } from '@/components/ui/QuantityStepper'
import { EmptyState, ErrorState, Notice, Skeleton } from '@/components/ui/States'
import { useSession } from '@/features/auth/session'
import { useAddToCart } from '@/features/cart/queries'
import { cn } from '@/lib/cn'
import { formatMoney } from '@/lib/money'
import { storeHref } from './catalogFilters'
import { PriceSticker } from './PriceSticker'
import { Breadcrumbs, MoreFromBrand, TrustStrip } from './ProductExtras'
import { productTone, typeChipRing } from './productTone'
import { ProductCover } from './ProductCover'
import { useProduct } from './queries'
import { isSampleId } from './samples'
import { readProduct, variantLabel } from './variant'

function BackLink() {
  return (
    <Link to="/" className="inline-flex items-center gap-1.5 text-sm font-semibold text-ink-soft hover:text-ink">
      <span aria-hidden="true">←</span> Volver al catálogo
    </Link>
  )
}

export function ProductPage() {
  const { productId = '' } = useParams()
  const { data: product, isPending, error, refetch } = useProduct(productId)
  const { session } = useSession()
  const location = useLocation()
  const navigate = useNavigate()
  const addToCart = useAddToCart()
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [quantity, setQuantity] = useState(1)

  if (isPending) {
    return (
      <output className="mx-auto grid max-w-6xl gap-10 px-4 py-10 sm:px-6 md:grid-cols-2">
        <span className="sr-only">cargando producto…</span>
        <Skeleton className="aspect-square" />
        <div className="grid content-start gap-4">
          <Skeleton className="h-5 w-24" />
          <Skeleton className="h-12 w-3/4" />
          <Skeleton className="h-24" />
        </div>
      </output>
    )
  }

  if (error || !product) {
    const gone = error instanceof ApiError && (error.status === 404 || error.status === 409)
    return (
      <div className="mx-auto max-w-3xl px-4 py-12 sm:px-6">
        <BackLink />
        <div className="mt-6">
          {gone ? (
            <EmptyState
              title={error.status === 404 ? 'este producto no existe' : 'este producto ya no está disponible'}
              description="puede que el vendedor lo haya retirado. Hay mucho más en el catálogo."
              action={<ButtonLink to="/">Ir al catálogo</ButtonLink>}
            />
          ) : (
            <ErrorState error={error} onRetry={() => void refetch()} />
          )}
        </div>
      </div>
    )
  }

  const view = readProduct(product)
  const selected = view.variants.find((v) => v.id === selectedId) ?? view.variants[0] ?? null
  const sample = isSampleId(product.id)
  const isBuyer = session?.role === 'BUYER'
  const price = selected?.price ?? null
  const canBuy = isBuyer && !sample && selected !== null && price !== null

  const add = () => {
    if (!selected) return
    addToCart.mutate(
      { productId: product.id, productVariantId: selected.id, quantity },
      {
        onSuccess: () =>
          toast.success('añadido al carrito', {
            description: selected.name ?? view.title,
            duration: 4000,
            action: { label: 'Ver carrito', onClick: () => navigate('/carrito') },
          }),
      },
    )
  }

  const digital = product.productType === 'DIGITAL'
  const currency = selected?.currency ?? view.currency

  return (
    <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6 md:py-12">
      <Breadcrumbs brand={view.brand} title={view.title} />
      <div className="mt-6 grid items-start gap-10 md:grid-cols-[1.05fr_1fr] md:gap-14">
        <div className="relative md:sticky md:top-24">
          <div className="aspect-square overflow-hidden rounded-[1.6rem] bg-card ring-1 ring-line">
            <ProductCover productId={product.id} title={view.title} image={selected?.image ?? view.image} />
          </div>
          <PriceSticker amount={price} currency={currency} type={product.productType} seed={product.id} size="lg" className="absolute right-5 -bottom-5" />
        </div>

        <div className="flex flex-col pt-3 md:pt-0">
          <div className="flex flex-wrap items-center gap-2">
            <Badge tone={productTone(product.productType)} className={typeChipRing[productTone(product.productType)]}>
              {product.productType === 'DIGITAL' ? 'Digital · entrega inmediata al pagar' : 'Físico · se envía desde bodega'}
            </Badge>
            {view.brand && (
              <Link
                to={storeHref(view.brand)}
                aria-label={`Ver la tienda ${view.brand}`}
                className="rounded-sm text-sm font-semibold text-ink-soft underline decoration-line-strong decoration-2 underline-offset-[3px] hover:text-cobalt hover:decoration-cobalt"
              >
                {view.brand}
              </Link>
            )}
          </div>
          <h1 className="mt-3 text-[clamp(2rem,4.5vw,3.2rem)] font-extrabold">{selected?.name ?? view.title}</h1>
          {(selected?.description ?? view.description) && (
            <p className="mt-4 text-lg text-ink-soft">{selected?.description ?? view.description}</p>
          )}

          {view.variants.length > 1 && (
            <fieldset className="mt-8">
              <legend className="mb-3 font-semibold">Elige una opción</legend>
              <div className="flex flex-wrap gap-2.5">
                {view.variants.map((variant) => {
                  const active = variant.id === selected?.id
                  return (
                    <label
                      key={variant.id}
                      className={cn(
                        'cursor-pointer rounded-2xl px-4 py-2.5 ring-1 ring-inset transition-colors has-[:focus-visible]:outline-3 has-[:focus-visible]:outline-cobalt',
                        active ? 'bg-ink text-white ring-ink' : 'bg-card ring-line-strong hover:ring-ink',
                      )}
                    >
                      <input
                        type="radio"
                        name="variante"
                        value={variant.id}
                        checked={active}
                        onChange={() => setSelectedId(variant.id)}
                        className="sr-only"
                      />
                      <span className="block font-semibold">{variantLabel(variant)}</span>
                      {view.hasPriceRange && variant.price !== null && (
                        <span className={cn('block text-sm', active ? 'text-white/75' : 'text-ink-soft')}>
                          {formatMoney(variant.price, variant.currency)}
                        </span>
                      )}
                    </label>
                  )
                })}
              </div>
            </fieldset>
          )}

          {selected && selected.options.length > 0 && (
            <dl className="mt-6 grid grid-cols-[auto_1fr] gap-x-6 gap-y-1 text-sm">
              {selected.options.map(([key, value]) => (
                <div key={key} className="contents">
                  <dt className="text-ink-faint">{key}</dt>
                  <dd className="font-semibold">{value}</dd>
                </div>
              ))}
            </dl>
          )}

          <div className="mt-8 grid gap-4 rounded-[var(--radius-card)] bg-card p-5 ring-1 ring-line">
            <p className="flex flex-wrap items-baseline justify-between gap-x-3" data-testid="buy-price">
              <span className="text-sm font-semibold text-ink-soft">Precio{view.variants.length > 1 ? ' de esta opción' : ''}</span>
              <span className="font-display text-3xl font-extrabold tabular-nums" aria-live="polite">
                {price === null ? 'por definir' : formatMoney(price, currency)}
              </span>
            </p>
            {sample ? (
              <Notice title="este es un producto de ejemplo">no se puede comprar; cuando el backend esté en línea verás productos reales.</Notice>
            ) : !session ? (
              <>
                <p className="text-ink-soft">ingresa con tu cuenta de comprador para agregar productos al carrito.</p>
                <ButtonLink to="/ingresar" state={{ from: location.pathname }} size="lg">
                  Ingresar para comprar
                </ButtonLink>
              </>
            ) : !isBuyer ? (
              <Notice tone="cobalt" title="solo los compradores pueden comprar">
                tu cuenta tiene otro rol, así que puedes ver el catálogo pero no agregar al carrito.
              </Notice>
            ) : (
              <>
                {price === null && <Notice title="esta opción no tiene precio publicado">elige otra opción o vuelve más tarde.</Notice>}
                <div className="flex flex-wrap items-center gap-3">
                  <QuantityStepper value={quantity} onChange={setQuantity} label="Cantidad" />
                  <Button size="lg" className="flex-1" onClick={add} loading={addToCart.isPending} disabled={!canBuy}>
                    Agregar al carrito
                  </Button>
                </div>
                {price !== null && quantity > 1 && selected && (
                  <p className="text-sm text-ink-soft">
                    {quantity} × {formatMoney(price, selected.currency)} ={' '}
                    <strong className="text-ink">{formatMoney(price * quantity, selected.currency)}</strong>
                  </p>
                )}
              </>
            )}
          </div>
          <div className="mt-4">
            <TrustStrip digital={digital} />
          </div>
        </div>
      </div>
      <MoreFromBrand product={product} brand={view.brand} />
    </div>
  )
}
