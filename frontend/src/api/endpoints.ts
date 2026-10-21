import { http, pageQuery } from './client'
import type {
  AddCartItemRequest,
  AddProductVariantRequest,
  AuthResponse,
  BuyerResponse,
  CartResponse,
  ChangeUserRoleRequest,
  ConfirmCartRequest,
  InventoryItemResponse,
  InventoryMovementResponse,
  InvoicedTotal,
  InvoiceResponse,
  IssueInvoiceRequest,
  IssueRefundRequest,
  LoginRequest,
  OrderResponse,
  OrderStatus,
  Page,
  PageParams,
  ProductResponse,
  ProductReturnResponse,
  RefundResponse,
  RegisterBuyerRequest,
  RegisterProductRequest,
  RegisterSellerRequest,
  RegisterWarehouseRequest,
  RequestReturnRequest,
  SellerResponse,
  ShipmentActionRequest,
  ShipmentResponse,
  StockLineMovementRequest,
  StockMovementRequest,
  UserResponse,
  WarehouseResponse,
} from './types'

const enc = encodeURIComponent

export const authApi = {
  login: (body: LoginRequest) => http.post<AuthResponse>('/auth/login', body),
  register: (body: RegisterBuyerRequest) => http.post<AuthResponse>('/auth/register', body),
}

export const catalogApi = {
  list: (params?: PageParams, signal?: AbortSignal) =>
    http.get<Page<ProductResponse>>('/catalog/products', pageQuery(params), signal),
  get: (productId: string, signal?: AbortSignal) =>
    http.get<ProductResponse>(`/catalog/products/${enc(productId)}`, undefined, signal),
}

export const buyerApi = {
  addAddress: (address: string) => http.post<BuyerResponse>('/buyer/me/addresses', { address }),
  setPrimaryAddress: (address: string) => http.put<BuyerResponse>('/buyer/me/addresses/primary', { address }),
  cart: () => http.get<CartResponse>('/buyer/me/cart'),
  addCartItem: (body: AddCartItemRequest) => http.post<CartResponse>('/buyer/me/cart/items', body),
  updateCartItem: (cartItemId: string, quantity: number) =>
    http.patch<CartResponse>(`/buyer/me/cart/items/${enc(cartItemId)}`, { quantity }),
  removeCartItem: (cartItemId: string) => http.delete<CartResponse>(`/buyer/me/cart/items/${enc(cartItemId)}`),
  clearCart: () => http.delete<CartResponse>('/buyer/me/cart'),
  confirmCart: (body: ConfirmCartRequest) => http.post<OrderResponse>('/buyer/me/orders', body),
  pay: (orderId: string) => http.post<OrderResponse>(`/buyer/me/orders/${enc(orderId)}/payment`),
  orders: (params?: PageParams) => http.get<Page<OrderResponse>>('/buyer/me/orders', pageQuery(params)),
  order: (orderId: string) => http.get<OrderResponse>(`/buyer/me/orders/${enc(orderId)}`),
  invoices: (params?: PageParams) => http.get<Page<InvoiceResponse>>('/buyer/me/invoices', pageQuery(params)),
  invoiceOf: (orderId: string) => http.get<InvoiceResponse>(`/buyer/me/orders/${enc(orderId)}/invoice`),
  requestReturn: (body: RequestReturnRequest) => http.post<ProductReturnResponse>('/buyer/me/returns', body),
  returnsOf: (orderId: string, params?: PageParams) =>
    http.get<Page<ProductReturnResponse>>(`/buyer/me/orders/${enc(orderId)}/returns`, pageQuery(params)),
}

export const sellerApi = {
  registerProduct: (body: RegisterProductRequest) => http.post<ProductResponse>('/seller/products', body),
  products: (params?: PageParams) => http.get<Page<ProductResponse>>('/seller/products', pageQuery(params)),
  publish: (productId: string) => http.post<ProductResponse>(`/seller/products/${enc(productId)}/publication`),
  suspend: (productId: string) => http.delete<ProductResponse>(`/seller/products/${enc(productId)}/publication`),
  discontinue: (productId: string) => http.delete<ProductResponse>(`/seller/products/${enc(productId)}`),
  addVariant: (productId: string, body: AddProductVariantRequest) =>
    http.post<ProductResponse>(`/seller/products/${enc(productId)}/variants`, body),
  removeVariant: (productId: string, variantId: string) =>
    http.delete<ProductResponse>(`/seller/products/${enc(productId)}/variants/${enc(variantId)}`),
  warehouses: (params?: PageParams) => http.get<Page<WarehouseResponse>>('/seller/warehouses', pageQuery(params)),
  warehouse: (warehouseId: string) => http.get<WarehouseResponse>(`/seller/warehouses/${enc(warehouseId)}`),
  stockOf: (productId: string, params?: PageParams) =>
    http.get<Page<InventoryItemResponse>>(`/seller/products/${enc(productId)}/stock`, pageQuery(params)),
  receive: (body: StockMovementRequest) => http.post<InventoryMovementResponse>('/seller/stock/receipts', body),
  adjust: (body: StockMovementRequest) => http.post<InventoryMovementResponse>('/seller/stock/adjustments', body),
  markDamaged: (inventoryItemId: string) =>
    http.post<InventoryItemResponse>(`/seller/stock/${enc(inventoryItemId)}/damage`),
}

