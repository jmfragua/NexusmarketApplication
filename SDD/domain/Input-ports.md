# Puertos de Entrada (Input Ports) — NexusMarket

## 1. Introducción

Los **puertos de entrada** definen los contratos por los que los mecanismos de entrega externos
—controladores REST, filtros de seguridad— interactúan con el núcleo del dominio.

En esta arquitectura los puertos de entrada se organizan **estrictamente por rol del sistema**
(`UserRole`), más un puerto de acceso público para las operaciones que no requieren autenticación.
Esto garantiza que:

- Cada rol expone una interfaz cohesiva con exactamente las operaciones que le permite la matriz de
  responsabilidades (RD-ROL-06).
- Los controladores REST y la capa de seguridad evalúan los permisos directamente contra el puerto
  del rol destino.
- Los métodos operan exclusivamente sobre **modelos de dominio** y **objetos de valor**, nunca sobre
  DTOs ni tipos primitivos sueltos.

Ubicación en el código: `src/main/java/application/domain/ports/in/`.

---

## 2. Principios generales

### 2.1 Aislamiento por rol

Cada rol interactúa con la aplicación a través de su propia interfaz:

| Puerto | Rol (`UserRole`) | Responsabilidad |
|---|---|---|
| `PublicAccessPort` | — (público / sin autenticar) | Catálogo público, autoregistro de comprador, identificación |
| `BuyerPort` | `BUYER` | Direcciones, carrito, pedidos, facturas propias, devoluciones |
| `SellerPort` | `SELLER` | Catálogo propio, variantes, bodegas propias, inventario propio |
| `LogisticOperatorPort` | `LOGISTICS_OPERATOR` | Movimientos de bodega, empaque, despacho y entrega |
| `AdministratorPort` | `ADMINISTRATOR` | Alta de vendedores, estado de usuarios, bodegas del marketplace, reembolsos |
| `SupervisorPort` | `SUPERVISOR` | Consulta de solo lectura y reportes administrativos |

### 2.2 Parámetros de dominio

Todos los métodos reciben modelos de dominio (`User`, `Buyer`, `Seller`, `Order`, `Product`, …).
El objeto `User` que recibe cada operación representa al usuario autenticado que la ejecuta (RG-01)
y es el que las validaciones de rol y de pertenencia utilizan para hacer cumplir RG-03.

### 2.3 Regla de vendedores

`PublicAccessPort` **no** expone el registro de vendedores: los vendedores no pueden autoregistrarse
y son incorporados por el administrador junto con su primera bodega (RD-ROL-05). Esa operación vive
únicamente en `AdministratorPort`.

### 2.4 El carrito nace con el comprador

`registerBuyer` recibe un `CartId` además del comprador: el alta crea también su carrito activo
(RD-PED-04). Sin él, el comprador no tendría carrito que consultar y el flujo de compra sería
inalcanzable. Ver [user-services.md](services/user-services.md).

---

## 3. Definición de los puertos

### 3.1 PublicAccessPort

Operaciones que no requieren autenticación previa.

```java
public interface PublicAccessPort {
    User<?> login(EmailAddress email);
    Buyer registerBuyer(Buyer buyer, CartId cartId);
    List<Product> consultPublicCatalog();
    Product consultPublishedProduct(ProductId productId);
}
```

| Método | Reglas aplicadas | Servicios de dominio |
|---|---|---|
| `login` | RG-01, RD-ROL-01, RD-ALC-01 | `LoginService` |
| `registerBuyer` | RD-ID-03, RD-ID-04, RD-VO-09, RD-PED-04 | `RegisterBuyerService` |
| `consultPublicCatalog` | RD-CAT-03, RD-CAT-05 | `ConsultCatalogService` |
| `consultPublishedProduct` | RD-CAT-03 | `ConsultCatalogService` |

---

### 3.2 BuyerPort (`BUYER`)

El comprador administra únicamente la información necesaria para su participación comercial: nunca
administra información de otros compradores ni inventarios (RD-ROL-04).

