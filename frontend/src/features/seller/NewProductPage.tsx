import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { ProductType } from '@/api/types'
import { PageHeader } from '@/components/ui/PageHeader'
import { Notice } from '@/components/ui/States'
import { cn } from '@/lib/cn'
import { useRegisterProduct } from './queries'
import { VariantForm } from './VariantForm'

const types: Array<{ value: ProductType; title: string; text: string }> = [
  { value: 'PHYSICAL', title: 'Físico', text: 'se guarda en bodega y se envía al comprador.' },
  { value: 'DIGITAL', title: 'Digital', text: 'se entrega de inmediato al confirmar el pago.' },
]

export function NewProductPage() {
  const navigate = useNavigate()
  const register = useRegisterProduct()
  const [productType, setProductType] = useState<ProductType>('PHYSICAL')

  return (
    <>
      <PageHeader eyebrow="Vendedor" title="registrar producto" description="el tipo no se puede cambiar después. La primera variante define nombre y precio." />
      <div className="grid gap-8 lg:grid-cols-[1fr_20rem]">
        <section className="rounded-[var(--radius-card)] bg-card p-5 ring-1 ring-line sm:p-6">
          <VariantForm
            submitLabel="Registrar y publicar"
            pending={register.isPending}
            onSubmit={(attributes) =>
              register.mutate(
                { productType, attributes },
                { onSuccess: (product) => navigate(`/vendedor/productos/${encodeURIComponent(product.id)}`) },
              )
            }
          >
            <fieldset className="grid gap-2.5 sm:grid-cols-2">
              <legend className="mb-2 text-sm font-semibold">Tipo de producto</legend>
              {types.map((t) => (
                <label
                  key={t.value}
                  className={cn('cursor-pointer rounded-2xl p-4 ring-1 ring-inset transition-colors has-[:focus-visible]:outline-3 has-[:focus-visible]:outline-cobalt', productType === t.value ? 'bg-cobalt-wash ring-cobalt' : 'ring-line hover:ring-ink/40')}
                >
                  <input type="radio" name="tipo" className="sr-only" checked={productType === t.value} onChange={() => setProductType(t.value)} />
                  <span className="block font-semibold">{t.title}</span>
                  <span className="text-sm text-ink-soft">{t.text}</span>
                </label>
              ))}
            </fieldset>
          </VariantForm>
        </section>
        <aside className="grid content-start gap-4">
          <Notice tone="cobalt" title="cómo se guarda tu producto">
            el sistema solo conoce variantes con atributos. Nombre, precio, moneda, marca, descripción e imagen se guardan como atributos
            reservados; las demás opciones (color, talla…) las elige el comprador.
          </Notice>
          <Notice title="existencias">
            registrar el producto no crea existencias en tus bodegas. Consulta el detalle del producto para ver cómo quedan.
          </Notice>
        </aside>
      </div>
    </>
  )
}
