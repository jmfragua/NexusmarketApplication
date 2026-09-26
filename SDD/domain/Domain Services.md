# Servicios de Dominio — NexusMarket

## 1. Introducción

Un **servicio de dominio** contiene lógica de negocio que no pertenece naturalmente a una sola
entidad: coordina varias entidades, verifica reglas que dependen del conjunto de datos, o aplica
restricciones de rol antes de invocar el comportamiento del modelo.

En NexusMarket los servicios de dominio cumplen tres propósitos:

- **Orquestar el comportamiento ya encapsulado en las entidades.** El servicio no reimplementa las
  reglas: `Order` sigue siendo quien decide si una transición es válida, `InventoryItem` sigue siendo
  quien impide un saldo negativo. El servicio decide *cuándo* se invoca y *quién* puede hacerlo.
- **Verificar lo que el modelo no puede verificar por sí solo.** La unicidad del correo y del
  documento depende del conjunto completo de datos y no cabe dentro del objeto de valor (RD-VO-09):
  vive en `RegisterBuyerService` y `RegisterSellerService`.
- **Hacer cumplir la matriz de responsabilidades.** Ningún participante administra información fuera
  de su rol (RG-03, RD-ROL-06); los servicios de `authorization/` centralizan esa verificación.

Ubicación en el código: `src/main/java/application/domain/services/`.

---

## 2. Principios de diseño

- **RD-SRV-01.** Una clase por caso de uso de negocio. Un servicio no agrupa operaciones no
  relacionadas: `PublishProductService` publica, `SuspendProductService` suspende.
- **RD-SRV-02.** Los servicios dependen únicamente de modelos de dominio, objetos de valor y
  **interfaces** de puertos de salida. Cero importaciones de Spring, JPA, MongoDB o HTTP.
- **RD-SRV-03.** Las dependencias se inyectan por constructor y se validan con
  `Objects.requireNonNull`: un servicio a medio construir no puede existir.
- **RD-SRV-04.** Las violaciones de regla se señalan con excepciones específicas de
  `domain/exceptions/`, nunca con excepciones genéricas.
- **RD-SRV-05.** Todo servicio que modifica estado valida primero el rol del actor
  (`ValidateRoleAuthorizationService`) y, cuando corresponde, la pertenencia del recurso
  (`ValidateBuyerOwnershipService`, `ValidateSellerOwnershipService`).
- **RD-SRV-06.** Cuando una transición del modelo puede fallar, el servicio la comprueba **antes**
  de invocarla (`isModifiable()`, `canTransitionTo()`, `canReserve()`) para lanzar la excepción de
  dominio correspondiente en lugar de dejar escapar una excepción genérica del modelo.

---

## 3. Áreas de servicio

Los 48 servicios se agrupan en 13 áreas, una subcarpeta por área:

| Área | Servicios | Documento | Responsabilidad |
|---|:---:|---|---|
| `authorization/` | 4 | [authorization-services.md](services/authorization-services.md) | Rol, estado activo y pertenencia del recurso |
| `user/` | 5 | [user-services.md](services/user-services.md) | Alta de comprador, estado y rol de usuarios, identificación |
| `seller/` | 2 | [seller-services.md](services/seller-services.md) | Incorporación y consulta de vendedores |
| `warehouse/` | 2 | [warehouse-services.md](services/warehouse-services.md) | Bodegas del marketplace y consulta de bodegas |
| `catalog/` | 7 | [catalog-services.md](services/catalog-services.md) | Productos, variantes y ciclo del catálogo |
| `inventory/` | 8 | [inventory-services.md](services/inventory-services.md) | Existencias distribuidas y sus movimientos |
| `cart/` | 5 | [cart-services.md](services/cart-services.md) | Selección provisional del comprador |
| `order/` | 4 | [order-services.md](services/order-services.md) | Ciclo de vida del pedido |
| `invoice/` | 2 | [invoice-services.md](services/invoice-services.md) | Emisión y consulta de facturas |
| `shipment/` | 3 | [shipment-services.md](services/shipment-services.md) | Empaque, despacho y entrega |
| `returns/` | 2 | [return-services.md](services/return-services.md) | Devoluciones de posventa |
| `refund/` | 2 | [refund-services.md](services/refund-services.md) | Reembolsos derivados de devoluciones |
| `reporting/` | 2 | [reporting-services.md](services/reporting-services.md) | Consultas consolidadas de solo lectura (OBJ-12) |

> El paquete Java del área de devoluciones es `returns` porque `return` es una palabra reservada
> del lenguaje.

---

## 4. Inventario completo de servicios

