import type { OrderStatus } from '@/api/types'
import { Badge, type Tone } from '@/components/ui/Badge'
import { orderStatusLabel } from '@/lib/labels'

const tone: Record<OrderStatus, Tone> = {
  CART: 'neutral',
  PENDING_PAYMENT: 'amber',
  PAID: 'cobalt',
  DISPATCHED: 'lime',
  DELIVERED: 'mint',
}

export function OrderStatusBadge({ status }: { status: OrderStatus }) {
  return <Badge tone={tone[status]}>{orderStatusLabel[status]}</Badge>
}
