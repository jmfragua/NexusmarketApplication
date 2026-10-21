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

export function StatusTimeline({ status, audience = 'buyer' }: { status: OrderStatus; audience?: 'buyer' | 'staff' }) {
  const reached = steps.indexOf(status)
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
                <span className="sr-only">{done ? ', completado' : current ? ', estado actual' : ', pendiente'}</span>
              </span>
              {current && <span className="block text-sm text-ink-soft">{hints[audience][step]}</span>}
            </span>
          </li>
        )
      })}
    </ol>
  )
}
