# Puertos de Salida (Output Ports) — NexusMarket

## 1. Introducción

Los **puertos de salida** son las interfaces que el dominio utiliza para alcanzar el mundo exterior
—persistencia, principalmente— sin conocer ninguna tecnología concreta. El dominio declara *qué*
necesita; los adaptadores de `adapters/persistence/` deciden *cómo* se resuelve.

Ubicación en el código: `src/main/java/application/domain/ports/out/`.

Esta separación es la que permite cumplir RD-ALC-02: el modelo no incorpora decisiones de tecnología
ni de almacenamiento de la información.

---

## 2. Principios generales

- **RD-POUT-01.** Un puerto de salida por agregado del dominio. No existen repositorios genéricos ni
  compartidos entre agregados no relacionados.
- **RD-POUT-02.** Las firmas operan sobre modelos de dominio y objetos de valor. Un puerto de salida
  jamás expone entidades ORM, documentos, DTOs ni anotaciones de framework.
- **RD-POUT-03.** Las consultas de unicidad que no pueden vivir dentro del objeto de valor
  (RD-VO-09) se exponen aquí: `existsByEmail`, `existsByIdentityDocument`.
- **RD-POUT-04.** Los identificadores de las nuevas entidades los aporta quien invoca el caso de
  uso, no el puerto: el dominio ya recibe el `Identifier` en el constructor de cada entidad
  (RD-ID-01).
- **RD-POUT-05.** Las búsquedas que pueden no encontrar resultado devuelven `Optional`; las que
  devuelven colecciones nunca devuelven `null`.

---

## 3. Catálogo de puertos de salida

| Puerto | Agregado | Reglas que sostiene |
|---|---|---|
| `UserRepositoryPort` | `User` / `Buyer` | RD-ID-02, RD-ID-03, RD-ID-04, RD-VO-09 |
| `SellerRepositoryPort` | `Seller` | RD-ROL-05 |
| `WarehouseRepositoryPort` | `Warehouse` | RD-INV-01, RG-03 |
| `ProductRepositoryPort` | `Product` | RD-CAT-01, RD-CAT-03 |
| `InventoryItemRepositoryPort` | `InventoryItem` | RD-INV-01, RD-ID-05 |
| `InventoryMovementRepositoryPort` | `InventoryMovement` | RD-INV-04 |
| `CartRepositoryPort` | `Cart` | RD-PED-04 |
| `OrderRepositoryPort` | `Order` | RD-PED-05 |
| `InvoiceRepositoryPort` | `Invoice` | RD-FAC-01 |
| `ShipmentRepositoryPort` | `Shipment` | RD-LOG-01, RD-LOG-02 |
| `ProductReturnRepositoryPort` | `ProductReturn` | RD-POS-01 |
| `RefundRepositoryPort` | `Refund` | RD-POS-02 |

---

## 4. Definición de los puertos

### 4.1 UserRepositoryPort

La unicidad del correo y del documento depende del conjunto de datos y no puede verificarse dentro
del objeto de valor (RD-VO-09): este puerto expone las consultas que los servicios usan para hacer
cumplir RD-ID-03 y RD-ID-04.

```java
public interface UserRepositoryPort {
    <U extends User<?>> U save(U user);
    Optional<User<?>> findByIdentifier(Identifier identifier);
    Optional<Buyer> findBuyerById(BuyerId buyerId);
    Optional<User<?>> findByEmail(EmailAddress email);
    boolean existsByEmail(EmailAddress email);
    boolean existsByIdentityDocument(IdentityDocument identityDocument);
    List<User<?>> findByRole(UserRole role);
    List<User<?>> findAll();
}
```

### 4.2 SellerRepositoryPort

```java
public interface SellerRepositoryPort {
    Seller save(Seller seller);
    Optional<Seller> findById(SellerId sellerId);
    List<Seller> findAll();
}
```

### 4.3 WarehouseRepositoryPort

```java
public interface WarehouseRepositoryPort {
    Warehouse save(Warehouse warehouse);
    Optional<Warehouse> findById(WarehouseId warehouseId);
    List<SellerWarehouse> findBySeller(SellerId sellerId);
    List<Warehouse> findAll();
}
```