| # | Servicio | Área | Operación principal |
|---:|---|---|---|
| 1 | `ValidateUserActiveStatusService` | authorization | `validate(actor)` |
| 2 | `ValidateRoleAuthorizationService` | authorization | `validate(actor, operation, roles...)` |
| 3 | `ValidateBuyerOwnershipService` | authorization | `validateCart` / `validateOrder` / `validateInvoice` |
| 4 | `ValidateSellerOwnershipService` | authorization | `validateProduct` / `validateWarehouse` |
| 5 | `RegisterBuyerService` | user | `register(buyer)` |
| 6 | `BlockUserService` | user | `block(actor, target)` |
| 7 | `ActivateUserService` | user | `activate(actor, target)` |
| 8 | `ChangeUserRoleService` | user | `changeRole(actor, target, newRole)` |
| 9 | `LoginService` | user | `login(email)` |
| 10 | `RegisterSellerService` | seller | `register(actor, …, firstWarehouse)` |
| 11 | `ConsultSellerService` | seller | `consult` / `consultAll` |
| 12 | `RegisterMarketplaceWarehouseService` | warehouse | `register(actor, warehouseId)` |
| 13 | `ConsultWarehouseService` | warehouse | `consult` / `consultOwn` / `consultAll` |
| 14 | `RegisterProductService` | catalog | `register(seller, product)` |
| 15 | `PublishProductService` | catalog | `publish(seller, product)` |
| 16 | `SuspendProductService` | catalog | `suspend(seller, product)` |
| 17 | `DiscontinueProductService` | catalog | `discontinue(seller, product)` |
| 18 | `AddProductVariantService` | catalog | `addVariant(seller, product, variant)` |
| 19 | `RemoveProductVariantService` | catalog | `removeVariant(seller, product, variant)` |
| 20 | `ConsultCatalogService` | catalog | `consultPublicCatalog` / `consultOwnCatalog` |
| 21 | `ReceiveStockService` | inventory | `receive(actor, item, quantity, movementId)` |
| 22 | `ReserveStockService` | inventory | `reserve(actor, line, item, movementId)` |
| 23 | `ReleaseReservationService` | inventory | `release(actor, line, item, movementId)` |
| 24 | `IssueForSaleService` | inventory | `issue(actor, line, item, movementId)` |
| 25 | `AdjustStockService` | inventory | `adjust(actor, item, newQuantity, movementId)` |
| 26 | `MarkDamagedService` | inventory | `markAsDamaged(actor, item)` |
| 27 | `RestockReturnService` | inventory | `restock(actor, return, item, movementId)` |
| 28 | `ConsultInventoryService` | inventory | `consultStock` / `consultMovements` |
| 29 | `AddCartItemService` | cart | `addItem(buyer, cart, product, variantId, quantity, itemId)` |
| 30 | `RemoveCartItemService` | cart | `removeItem(buyer, cart, item)` |
| 31 | `UpdateCartItemQuantityService` | cart | `updateQuantity(buyer, cart, item, quantity)` |
| 32 | `ClearCartService` | cart | `clear(buyer, cart)` |
| 33 | `ConfirmCartService` | cart | `confirm(buyer, cart, orderId, address, lines)` |
| 34 | `ConfirmPaymentService` | order | `confirmPayment(buyer, order)` |
| 35 | `DispatchOrderService` | order | `dispatch(actor, order)` |
| 36 | `CompleteDeliveryService` | order | `completeDelivery(actor, order)` |
| 37 | `ConsultOrderService` | order | `consultOwn` / `consultByStatus` |
| 38 | `IssueInvoiceService` | invoice | `issue(actor, order, invoiceId)` |
| 39 | `ConsultInvoiceService` | invoice | `consultByOrder` / `consultOwn` |
| 40 | `PackShipmentService` | shipment | `pack(actor, shipment, order, products)` |
| 41 | `DispatchShipmentService` | shipment | `dispatch(actor, shipment, order)` |
| 42 | `ConfirmShipmentDeliveryService` | shipment | `confirmDelivery(actor, shipment, order)` |
| 43 | `RequestReturnService` | returns | `requestReturn(buyer, order, line, quantity, returnId)` |
| 44 | `ConsultReturnService` | returns | `consult` / `consultOwn` |
| 45 | `IssueRefundService` | refund | `issue(actor, return, invoice, amount, refundId)` |
| 46 | `ConsultRefundService` | refund | `consult` / `consultByReturn` / `consultAll` |
| 47 | `ConsultSalesReportService` | reporting | `consultCompletedOrders` / `consultInvoicedTotal` |
| 48 | `ConsultInventoryReportService` | reporting | `consultStockByWarehouse` / `consultDamagedStock` |

---

## 5. Excepciones de dominio

Los servicios señalan las violaciones de regla con las excepciones de
`src/main/java/application/domain/exceptions/`, todas descendientes de `DomainException`:

| Excepción | Regla que protege |
|---|---|
| `EntityNotFoundException` | Referencia a una entidad inexistente |
| `UnauthorizedOperationException` | RG-03, RD-ROL-03 |
| `UserNotActiveException` | RG-01, RD-ROL-01 |
| `DuplicateEmailException` | RD-ID-03 |
| `DuplicateIdentityDocumentException` | RD-ID-04 |
| `BuyerNotAllowedToPurchaseException` | Estado comercial del comprador |
| `ProductNotOwnedException` | RD-CAT-01 |
| `WarehouseNotOwnedException` | RG-03 |
| `InsufficientStockException` | RD-INV-02, RD-INV-03 |
| `DamagedStockException` | RD-INV-03 |
| `DuplicateInventoryItemException` | RD-ID-05, RD-INV-01 |
| `OrderNotModifiableException` | RD-PED-02 |
| `InvalidOrderTransitionException` | RD-PED-01 |
| `EmptyCartException` | Un pedido tiene al menos una línea |
| `ProductNotAvailableException` | RD-CAT-03, RD-CAT-05 |
| `InvoiceNotIssuedException` | RD-FAC-01 |
| `ShipmentNotAllowedException` | RD-LOG-01 |
| `ReturnNotAllowedException` | RD-POS-01 |
| `RefundExceedsInvoiceException` | RD-POS-02 |