```java
public interface BuyerPort {
    Buyer addAdditionalAddress(Buyer buyer, Address address);
    Buyer changePrimaryAddress(Buyer buyer, Address address);

    Cart consultMyCart(Buyer buyer);
    CartItem addCartItem(Buyer buyer, Cart cart, Product product, ProductVariantId variantId,
                         Quantity quantity, CartItemId cartItemId);
    Cart removeCartItem(Buyer buyer, Cart cart, CartItem item);
    Cart updateCartItemQuantity(Buyer buyer, Cart cart, CartItem item, Quantity quantity);
    Cart clearCart(Buyer buyer, Cart cart);

    Order confirmCart(Buyer buyer, Cart cart, OrderId orderId, Address deliveryAddress, List<OrderLine> lines);
    Order confirmPayment(Buyer buyer, Order order);
    Order consultMyOrder(Buyer buyer, OrderId orderId);
    List<Order> consultMyOrders(Buyer buyer);
    Invoice consultMyInvoice(Buyer buyer, Order order);
    List<Invoice> consultMyInvoices(Buyer buyer);

    ProductReturn requestReturn(Buyer buyer, Order order, OrderLine orderLine, Quantity quantity,
                                ProductReturnId returnId);
    List<ProductReturn> consultMyReturns(Buyer buyer, Order order);

    Buyer consultMyProfile(Buyer buyer);
    List<Shipment> consultMyShipments(Buyer buyer, Order order);
}
```

| Método | Reglas aplicadas | Servicios de dominio |
|---|---|---|
| `addCartItem` | RD-PED-04, RD-CAT-03, RD-CAT-05 | `AddCartItemService` |
| `removeCartItem` / `updateCartItemQuantity` / `clearCart` | RD-PED-04 | `RemoveCartItemService`, `UpdateCartItemQuantityService`, `ClearCartService` |
| `confirmCart` | RD-PED-01, RD-PED-04 | `ConfirmCartService` |
| `confirmPayment` | RD-PED-01, RD-PED-02, RD-PED-03 | `ConfirmPaymentService` |
| `consultMyOrder(s)` | RD-PED-05, RD-ROL-04 | `ConsultOrderService` |
| `consultMyInvoice(s)` | RD-FAC-01, RD-ROL-04 | `ConsultInvoiceService` |
| `requestReturn` | RD-POS-01 | `RequestReturnService` |
| `consultMyProfile` | RD-ROL-04 | `ValidateRoleAuthorizationService` |
| `consultMyShipments` | RD-PED-05, RD-ROL-04 | `ConsultShipmentService` |

---

### 3.3 SellerPort (`SELLER`)

```java
public interface SellerPort {
    Product registerProduct(Seller seller, Product product);
    Product publishProduct(Seller seller, Product product);
    Product suspendProduct(Seller seller, Product product);
    Product discontinueProduct(Seller seller, Product product);
    Product addProductVariant(Seller seller, Product product, ProductVariant variant);
    Product removeProductVariant(Seller seller, Product product, ProductVariant variant);
    List<Product> consultMyCatalog(Seller seller);
    Product consultMyProduct(Seller seller, ProductId productId);

    List<SellerWarehouse> consultMyWarehouses(Seller seller);
    Warehouse consultMyWarehouse(Seller seller, WarehouseId warehouseId);

    InventoryItem openStockRecord(Seller seller, Product product, Warehouse warehouse,
                                  InventoryItemId inventoryItemId);
    InventoryMovement receiveStock(Seller seller, InventoryItem inventoryItem, Quantity quantity,
                                   InventoryMovementId movementId);
    InventoryMovement adjustStock(Seller seller, InventoryItem inventoryItem, Quantity newAvailableQuantity,
                                  InventoryMovementId movementId);
    InventoryItem markStockAsDamaged(Seller seller, InventoryItem inventoryItem);
    List<InventoryItem> consultStockOfMyProduct(Seller seller, ProductId productId);
    InventoryItem consultStockRecord(Seller seller, InventoryItemId inventoryItemId);
}
```