`findBySeller` devuelve `SellerWarehouse` porque las bodegas del marketplace no pertenecen a ningún
vendedor.

### 4.4 ProductRepositoryPort

```java
public interface ProductRepositoryPort {
    Product save(Product product);
    Optional<Product> findById(ProductId productId);
    List<Product> findBySeller(SellerId sellerId);
    List<Product> findVisibleInCatalog();
    List<Product> findAll();
}
```

`findVisibleInCatalog` sostiene RD-CAT-03; el servicio vuelve a filtrar con
`Product.isVisibleInCatalog()` porque la visibilidad también exige al menos una variante
(RD-CAT-05).

### 4.5 InventoryItemRepositoryPort

```java
public interface InventoryItemRepositoryPort {
    InventoryItem save(InventoryItem inventoryItem);
    Optional<InventoryItem> findById(InventoryItemId inventoryItemId);
    Optional<InventoryItem> findByProductAndWarehouse(ProductId productId, WarehouseId warehouseId);
    List<InventoryItem> findByWarehouse(WarehouseId warehouseId);
    List<InventoryItem> findByProduct(ProductId productId);
    List<InventoryItem> findAll();
}
```

`findByProductAndWarehouse` materializa la clave única del inventario distribuido (RD-ID-05):
no existen dos registros de existencias para el mismo producto en la misma bodega.

### 4.6 InventoryMovementRepositoryPort

```java
public interface InventoryMovementRepositoryPort {
    InventoryMovement save(InventoryMovement inventoryMovement);
    Optional<InventoryMovement> findById(InventoryMovementId inventoryMovementId);
    List<InventoryMovement> findByInventoryItem(InventoryItemId inventoryItemId);
    List<InventoryMovement> findAll();
}
```

### 4.7 CartRepositoryPort

```java
public interface CartRepositoryPort {
    Cart save(Cart cart);
    Optional<Cart> findById(CartId cartId);
    Optional<Cart> findByBuyer(BuyerId buyerId);
}
```

### 4.8 OrderRepositoryPort

```java
public interface OrderRepositoryPort {
    Order save(Order order);
    Optional<Order> findById(OrderId orderId);
    List<Order> findByBuyer(BuyerId buyerId);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findAll();
}
```

### 4.9 InvoiceRepositoryPort

```java
public interface InvoiceRepositoryPort {
    Invoice save(Invoice invoice);
    Optional<Invoice> findById(InvoiceId invoiceId);
    Optional<Invoice> findByOrder(OrderId orderId);
    List<Invoice> findByBuyer(BuyerId buyerId);
    List<Invoice> findAll();
}
```

### 4.10 ShipmentRepositoryPort

```java
public interface ShipmentRepositoryPort {
    Shipment save(Shipment shipment);
    Optional<Shipment> findById(ShipmentId shipmentId);
    List<Shipment> findByOrder(OrderId orderId);
    List<Shipment> findByWarehouse(WarehouseId warehouseId);
    List<Shipment> findAll();
}
```

### 4.11 ProductReturnRepositoryPort

```java
public interface ProductReturnRepositoryPort {
    ProductReturn save(ProductReturn productReturn);
    Optional<ProductReturn> findById(ProductReturnId productReturnId);
    List<ProductReturn> findByOrder(OrderId orderId);
    List<ProductReturn> findAll();
}
```

### 4.12 RefundRepositoryPort

```java
public interface RefundRepositoryPort {
    Refund save(Refund refund);
    Optional<Refund> findById(RefundId refundId);
    Optional<Refund> findByProductReturn(ProductReturnId productReturnId);
    List<Refund> findAll();
}
```

---

## 5. Puertos fuera de esta entrega

La autenticación técnica está explícitamente fuera del dominio (RD-ALC-01), por lo que
`JwtServicePort` y `PasswordServicePort` **no** se declaran en esta etapa: aparecerán cuando se
implemente la capa de seguridad, junto con los adaptadores REST.
