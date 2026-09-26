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

---

## 3. Definición de los puertos

### 3.1 PublicAccessPort

Operaciones que no requieren autenticación previa.

```java
public interface PublicAccessPort {
    User<?> login(EmailAddress email);
    Buyer registerBuyer(Buyer buyer);
    List<Product> consultPublicCatalog();
    Product consultPublishedProduct(ProductId productId);
}
```

| Método | Reglas aplicadas | Servicios de dominio |
|---|---|---|
| `login` | RG-01, RD-ROL-01, RD-ALC-01 | `LoginService` |
| `registerBuyer` | RD-ID-03, RD-ID-04, RD-VO-09 | `RegisterBuyerService` |
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

    List<SellerWarehouse> consultMyWarehouses(Seller seller);
    Warehouse consultMyWarehouse(Seller seller, WarehouseId warehouseId);

    InventoryMovement receiveStock(Seller seller, InventoryItem inventoryItem, Quantity quantity,
                                   InventoryMovementId movementId);
    InventoryMovement adjustStock(Seller seller, InventoryItem inventoryItem, Quantity newAvailableQuantity,
                                  InventoryMovementId movementId);
    InventoryItem markStockAsDamaged(Seller seller, InventoryItem inventoryItem);
    List<InventoryItem> consultStockOfMyProduct(Seller seller, ProductId productId);
}
```

| Método | Reglas aplicadas | Servicios de dominio |
|---|---|---|
| `registerProduct` | RD-CAT-01, RD-CAT-02, RD-ROL-06 | `RegisterProductService` |
| `publishProduct` / `suspendProduct` | RD-CAT-03 | `PublishProductService`, `SuspendProductService` |
| `discontinueProduct` | RD-CAT-04, RD-VO-13 | `DiscontinueProductService` |
| `addProductVariant` / `removeProductVariant` | RD-CAT-05 | `AddProductVariantService`, `RemoveProductVariantService` |
| `consultMyWarehouse(s)` | RG-03, RD-ROL-03 | `ConsultWarehouseService` |
| `receiveStock` / `adjustStock` | RD-INV-02, RD-INV-04 | `ReceiveStockService`, `AdjustStockService` |
| `markStockAsDamaged` | RD-INV-03, RD-VO-12 | `MarkDamagedService` |

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
}
```

| Método | Reglas aplicadas | Servicios de dominio |
|---|---|---|
| `reserveStock` | RD-INV-02, RD-INV-03, RD-INV-04 | `ReserveStockService` |
| `releaseReservation` | RD-INV-04 | `ReleaseReservationService` |
| `issueStockForSale` | RD-INV-04, RD-LOG-02 | `IssueForSaleService` |
| `restockReturn` | RD-POS-01, RD-INV-04 | `RestockReturnService` |
| `packShipment` | RD-LOG-01, RD-PED-03 | `PackShipmentService` |
| `dispatchShipment` | RD-PED-01, RD-LOG-02 | `DispatchShipmentService` |
| `confirmShipmentDelivery` | RD-PED-01, RD-PED-02 | `ConfirmShipmentDeliveryService` |

---

### 3.5 AdministratorPort (`ADMINISTRATOR`)

```java
public interface AdministratorPort {
    Seller registerSeller(User<?> administrator, SellerId sellerId, FullName fullName, EmailAddress email,
                          IdentityDocument identityDocument, UserStatus status, SellerWarehouse firstWarehouse);
    Seller consultSeller(User<?> administrator, SellerId sellerId);
    List<Seller> consultSellers(User<?> administrator);

    User<?> blockUser(User<?> administrator, User<?> target);
    User<?> activateUser(User<?> administrator, User<?> target);
    User<?> changeUserRole(User<?> administrator, User<?> target, UserRole newRole);

    MarketplaceWarehouse registerMarketplaceWarehouse(User<?> administrator, WarehouseId warehouseId);

    Refund issueRefund(User<?> administrator, ProductReturn productReturn, Invoice invoice, Money amount,
                       RefundId refundId);
    List<Refund> consultRefunds(User<?> administrator);
    ProductReturn consultReturn(User<?> administrator, ProductReturnId returnId);
}
```

| Método | Reglas aplicadas | Servicios de dominio |
|---|---|---|
| `registerSeller` | RD-ROL-05, RD-ROL-06, RD-ID-03, RD-ID-04 | `RegisterSellerService` |
| `blockUser` / `activateUser` | RG-01, RD-ROL-01 | `BlockUserService`, `ActivateUserService` |
| `changeUserRole` | RG-02, RD-ROL-02 | `ChangeUserRoleService` |
| `registerMarketplaceWarehouse` | RD-ROL-06 | `RegisterMarketplaceWarehouseService` |
| `issueRefund` | RD-POS-02, RD-ROL-06 | `IssueRefundService` |

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