| Método | Reglas aplicadas | Servicios de dominio |
|---|---|---|
| `registerProduct` | RD-CAT-01, RD-CAT-02, RD-ROL-06 | `RegisterProductService` |
| `publishProduct` / `suspendProduct` | RD-CAT-03 | `PublishProductService`, `SuspendProductService` |
| `discontinueProduct` | RD-CAT-04, RD-VO-13 | `DiscontinueProductService` |
| `addProductVariant` / `removeProductVariant` | RD-CAT-05 | `AddProductVariantService`, `RemoveProductVariantService` |
| `consultMyWarehouse(s)` | RG-03, RD-ROL-03 | `ConsultWarehouseService` |
| `openStockRecord` | RD-INV-01, RD-INV-05, RD-ID-05, RG-03 | `OpenStockRecordService` |
| `receiveStock` / `adjustStock` | RD-INV-02, RD-INV-04 | `ReceiveStockService`, `AdjustStockService` |
| `markStockAsDamaged` | RD-INV-03, RD-VO-12 | `MarkDamagedService` |
| `consultMyProduct` | RD-CAT-01, RG-03 | `ValidateSellerOwnershipService` |
| `consultStockRecord` | RD-ROL-06, RD-INV-01, RG-03 | `ValidateRoleAuthorizationService`, `ValidateSellerOwnershipService.validateStockRecord` |
| `receiveStock` / `adjustStock` / `markStockAsDamaged` (pertenencia) | RG-03 | `ValidateSellerOwnershipService.validateStockRecord` |
| `consultStockOfMyProduct` | RD-CAT-01, RG-03 | `ValidateSellerOwnershipService.validateProduct` |

`consultMyProduct` valida la pertenencia **mientras carga**: un vendedor que pida el producto de
otro recibe `ProductNotOwnedException` en la resolución, no en la operación posterior. Es lo que
permite que `SellerRestController` no necesite ningún puerto de salida (RD-PIN-06).

---

### 3.4 LogisticOperatorPort (`LOGISTICS_OPERATOR`)

```java
public interface LogisticOperatorPort {
    Warehouse consultWarehouse(User<?> operator, WarehouseId warehouseId);
    List<Warehouse> consultWarehouses(User<?> operator);
    List<InventoryItem> consultStockOfWarehouse(User<?> operator, WarehouseId warehouseId);

    InventoryMovement receiveStock(User<?> operator, InventoryItem inventoryItem, Quantity quantity,
                                   InventoryMovementId movementId);
    InventoryMovement reserveStock(User<?> operator, OrderLine orderLine, InventoryItem inventoryItem,
                                   InventoryMovementId movementId);
    InventoryMovement releaseReservation(User<?> operator, OrderLine orderLine, InventoryItem inventoryItem,
                                         InventoryMovementId movementId);
    InventoryMovement issueStockForSale(User<?> operator, OrderLine orderLine, InventoryItem inventoryItem,
                                        InventoryMovementId movementId);
    InventoryMovement restockReturn(User<?> operator, ProductReturn productReturn, InventoryItem inventoryItem,
                                    InventoryMovementId movementId);

    Shipment packShipment(User<?> operator, Shipment shipment, Order order, Collection<Product> products);
    Shipment dispatchShipment(User<?> operator, Shipment shipment, Order order);
    Shipment confirmShipmentDelivery(User<?> operator, Shipment shipment, Order order);
    List<Order> consultOrdersByStatus(User<?> operator, OrderStatus status);
    List<Shipment> consultShipmentsOfOrder(User<?> operator, Order order);

    // Resolución y entrada por identificador (RD-PIN-06)
    Order consultOrder(User<?> operator, OrderId orderId);
    InventoryItem consultStockRecord(User<?> operator, InventoryItemId inventoryItemId);
    InventoryMovement restockReturn(User<?> operator, ProductReturnId returnId, InventoryItemId inventoryItemId,
                                    InventoryMovementId movementId);
    Shipment packShipment(User<?> operator, ShipmentId shipmentId, OrderId orderId,
                          WarehouseId originWarehouseId, Address deliveryAddress);
    Shipment dispatchShipment(User<?> operator, ShipmentId shipmentId, OrderId orderId);
    Shipment confirmShipmentDelivery(User<?> operator, ShipmentId shipmentId, OrderId orderId);
}
```