export const logisticsApi = {
  warehouses: (params?: PageParams) => http.get<Page<WarehouseResponse>>('/logistics/warehouses', pageQuery(params)),
  stockOf: (warehouseId: string, params?: PageParams) =>
    http.get<Page<InventoryItemResponse>>(`/logistics/warehouses/${enc(warehouseId)}/stock`, pageQuery(params)),
  receive: (body: StockMovementRequest) => http.post<InventoryMovementResponse>('/logistics/stock/receipts', body),
  reserve: (body: StockLineMovementRequest) =>
    http.post<InventoryMovementResponse>('/logistics/stock/reservations', body),
  release: (body: StockLineMovementRequest) => http.post<InventoryMovementResponse>('/logistics/stock/releases', body),
  issueForSale: (body: StockLineMovementRequest) =>
    http.post<InventoryMovementResponse>('/logistics/stock/sale-outbounds', body),
  restockReturn: (returnId: string, inventoryItemId: string, movementId: string) =>
    http.post<InventoryMovementResponse>(`/logistics/stock/returns/${enc(returnId)}`, undefined, {
      inventoryItemId,
      movementId,
    }),
  pack: (body: ShipmentActionRequest) => http.post<ShipmentResponse>('/logistics/shipments/packaging', body),
  dispatch: (body: ShipmentActionRequest) => http.post<ShipmentResponse>('/logistics/shipments/dispatches', body),
  deliver: (body: ShipmentActionRequest) => http.post<ShipmentResponse>('/logistics/shipments/deliveries', body),
  ordersByStatus: (status: OrderStatus, params?: PageParams) =>
    http.get<Page<OrderResponse>>('/logistics/orders', { status, ...pageQuery(params) }),
}

export const adminApi = {
  registerSeller: (body: RegisterSellerRequest) => http.post<SellerResponse>('/admin/sellers', body),
  sellers: (params?: PageParams) => http.get<Page<SellerResponse>>('/admin/sellers', pageQuery(params)),
  seller: (sellerId: string) => http.get<SellerResponse>(`/admin/sellers/${enc(sellerId)}`),
  block: (userId: string) => http.post<UserResponse>(`/admin/users/${enc(userId)}/block`),
  activate: (userId: string) => http.post<UserResponse>(`/admin/users/${enc(userId)}/activation`),
  changeRole: (userId: string, body: ChangeUserRoleRequest) =>
    http.put<UserResponse>(`/admin/users/${enc(userId)}/role`, body),
  registerWarehouse: (body: RegisterWarehouseRequest) => http.post<WarehouseResponse>('/admin/warehouses', body),
  issueInvoice: (body: IssueInvoiceRequest) => http.post<InvoiceResponse>('/admin/invoices', body),
  productReturn: (returnId: string) => http.get<ProductReturnResponse>(`/admin/returns/${enc(returnId)}`),
  issueRefund: (body: IssueRefundRequest) => http.post<RefundResponse>('/admin/refunds', body),
  refunds: (params?: PageParams) => http.get<Page<RefundResponse>>('/admin/refunds', pageQuery(params)),
}

export const supervisorApi = {
  ordersByStatus: (status: OrderStatus, params?: PageParams) =>
    http.get<Page<OrderResponse>>('/supervisor/orders', { status, ...pageQuery(params) }),
  completedOrders: (params?: PageParams) =>
    http.get<Page<OrderResponse>>('/supervisor/reports/completed-orders', pageQuery(params)),
  invoices: (params?: PageParams) => http.get<Page<InvoiceResponse>>('/supervisor/reports/invoices', pageQuery(params)),
  invoicedTotal: () => http.get<InvoicedTotal>('/supervisor/reports/invoiced-total'),
  stockOfWarehouse: (warehouseId: string, params?: PageParams) =>
    http.get<Page<InventoryItemResponse>>(
      `/supervisor/reports/warehouses/${enc(warehouseId)}/stock`,
      pageQuery(params),
    ),
  damagedStock: (params?: PageParams) =>
    http.get<Page<InventoryItemResponse>>('/supervisor/reports/damaged-stock', pageQuery(params)),
  movements: (params?: PageParams) =>
    http.get<Page<InventoryMovementResponse>>('/supervisor/reports/inventory-movements', pageQuery(params)),
  refunds: (params?: PageParams) => http.get<Page<RefundResponse>>('/supervisor/reports/refunds', pageQuery(params)),
}
