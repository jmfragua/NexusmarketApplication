import { useState } from 'react'
import type { ProductResponse, ProductStatus } from '@/api/types'
import { Badge, type Tone } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Dialog } from '@/components/ui/Dialog'
import { productStatusLabel } from '@/lib/labels'
import { useDiscontinue, usePublish, useSuspend } from './queries'

const tone: Record<ProductStatus, Tone> = { PUBLISHED: 'mint', SUSPENDED: 'amber', DISCONTINUED: 'neutral' }

export function ProductStatusBadge({ product }: { product: ProductResponse }) {
  return (
    <span className="inline-flex flex-wrap gap-1.5">
      <Badge tone={tone[product.status]}>{productStatusLabel[product.status]}</Badge>
      {product.status === 'PUBLISHED' && !product.visibleInCatalog && <Badge tone="coral">Oculto: sin variantes</Badge>}
    </span>
  )
}

/**
 * Publicado ⇄ Suspendido → Descontinuado. Discontinuing is terminal and the backend only allows it
 * from a suspended product, so the UI never offers a transition the model would reject.
 */
export function ProductLifecycle({ product, size = 'sm' }: { product: ProductResponse; size?: 'sm' | 'md' }) {
  const publish = usePublish()
  const suspend = useSuspend()
  const discontinue = useDiscontinue()
  const [confirm, setConfirm] = useState(false)

  if (product.status === 'DISCONTINUED') {
    return <p className="text-sm text-ink-soft">descontinuado de forma definitiva; ya no admite cambios.</p>
  }

  return (
    <div className="flex flex-wrap gap-2">
      {product.status === 'PUBLISHED' ? (
        <Button variant="outline" size={size} loading={suspend.isPending} onClick={() => suspend.mutate(product.id)}>
          Suspender
        </Button>
      ) : (
        <>
          <Button size={size} loading={publish.isPending} onClick={() => publish.mutate(product.id)}>
            Publicar
          </Button>
          <Button variant="danger" size={size} onClick={() => setConfirm(true)}>
            Descontinuar
          </Button>
        </>
      )}
      <Dialog
        open={confirm}
        onOpenChange={setConfirm}
        title="¿descontinuar este producto?"
        description="es definitivo: no podrás volver a publicarlo ni cambiar sus variantes."
      >
        <div className="flex justify-end gap-2">
          <Button variant="outline" onClick={() => setConfirm(false)}>
            Cancelar
          </Button>
          <Button
            variant="danger"
            loading={discontinue.isPending}
            onClick={() => discontinue.mutate(product.id, { onSuccess: () => setConfirm(false) })}
          >
            Descontinuar
          </Button>
        </div>
      </Dialog>
    </div>
  )
}