Las cuatro sobrecargas por identificador no son azúcar sintáctico del adaptador: `packShipment`
decide si reutiliza el envío existente o lo abre, y recopila los productos del pedido para
determinar si requiere logística (RD-LOG-01). Esa decisión es del caso de uso. Mientras vivía en el
controlador, el adaptador REST tenía que conocer cinco puertos de salida.

| Método | Reglas aplicadas | Servicios de dominio |
|---|---|---|
| `reserveStock` | RD-INV-02, RD-INV-03, RD-INV-04 | `ReserveStockService` |
| `releaseReservation` | RD-INV-04 | `ReleaseReservationService` |
| `issueStockForSale` | RD-INV-04, RD-LOG-02 | `IssueForSaleService` |
| `restockReturn` | RD-POS-01, RD-INV-04 | `RestockReturnService` |
| `packShipment` | RD-LOG-01, RD-PED-03 | `PackShipmentService` |
| `dispatchShipment` | RD-PED-01, RD-LOG-02 | `DispatchShipmentService` |
| `confirmShipmentDelivery` | RD-PED-01, RD-PED-02 | `ConfirmShipmentDeliveryService` |
| `consultShipmentsOfOrder` | RD-LOG-01, RD-ROL-06 | `ConsultShipmentService` |
| `consultOrder` / `consultStockRecord` | RD-ROL-06 | `ValidateRoleAuthorizationService` |

---

### 3.5 AdministratorPort (`ADMINISTRATOR`)

```java
public interface AdministratorPort {
    Seller registerSeller(User<?> administrator, SellerId sellerId, FullName fullName, EmailAddress email,
                          IdentityDocument identityDocument, UserStatus status, SellerWarehouse firstWarehouse);
    Seller consultSeller(User<?> administrator, SellerId sellerId);
    List<Seller> consultSellers(User<?> administrator);

    StaffUser registerStaffUser(User<?> administrator, UserId userId, FullName fullName, EmailAddress email,
                                IdentityDocument identityDocument, UserRole role);

    User<?> consultUser(User<?> administrator, Identifier userId);
    User<?> blockUser(User<?> administrator, User<?> target);
    User<?> activateUser(User<?> administrator, User<?> target);
    User<?> changeUserRole(User<?> administrator, User<?> target, UserRole newRole);

    MarketplaceWarehouse registerMarketplaceWarehouse(User<?> administrator, WarehouseId warehouseId);

    Invoice issueInvoice(User<?> administrator, OrderId orderId, InvoiceId invoiceId);
    Invoice consultInvoice(User<?> administrator, InvoiceId invoiceId);

    Refund issueRefund(User<?> administrator, ProductReturn productReturn, Invoice invoice, Money amount,
                       RefundId refundId);
    List<Refund> consultRefunds(User<?> administrator);
    ProductReturn consultReturn(User<?> administrator, ProductReturnId returnId);

    List<User<?>> consultUsers(User<?> administrator);
    List<Warehouse> consultWarehouses(User<?> administrator);
    Invoice consultInvoiceOfOrder(User<?> administrator, OrderId orderId);
}
```

| Método | Reglas aplicadas | Servicios de dominio |
|---|---|---|
| `registerSeller` | RD-ROL-05, RD-ROL-06, RD-ID-03, RD-ID-04 | `RegisterSellerService` |
| `registerStaffUser` | RD-ROL-07, RD-ROL-09, RD-ID-02, RD-ID-03, RD-ID-04 | `RegisterStaffUserService` |
| `blockUser` / `activateUser` | RG-01, RD-ROL-01 | `BlockUserService`, `ActivateUserService` |
| `changeUserRole` | RG-02, RD-ROL-02 | `ChangeUserRoleService` |
| `registerMarketplaceWarehouse` | RD-ROL-06 | `RegisterMarketplaceWarehouseService` |
| `issueRefund` | RD-POS-02, RD-ROL-06 | `IssueRefundService` |
| `consultUser` | RG-01, RD-ROL-06 | `ValidateRoleAuthorizationService` |
| `issueInvoice` | RD-FAC-01 | `IssueInvoiceService` |
| `consultInvoice` | RD-FAC-01, RD-ROL-06 | `ValidateRoleAuthorizationService` |
| `consultUsers` | OBJ-01, RD-ROL-06 | `ValidateRoleAuthorizationService` |
| `consultWarehouses` | OBJ-04, RD-ROL-06 | `ConsultWarehouseService` |
| `consultInvoiceOfOrder` | RD-FAC-01, RD-POS-02 | `ConsultInvoiceService` |

