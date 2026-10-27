import type { OrderStatus } from '@/api/types'
import { cn } from '@/lib/cn'
import { orderStatusLabel } from '@/lib/labels'

const steps: OrderStatus[] = ['CART', 'PENDING_PAYMENT', 'PAID', 'DISPATCHED', 'DELIVERED']

const hints: Record<'buyer' | 'staff', Record<OrderStatus, string>> = {
  buyer: {
    CART: 'seleccionaste los productos',
    PENDING_PAYMENT: 'esperando la confirmación del pago',
    PAID: 'la bodega está alistando tu pedido',
    DISPATCHED: 'tu pedido salió de la bodega',
    DELIVERED: 'entregado, ¡que lo disfrutes!',
  },
  staff: {
    CART: 'el comprador aún elige productos',
    PENDING_PAYMENT: 'el comprador no ha pagado',
    PAID: 'listo para alistar y empacar',
    DISPATCHED: 'en camino al comprador',
    DELIVERED: 'entrega confirmada',
  },
}

// For a buyer, a paid order is not waiting on the payment any more: "Pagado" is done and what is
// under way is the dispatch, so the highlight moves there with a hint of its own.
const underWay: Partial<Record<OrderStatus, { step: OrderStatus; hint: string }>> = {
  PAID: { step: 'DISPATCHED', hint: 'la bodega está alistando tu pedido para despacharlo' },
}

export function StatusTimeline({ status, audience = 'buyer' }: { status: OrderStatus; audience?: 'buyer' | 'staff' }) {
  const next = audience === 'buyer' ? underWay[status] : undefined
  // The step drawn as current: the one under way, or else the status itself.
  const reached = next ? steps.indexOf(next.step) : steps.indexOf(status)
  return (
    <ol className="grid gap-0 sm:grid-cols-5" aria-label="Estado del pedido">
      {steps.map((step, i) => {
        const done = i < reached
        const current = i === reached
        return (
          <li key={step} className="relative flex gap-3 pb-5 sm:block sm:pb-0" aria-current={current ? 'step' : undefined}>
            {i < steps.length - 1 && (
              <span
                className={cn(
                  'absolute top-7 left-[0.9rem] h-[calc(100%-1.75rem)] w-0.5 sm:top-[0.9rem] sm:left-9 sm:h-0.5 sm:w-[calc(100%-2.25rem)]',
                  i < reached ? 'bg-cobalt' : 'bg-line-strong',
                )}
                aria-hidden="true"
              />
            )}
            <span
              className={cn(
                'relative z-10 grid size-7 shrink-0 place-items-center rounded-full text-xs font-bold ring-4 ring-card',
                done && 'bg-cobalt text-white',
                current && 'bg-lime text-ink ring-lime/40 motion-safe:animate-pop',
                !done && !current && 'bg-paper text-ink-faint ring-line',
              )}
              aria-hidden="true"
            >
              {done ? '✓' : i + 1}
            </span>
            <span className="sm:mt-3 sm:block sm:pr-3">
              <span className={cn('block font-semibold', !done && !current && 'text-ink-faint')}>
                {orderStatusLabel[step]}
                <span className="sr-only">{done ? ', completado' : current ? (next ? ', en curso' : ', estado actual') : ', pendiente'}</span>
              </span>
              {current && <span className="block text-sm text-ink-soft">{next ? next.hint : hints[audience][step]}</span>}
            </span>
          </li>
        )
      })}
    </ol>
  )
}
