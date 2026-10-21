import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import type { InventoryItemResponse } from '@/api/types'
import { CopyId } from '@/components/CopyId'
import { Button, ButtonLink } from '@/components/ui/Button'
import { Dialog } from '@/components/ui/Dialog'
import { EmptyState, ErrorState, LoadingRows, Notice } from '@/components/ui/States'
import { ProductCover } from '@/features/catalog/ProductCover'
import { readProduct, variantLabel } from '@/features/catalog/variant'
import { QuantityDialog } from '@/features/inventory/QuantityDialog'
import { StockTable } from '@/features/inventory/StockTable'
import { formatMoney } from '@/lib/money'
import { productTypeLabel } from '@/lib/labels'
import { ProductLifecycle, ProductStatusBadge } from './ProductLifecycle'
import { useAddVariant, useAdjustStock, useMarkDamaged, useMyProduct, useReceiveStock, useRemoveVariant, useStockOf } from './queries'
import { VariantForm } from './VariantForm'

type StockAction = { kind: 'receive' | 'adjust'; item: InventoryItemResponse }

function StockSection({ productId, editable }: { productId: string; editable: boolean }) {
  const stock = useStockOf(productId)
  const receive = useReceiveStock(productId)
  const adjust = useAdjustStock(productId)
  const damage = useMarkDamaged(productId)
  const [action, setAction] = useState<StockAction | null>(null)

  return (
    <section aria-labelledby="existencias" className="mt-10">
      <h2 id="existencias" className="mb-1 text-2xl font-bold">
        existencias
      </h2>
      <p className="mb-4 text-ink-soft">el inventario es distribuido: cada registro une este producto con una bodega.</p>
      {stock.isPending ? (
        <LoadingRows rows={2} label="cargando existencias" />
      ) : stock.error ? (
        <ErrorState error={stock.error} onRetry={() => void stock.refetch()} />
      ) : stock.data.content.length === 0 ? (
        <EmptyState
          title="este producto no tiene registros de existencias"
          description={
            <>
              el backend no ofrece una operación para crear el registro de un producto en una bodega, así que todavía no se pueden
              registrar ingresos. Los productos digitales no lo necesitan.
            </>
          }
        />
      ) : (
        <StockTable
          caption="Existencias del producto por bodega"
          reference="warehouse"
          items={stock.data.content}
          renderActions={
            editable
              ? (item) => (
                  <div className="flex justify-end gap-1.5">
                    <Button variant="outline" size="sm" onClick={() => setAction({ kind: 'receive', item })}>
                      Ingreso
                    </Button>
                    <Button variant="outline" size="sm" onClick={() => setAction({ kind: 'adjust', item })}>
                      Ajustar
                    </Button>
                    {item.condition !== 'DAMAGED' && (
                      <Button variant="danger" size="sm" loading={damage.isPending && damage.variables === item.id} onClick={() => damage.mutate(item.id)}>
                        Dañado
                      </Button>
                    )}
                  </div>
                )
              : undefined
          }
        />
      )}

      <QuantityDialog
        key={action ? `${action.kind}-${action.item.id}` : 'none'}
        open={action !== null}
        onOpenChange={(open) => !open && setAction(null)}
        title={action?.kind === 'adjust' ? 'ajustar existencias' : 'registrar ingreso'}
        description={
          action?.kind === 'adjust'
            ? `corrige la cantidad disponible; hoy hay ${action.item.availableQuantity}. El ajuste queda auditado.`
            : 'suma unidades que llegaron a la bodega.'
        }
        label={action?.kind === 'adjust' ? 'Nueva cantidad disponible' : 'Unidades que ingresan'}
        submitLabel={action?.kind === 'adjust' ? 'Guardar ajuste' : 'Registrar ingreso'}
        initial={action?.kind === 'adjust' ? Math.max(1, action.item.availableQuantity) : 1}
        pending={receive.isPending || adjust.isPending}
        onSubmit={(quantity) => {
          if (!action) return
          const run = action.kind === 'adjust' ? adjust : receive
          run.mutate({ inventoryItemId: action.item.id, quantity }, { onSuccess: () => setAction(null) })
        }}
      />
    </section>
  )
}

