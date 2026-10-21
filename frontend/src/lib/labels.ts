import type {
  MovementType,
  OrderStatus,
  ProductStatus,
  ProductType,
  Role,
  StockCondition,
  UserStatus,
  WarehouseType,
} from '@/api/types'

export const roleLabel: Record<Role, string> = {
  BUYER: 'Comprador',
  SELLER: 'Vendedor',
  LOGISTICS_OPERATOR: 'Operador logístico',
  ADMINISTRATOR: 'Administrador',
  SUPERVISOR: 'Supervisor',
}

export const orderStatusLabel: Record<OrderStatus, string> = {
  CART: 'Carrito',
  PENDING_PAYMENT: 'Pendiente de pago',
  PAID: 'Pagado',
  DISPATCHED: 'Despachado',
  DELIVERED: 'Entregado',
}

export const productStatusLabel: Record<ProductStatus, string> = {
  PUBLISHED: 'Publicado',
  SUSPENDED: 'Suspendido',
  DISCONTINUED: 'Descontinuado',
}

export const productTypeLabel: Record<ProductType, string> = {
  PHYSICAL: 'Físico',
  DIGITAL: 'Digital',
}

export const conditionLabel: Record<StockCondition, string> = {
  AVAILABLE: 'Disponible',
  DAMAGED: 'Dañado',
}

export const userStatusLabel: Record<UserStatus, string> = {
  ACTIVE: 'Activo',
  BLOCKED: 'Bloqueado',
}

export const movementLabel: Record<MovementType, string> = {
  INBOUND: 'Ingreso',
  RESERVATION: 'Reserva',
  SALE_OUTBOUND: 'Salida por venta',
  ADJUSTMENT: 'Ajuste',
  RETURN: 'Devolución',
}

export const warehouseTypeLabel: Record<WarehouseType, string> = {
  MARKETPLACE: 'Del marketplace',
  SELLER: 'Del vendedor',
}