La facturación corresponde al vendedor y al administrador (RD-FAC-01). Figura aquí para que el
administrador no tenga que salirse de su puerto; el servicio detrás sigue validando el rol, que es
donde vive la regla. `consultUser` está restringido a `ADMINISTRATOR` porque solo él gestiona el
estado y el rol de los usuarios: no es una búsqueda libre.

---

### 3.6 SupervisorPort (`SUPERVISOR`)

Perfil de consulta y seguimiento operativo: **no ejecuta procesos de modificación** (RD-ROL-06).
Todos sus métodos son de solo lectura.

```java
public interface SupervisorPort {
    List<Order> consultOrdersByStatus(User<?> supervisor, OrderStatus status);
    List<Order> consultCompletedOrders(User<?> supervisor);
    List<Invoice> consultIssuedInvoices(User<?> supervisor);
    Optional<Money> consultInvoicedTotal(User<?> supervisor);
    List<InventoryItem> consultStockByWarehouse(User<?> supervisor, WarehouseId warehouseId);
    List<InventoryItem> consultDamagedStock(User<?> supervisor);
    List<InventoryMovement> consultInventoryMovements(User<?> supervisor);
    List<Refund> consultRefunds(User<?> supervisor);
}
```

| Método | Reglas aplicadas | Servicios de dominio |
|---|---|---|
| `consultCompletedOrders` / `consultIssuedInvoices` / `consultInvoicedTotal` | OBJ-12, RD-ROL-06 | `ConsultSalesReportService` |
| `consultStockByWarehouse` / `consultDamagedStock` / `consultInventoryMovements` | RD-INV-01, RD-INV-03, RD-INV-04 | `ConsultInventoryReportService` |
| `consultRefunds` | RD-POS-02 | `ConsultRefundService` |

---

## 4. Reglas de diseño de los puertos de entrada

- **RD-PIN-01.** Un puerto de entrada por rol del sistema, más el puerto público. Ningún puerto se
  organiza por entidad ni por agregado.
- **RD-PIN-02.** Las firmas operan sobre modelos de dominio y objetos de valor; nunca sobre DTOs,
  cadenas sueltas ni tipos primitivos que representen conceptos acotados (RD-VO-15).
- **RD-PIN-03.** Toda operación recibe el usuario autenticado que la ejecuta (RG-01). Las
  implementaciones delegan la verificación de rol y pertenencia en los servicios de
  `domain/services/authorization/`.
- **RD-PIN-04.** Un puerto no expone operaciones fuera del alcance de su rol: la matriz de
  responsabilidades (RD-ROL-06) es la que decide qué método vive en qué puerto.
- **RD-PIN-05.** Las interfaces no declaran dependencias de framework: son contratos de dominio puro.
- **RD-PIN-06.** Cada puerto expone la **resolución por identificador** de las entidades sobre las
  que opera, con la validación de rol y pertenencia incluida en la carga. Sin esto, el adaptador que
  recibe un identificador por HTTP no tiene más salida que inyectar un puerto de salida, lo que
  rompería RD-REST-02 y haría que la capa de entrega conociera la de persistencia. Donde el caso de
  uso tenga que *decidir* algo para resolver —abrir un envío que no existe, recopilar los productos
  de un pedido— el puerto expone una sobrecarga que acepta identificadores y toma esa decisión
  adentro, nunca en el controlador.
