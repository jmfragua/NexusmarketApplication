import { ApiError } from './client'

// The backend names the domain rule that rejected a request (`rule`) and explains it in English
// (`message`). Known rules get a Spanish sentence that says what happened and what to do; any other
// rule falls back to the backend message so nothing is ever hidden.
const byRule: Record<string, string> = {
  NetworkError: 'no pudimos conectar con el servidor; revisa tu conexión y vuelve a intentarlo',
  UnauthorizedOperationException: 'tu cuenta no tiene permiso para hacer esto',
  UserNotActiveException: 'tu usuario está bloqueado; pide a un administrador que lo reactive',
  ProductNotOwnedException: 'ese producto pertenece a otro vendedor',
  WarehouseNotOwnedException: 'esa bodega pertenece a otro vendedor o al marketplace',
  BuyerNotAllowedToPurchaseException: 'tu cuenta no está habilitada para comprar en este momento',
  EntityNotFoundException: 'no encontramos lo que buscas; puede que el identificador esté mal escrito',
  InvoiceNotIssuedException: 'todavía no se ha emitido la factura de este pedido',
  OrderNotModifiableException: 'este pedido ya terminó y no se puede modificar',
  InvalidOrderTransitionException: 'el pedido no está en el estado que permite esta acción',
  InsufficientStockException: 'no hay existencias suficientes para esta operación',
  DamagedStockException: 'esas existencias están marcadas como dañadas y no se pueden usar',
  ProductNotAvailableException: 'este producto no está disponible para la venta en este momento',
  ReturnNotAllowedException: 'solo se pueden devolver productos de pedidos entregados',
  EmptyCartException: 'tu carrito está vacío; agrega algo antes de confirmar',
  DuplicateEmailException: 'ya existe una cuenta con ese correo',
  DuplicateIdentityDocumentException: 'ya existe una cuenta con ese documento de identidad',
  DuplicateInventoryItemException: 'ese producto ya tiene un registro de existencias en esa bodega',
  InvalidRoleChangeException: 'ese cambio de rol no está permitido: compradores y vendedores conservan su rol',
  RefundExceedsInvoiceException: 'el reembolso no puede superar el total facturado',
  ShipmentNotAllowedException: 'este pedido no tiene productos físicos que enviar',
}

const detailRules = new Set(['InvalidInput', 'RequestValidation', 'DomainInvariant'])

export function describeError(error: unknown, context?: 'login'): string {
  if (!(error instanceof ApiError)) {
    return error instanceof Error && error.message ? error.message : 'algo salió mal y no pudimos completar la acción'
  }
  if (context === 'login' && error.rule === 'UnauthorizedOperationException') {
    return 'el correo o la contraseña no coinciden'
  }
  const known = byRule[error.rule]
  if (known) return known
  if (detailRules.has(error.rule)) return `revisa los datos enviados (${error.message})`
  return error.message
}
