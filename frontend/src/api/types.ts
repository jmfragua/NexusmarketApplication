// Mirrors of the backend DTOs in src/main/java/application/adapters/rest/dtos.
// BigDecimal travels as a JSON number; it is kept as `number` and only formatted for display.

export const ROLES = ['BUYER', 'SELLER', 'LOGISTICS_OPERATOR', 'ADMINISTRATOR', 'SUPERVISOR'] as const
export type Role = (typeof ROLES)[number]

export const ORDER_STATUSES = ['CART', 'PENDING_PAYMENT', 'PAID', 'DISPATCHED', 'DELIVERED'] as const
export type OrderStatus = (typeof ORDER_STATUSES)[number]

export type ProductType = 'PHYSICAL' | 'DIGITAL'
export type ProductStatus = 'PUBLISHED' | 'SUSPENDED' | 'DISCONTINUED'
export type StockCondition = 'AVAILABLE' | 'DAMAGED'
export type UserStatus = 'ACTIVE' | 'BLOCKED'
export type MovementType = 'INBOUND' | 'RESERVATION' | 'SALE_OUTBOUND' | 'ADJUSTMENT' | 'RETURN'
export type WarehouseType = 'MARKETPLACE' | 'SELLER'

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface PageParams {
  page?: number
  size?: number
}

export interface ErrorBody {
  status: number
  rule: string
  message: string
}

// Requests

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterBuyerRequest {
  buyerId: string
  fullName: string
  email: string
  identityDocument: string
  password: string
  primaryAddress: string
}

export interface AddressRequest {
  address: string
}

export interface AddCartItemRequest {
  cartItemId: string
  productId: string
  productVariantId: string
  quantity: number
}

export interface UpdateCartItemQuantityRequest {
  quantity: number
}

export interface OrderLineRequest {
  orderLineId: string
  productId: string
  productVariantId: string
  quantity: number
  lineAmount: number
  currency: string
}

export interface ConfirmCartRequest {
  orderId: string
  deliveryAddress: string
  lines: OrderLineRequest[]
}

export interface RequestReturnRequest {
  returnId: string
  orderId: string
  orderLineId: string
  quantity: number
}

export interface RegisterProductRequest {
  productId: string
  productType: ProductType
}

export interface AddProductVariantRequest {
  variantId: string
  attributes: Record<string, string>
}

export interface StockMovementRequest {
  inventoryItemId: string
  movementId: string
  quantity: number
}

export interface StockLineMovementRequest {
  orderId: string
  orderLineId: string
  inventoryItemId: string
  movementId: string
}

export interface ShipmentActionRequest {
  shipmentId: string
  orderId: string
  warehouseId?: string
  deliveryAddress?: string
}

export interface RegisterSellerRequest {
  sellerId: string
  fullName: string
  email: string
  identityDocument: string
  password: string
  firstWarehouseId: string
}

export interface ChangeUserRoleRequest {
  role: Role
}

export interface RegisterWarehouseRequest {
  warehouseId: string
}

export interface IssueInvoiceRequest {
  invoiceId: string
  orderId: string
}

export interface IssueRefundRequest {
  refundId: string
  productReturnId: string
  invoiceId: string
  amount: number
  currency: string
}

// Responses

export interface AuthResponse {
  token: string
  userId: string
  email: string
  role: Role
}

export interface BuyerResponse {
  id: string
  fullName: string
  email: string
  identityDocument: string
  status: UserStatus
  primaryAddress: string
  additionalAddresses: string[]
  commercialStatus: 'ENABLED' | 'DISABLED'
}

export interface CartItemResponse {
  id: string
  productId: string
  productVariantId: string
  quantity: number
}

export interface CartResponse {
  id: string
  buyerId: string
  items: CartItemResponse[]
}

export interface ProductVariantResponse {
  id: string
  attributes: Record<string, string>
}

export interface ProductResponse {
  id: string
  sellerId: string
  productType: ProductType
  status: ProductStatus
  visibleInCatalog: boolean
  variants: ProductVariantResponse[]
}

export interface OrderLineResponse {
  id: string
  productId: string
  productVariantId: string
  quantity: number
  lineAmount: number
  currency: string
}

export interface OrderResponse {
  id: string
  buyerId: string
  status: OrderStatus
  modifiable: boolean
  deliveryAddress: string
  totalAmount: number
  currency: string
  lines: OrderLineResponse[]
}

export interface InvoiceResponse {
  id: string
  orderId: string
  buyerId: string
  totalAmount: number
  currency: string
}

export interface ProductReturnResponse {
  id: string
  orderId: string
  orderLineId: string
  quantity: number
}

export interface RefundResponse {
  id: string
  productReturnId: string
  invoiceId: string
  amount: number
  currency: string
}

export interface InventoryItemResponse {
  id: string
  productId: string
  warehouseId: string
  availableQuantity: number
  reservedQuantity: number
  condition: StockCondition
}

export interface InventoryMovementResponse {
  id: string
  inventoryItemId: string
  movementType: MovementType
  quantity: number
}

export interface WarehouseResponse {
  id: string
  warehouseType: WarehouseType
  sellerId: string | null
}

export interface SellerResponse {
  id: string
  fullName: string
  email: string
  identityDocument: string
  status: UserStatus
}

export interface UserResponse {
  id: string
  fullName: string
  email: string
  identityDocument: string
  role: Role
  status: UserStatus
}

export interface ShipmentResponse {
  id: string
  orderId: string
  warehouseId: string
  deliveryAddress: string
}

/** `GET /supervisor/reports/invoiced-total`: `{ "COP": 120000 }`, or `null` on 204. */
export type InvoicedTotal = Record<string, number> | null