export function SellerProductPage() {
  const { productId = '' } = useParams()
  const { product, isPending, error, refetch } = useMyProduct(productId)
  const addVariant = useAddVariant(productId)
  const removeVariant = useRemoveVariant(productId)
  const [adding, setAdding] = useState(false)

  if (isPending) return <LoadingRows rows={3} label="cargando el producto" />
  if (error) return <ErrorState error={error} onRetry={() => void refetch()} />
  if (!product) {
    return (
      <EmptyState
        title="no encontramos este producto en tu catálogo"
        description="puede que pertenezca a otro vendedor o que el enlace esté mal escrito."
        action={<ButtonLink to="/vendedor">Ver mis productos</ButtonLink>}
      />
    )
  }

  const view = readProduct(product)
  const editable = product.status !== 'DISCONTINUED'
  const first = view.variants[0]

  return (
    <>
      <Link to="/vendedor" className="text-sm font-semibold text-ink-soft hover:text-ink">
        <span aria-hidden="true">←</span> Mis productos
      </Link>
      <header className="mt-4 mb-8 grid gap-5 sm:grid-cols-[7rem_1fr] sm:items-center">
        <div className="aspect-square w-28 overflow-hidden rounded-2xl bg-card ring-1 ring-line" aria-hidden="true">
          <ProductCover productId={product.id} title={view.title} image={view.image} />
        </div>
        <div className="min-w-0">
          <h1 className="text-[clamp(1.9rem,4.5vw,2.8rem)] font-extrabold">{view.title}</h1>
          <div className="mt-2 flex flex-wrap items-center gap-2 text-sm text-ink-soft">
            <ProductStatusBadge product={product} />
            <span>{productTypeLabel[product.productType]}</span>
            <CopyId id={product.id} label="identificador del producto" />
          </div>
          <div className="mt-4">
            <ProductLifecycle product={product} size="md" />
          </div>
        </div>
      </header>

      {product.status === 'PUBLISHED' && !product.visibleInCatalog && (
        <div className="mb-6">
          <Notice tone="coral" title="no aparece en la tienda">un producto publicado necesita al menos una variante para ser visible.</Notice>
        </div>
      )}

      <section aria-labelledby="variantes">
        <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
          <h2 id="variantes" className="text-2xl font-bold">
            variantes
          </h2>
          {editable && <Button onClick={() => setAdding(true)}>Agregar variante</Button>}
        </div>
        {view.variants.length === 0 ? (
          <EmptyState title="sin variantes" description="agrega una variante con nombre y precio para que el producto aparezca en el catálogo." />
        ) : (
          <ul className="grid gap-3 sm:grid-cols-2">
            {view.variants.map((variant) => (
              <li key={variant.id} className="flex items-start justify-between gap-3 rounded-2xl bg-card p-4 ring-1 ring-line">
                <div className="min-w-0">
                  <p className="font-semibold">{variant.name ?? 'sin nombre'}</p>
                  <p className="text-sm text-ink-soft">{variant.options.length ? variantLabel(variant) : 'sin opciones'}</p>
                  <p className="mt-1 font-display text-lg font-bold">
                    {variant.price !== null ? formatMoney(variant.price, variant.currency) : <span className="text-coral-deep">sin precio</span>}
                  </p>
                </div>
                {editable && (
                  <Button
                    variant="ghost"
                    size="sm"
                    loading={removeVariant.isPending && removeVariant.variables === variant.id}
                    onClick={() => removeVariant.mutate(variant.id)}
                    aria-label={`Retirar la variante ${variantLabel(variant)}`}
                  >
                    Retirar
                  </Button>
                )}
              </li>
            ))}
          </ul>
        )}
      </section>

      <StockSection productId={product.id} editable={editable} />

      <Dialog open={adding} onOpenChange={setAdding} title="agregar variante" description="reutilizamos el nombre y la marca; cambia lo que distinga a esta opción.">
        <VariantForm
          submitLabel="Agregar variante"
          pending={addVariant.isPending}
          defaults={{
            name: first?.name ?? '',
            brand: first?.brand ?? '',
            description: first?.description ?? '',
            currency: first?.currency ?? 'COP',
            price: first?.price !== null && first?.price !== undefined ? String(first.price) : '',
            options: first?.options.length ? first.options.map(([key]) => ({ key, value: '' })) : [{ key: '', value: '' }],
          }}
          onSubmit={(attributes) => addVariant.mutate(attributes, { onSuccess: () => setAdding(false) })}
        />
      </Dialog>
    </>
  )
}
