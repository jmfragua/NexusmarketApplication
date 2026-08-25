# Modelo de Dominio — NexusMarket

## 1. Introducción

NexusMarket es una plataforma digital centralizada que actúa como intermediario comercial entre
compradores y vendedores. El sistema administra integralmente la operación: desde el registro de
usuarios y la publicación de productos, hasta la logística, la facturación y la posventa,
garantizando trazabilidad y coordinación entre todos los participantes.

Este documento describe el **modelo de dominio**: el conjunto de entidades que representan los
conceptos del negocio, sus atributos, sus relaciones y las reglas que deben cumplirse en todo
momento. El modelo se deriva exclusivamente de la *Especificación Funcional del Negocio —
NexusMarket*.

El propósito del modelo es:

- Representar en una sola estructura coherente los doce objetivos funcionales del negocio
  (OBJ-01 a OBJ-12).
- Encapsular las reglas de negocio junto a los datos que protegen, en lugar de dispersarlas en
  capas de aplicación o de presentación.
- Servir como base para la implementación, dejando explícitas las invariantes que ninguna
  operación puede violar (unicidad de usuarios, no negatividad del inventario, inmutabilidad de
  pedidos finalizados).

La especificación deja **fuera de alcance** interfaces gráficas, aplicaciones móviles, portales
web, mecanismos de autenticación técnica, tecnologías de implementación, arquitectura del software
y detalles de almacenamiento. Por lo tanto, este modelo describe únicamente el dominio del
negocio, sin decisiones de persistencia ni de infraestructura.

---

## 2. Jerarquía de clases de dominio

```
DomainEntity  (abstracta — raíz de toda entidad con identidad propia)
│
├── User  (abstracta)
│   ├── Buyer
│   └── Seller
│
├── Warehouse  (abstracta)
│   ├── MarketplaceWarehouse
│   └── SellerWarehouse
│
├── Product  (abstracta)
│   ├── PhysicalProduct
│   └── DigitalProduct
│
├── ProductVariant
│
├── InventoryItem
│   └── InventoryMovement
│
├── Cart
│   └── CartItem
│
├── Order
│   └── OrderLine
│
├── Invoice
│
├── Shipment
│
├── ProductReturn
│
└── Refund
```

### Nota sobre los roles

La especificación establece que cada usuario tiene **un único rol** (RG-02) y que ningún
participante puede administrar información fuera de su rol (RG-03). Los cinco participantes del
negocio son: Comprador, Vendedor, Operador Logístico, Administrador y Supervisor.

De ellos, únicamente **Comprador** y **Vendedor** reciben atributos y reglas propias en la
especificación (Dominios 2 y 3), por lo que se modelan como especializaciones de `User`. Los roles
**Operador Logístico**, **Administrador** y **Supervisor** no aportan atributos adicionales al
dominio: se representan mediante el objeto de valor `UserRole` sobre `User`.

---

## 3. Relaciones de dominio

```
                          ┌───────────────────────────┐
                          │           User            │
                          │  identifier / role / ...  │
                          └─────────────┬─────────────┘
                                        │ especializa
                        ┌───────────────┴───────────────┐
                        │                               │
              ┌─────────▼─────────┐           ┌─────────▼─────────┐
              │       Buyer       │           │      Seller       │
              └─────────┬─────────┘           └─────────┬─────────┘
                        │                               │
                        │ realiza                       │ es propietario de
                        │                               │
                        │                     ┌─────────▼──────────┐
                        │                     │  SellerWarehouse   │
                        │                     └─────────┬──────────┘
                        │                               │
                        │                               │  ┌──────────────────────┐
                        │                               │  │ MarketplaceWarehouse │
                        │                               │  └──────────┬───────────┘
                        │                               │             │
                        │                               └──────┬──────┘
                        │                                      │
                        │                             ┌────────▼────────┐
                        │                             │    Warehouse    │
                        │                             └────────┬────────┘
                        │                                      │ almacena en
                        │                                      │
                        │                             ┌────────▼────────┐
                        │                             │  InventoryItem  │  (1 producto + 1 bodega)
                        │                             └────────┬────────┘
                        │                                      │ registra
                        │                             ┌────────▼──────────┐
                        │                             │ InventoryMovement │
                        │                             └───────────────────┘
                        │                                      ▲
                        │                                      │ afecta a
                        │            ┌─────────────────────────┘
                        │            │
              ┌─────────▼─────────┐  │        ┌───────────────────────┐
              │       Cart        │  │        │        Seller         │
              └─────────┬─────────┘  │        └───────────┬───────────┘
                        │ contiene   │                    │ registra
              ┌─────────▼─────────┐  │        ┌───────────▼───────────┐
              │     CartItem      │──┼───────▶│       Product         │
              └───────────────────┘  │        │  (Physical/Digital)   │
                        │            │        └───────────┬───────────┘
                        │ se confirma│                    │ define
                        │ como       │        ┌───────────▼───────────┐
              ┌─────────▼─────────┐  │        │    ProductVariant     │
              │       Order       │  │        └───────────────────────┘
              └─────────┬─────────┘  │                    ▲
                        │ contiene   │                    │ referencia
              ┌─────────▼─────────┐  │                    │
              │     OrderLine     │──┴────────────────────┘
              └───────────────────┘
                        │
        ┌───────────────┼────────────────┬───────────────────┐
        │ genera        │ origina        │ puede originar    │
┌───────▼───────┐ ┌─────▼──────┐ ┌───────▼────────┐ ┌────────▼────────┐
│    Invoice    │ │  Shipment  │ │ ProductReturn  │ │     Refund      │
└───────────────┘ └────────────┘ └───────┬────────┘ └────────▲────────┘
                    (solo productos      │                   │
                     físicos)            └───────────────────┘
                                              da lugar a
```

### Cardinalidades principales

| Relación | Cardinalidad | Regla asociada |
|---|---|---|
| `Seller` → `SellerWarehouse` | 1 a N (mínimo 1) | El Administrador registra al vendedor **y su primera bodega**. |
| `Seller` → `Product` | 1 a N | El vendedor registra y administra sus propios productos. |
| `Product` → `ProductVariant` | 1 a N | Las variantes son una lista de diferencias (color, talla, modelo). |
| `Product` + `Warehouse` → `InventoryItem` | 1 a 1 por par | El inventario está vinculado **obligatoriamente** a un producto y una bodega específica. |
| `InventoryItem` → `InventoryMovement` | 1 a N | Todo cambio de existencias queda registrado como movimiento. |
| `Buyer` → `Cart` | 1 a 1 (activo) | El carrito es la selección provisional de productos del comprador. |
| `Cart` → `CartItem` | 1 a N | — |
| `Buyer` → `Order` | 1 a N | El comprador nunca administra pedidos de otros compradores. |
| `Order` → `OrderLine` | 1 a N | — |
| `Order` → `Invoice` | 1 a 1 | La facturación es la información comercial asociada a la venta. |
| `Order` → `Shipment` | 1 a N (solo físicos) | Los productos digitales no generan envío. |
| `Order` → `ProductReturn` | 1 a N | — |
| `ProductReturn` → `Refund` | 1 a 1 | El reembolso deriva de una devolución. |

---

## 4. Entidades del dominio

### 4.1 DomainEntity

**Descripción.** Clase base abstracta de todo objeto de dominio que posee identidad propia y ciclo
de vida. Dos entidades son la misma si comparten identificador, aunque sus demás atributos difieran.

**Hereda de.** — (raíz de la jerarquía)

| Atributo | Tipo | Descripción |
|---|---|---|
| `identifier` | `Identifier` | Identifica de forma única a la entidad dentro de su tipo. |

**Relaciones.** Todas las entidades del modelo heredan de `DomainEntity`.

**Operaciones generadas.**

- `sameIdentityAs(other): boolean` — compara entidades por identidad, nunca por atributos.

---

### 4.2 User

**Descripción.** Base de autenticación e identificación del Marketplace (Dominio 1). Representa a
toda persona autorizada para interactuar con el sistema. Garantiza la correcta identificación y el
estado operativo de los usuarios.

**Hereda de.** `DomainEntity`

| Atributo | Tipo | Descripción |
|---|---|---|
| `identifier` | `Identifier` | Identifica de forma única al usuario. Obligatorio y único. |
| `fullName` | `FullName` | Nombre oficial del usuario. Obligatorio, no vacío. |
| `email` | `EmailAddress` | Medio principal de acceso y comunicación. Obligatorio y único en la plataforma. |
| `identityDocument` | `IdentityDocument` | Documento de identidad. Único en la plataforma. |
| `role` | `UserRole` | Define las responsabilidades y permisos. Obligatorio y único por usuario. |
| `status` | `UserStatus` | Condición operativa (Activo, Bloqueado). Obligatorio, tomado de catálogo definido. |

**Relaciones.**

- Especializado por `Buyer` y `Seller`.
- Toda operación del sistema se ejecuta en nombre de un `User` autenticado (RG-01).

**Operaciones generadas.**

- `block()` / `activate()` — cambian el estado operativo del usuario.
- `changeRole(newRole)` — reasigna el rol; debe seguir siendo único por usuario.
- `canOperateOn(resource): boolean` — verifica que el recurso pertenece al ámbito de su rol (RG-03).
- `updateContactEmail(email)` — valida unicidad antes de aplicar el cambio.

---

### 4.3 Buyer

**Descripción.** Usuario que adquiere productos publicados (Dominio 2). Administra únicamente la
información necesaria para su participación en procesos comerciales.

**Hereda de.** `User`

| Atributo | Tipo | Descripción |
|---|---|---|
| `primaryAddress` | `Address` | Ubicación habitual para entregas. Obligatorio. |
| `additionalAddresses` | `List<Address>` | Ubicaciones secundarias de entrega. Opcional. |
| `commercialStatus` | `BuyerCommercialStatus` | Condición del comprador para realizar compras. Obligatorio. |

**Relaciones.**

- Posee un `Cart` activo.
- Realiza uno o varios `Order`.
- Puede solicitar `ProductReturn` y recibir `Refund`.

**Restricción clave.** El comprador **nunca** administrará información de otros compradores ni
inventarios.

**Operaciones generadas.**

- `addAdditionalAddress(address)` / `removeAdditionalAddress(address)`.
- `changePrimaryAddress(address)`.
- `canPurchase(): boolean` — evalúa el `commercialStatus` antes de confirmar un pedido.
- `placeOrder(cart): Order` — confirma el carrito como pedido.
- `requestReturn(orderLine): ProductReturn`.

---

### 4.4 Seller

**Descripción.** Responsable de comercializar productos: los registra y los administra (Dominio 3).

**Hereda de.** `User`

| Atributo | Tipo | Descripción |
|---|---|---|
| `identifier` | `Identifier` | Heredado de `User`; identifica al vendedor. |
| `status` | `UserStatus` | Heredado de `User`; condición operativa del vendedor. |

**Relaciones.**

- Es propietario de una o varias `SellerWarehouse` (al menos la primera, registrada en su
  incorporación).
- Registra y administra sus `Product`.
- Administra el `InventoryItem` de sus productos, junto con el Operador Logístico.

**Regla de negocio.** Los vendedores **no pueden auto-registrarse**; son incorporados por el
Administrador.

**Operaciones generadas.**

- `registerProduct(product)` — alta de producto en el catálogo propio.
- `publishProduct(product)` / `suspendProduct(product)` / `discontinueProduct(product)`.
- `ownsWarehouse(warehouse): boolean`.
- `ownsProduct(product): boolean` — soporte de RG-03.

---

### 4.5 Warehouse

**Descripción.** Lugar donde se administra el inventario físico (Dominio 4). Controla los espacios
físicos de almacenamiento.

**Hereda de.** `DomainEntity`

| Atributo | Tipo | Descripción |
|---|---|---|
| `identifier` | `Identifier` | Identifica de forma única a la bodega. |
| `type` | `WarehouseType` | Clasificación: bodega del Marketplace o bodega de Vendedor. |

**Relaciones.**

- Contiene `InventoryItem` (existencias por producto).
- Es el origen del despacho de un `Shipment`.
- Es administrada operativamente por usuarios con rol Operador Logístico.

**Operaciones generadas.**

- `stockOf(product): InventoryItem`.
- `receive(product, quantity)` — genera un movimiento de tipo Ingreso.
- `dispatch(orderLine)` — genera un movimiento de tipo Salida por venta.

---

### 4.6 MarketplaceWarehouse

**Descripción.** Bodega perteneciente al Marketplace, operada directamente por la plataforma.

**Hereda de.** `Warehouse`

| Atributo | Tipo | Descripción |
|---|---|---|
| `type` | `WarehouseType` | Fijo en `MARKETPLACE`. |

**Relaciones.** Las mismas de `Warehouse`; no está asociada a un `Seller`.

**Operaciones generadas.**

- Hereda las operaciones de `Warehouse`.

---

### 4.7 SellerWarehouse

**Descripción.** Bodega perteneciente a un vendedor. Se registra junto con la incorporación del
vendedor por parte del Administrador.

**Hereda de.** `Warehouse`

| Atributo | Tipo | Descripción |
|---|---|---|
| `type` | `WarehouseType` | Fijo en `SELLER`. |
| `sellerId` | `Identifier` | Vendedor propietario de la bodega. Obligatorio. |

**Relaciones.**

- Pertenece a un `Seller` (N a 1).

**Operaciones generadas.**

- Hereda las operaciones de `Warehouse`.
- `belongsTo(seller): boolean` — soporte de RG-03.

---

### 4.8 Product

**Descripción.** Bien físico o digital ofrecido en el catálogo (Dominio 5). El catálogo diferencia
entre productos físicos —que requieren inventario y despacho— y productos digitales —de entrega
inmediata tras el pago—.

**Hereda de.** `DomainEntity`

| Atributo | Tipo | Descripción |
|---|---|---|
| `identifier` | `Identifier` | Identifica de forma única al producto. |
| `sellerId` | `Identifier` | Vendedor que registra y administra el producto. |
| `productType` | `ProductType` | Físico o Digital. |
| `variants` | `List<ProductVariant>` | Diferencias de color, talla, modelo, etc. |
| `status` | `ProductStatus` | Publicado, Suspendido o Descontinuado. |

**Relaciones.**

- Pertenece a un `Seller`.
- Define una lista de `ProductVariant`.
- Es referenciado por `InventoryItem`, `CartItem` y `OrderLine`.

**Operaciones generadas.**

- `publish()` / `suspend()` / `discontinue()` — transiciones del estado de catálogo.
- `addVariant(variant)` / `removeVariant(variant)`.
- `isVisibleInCatalog(): boolean` — verdadero solo en estado Publicado.
- `requiresInventory(): boolean` — verdadero solo para productos físicos.

---

### 4.9 PhysicalProduct

**Descripción.** Producto físico. Requiere existencias en inventario y despacho logístico para su
entrega.

**Hereda de.** `Product`

| Atributo | Tipo | Descripción |
|---|---|---|
| `productType` | `ProductType` | Fijo en `PHYSICAL`. |

**Relaciones.**

- Tiene `InventoryItem` en una o varias `Warehouse`.
- Su `OrderLine` origina un `Shipment`.

**Operaciones generadas.**

- `requiresInventory(): boolean` — siempre verdadero.
- `availableStockIn(warehouse): Quantity`.

---

### 4.10 DigitalProduct

**Descripción.** Producto digital. No requiere inventario ni despacho: se entrega de forma
inmediata una vez confirmado el pago.

**Hereda de.** `Product`

| Atributo | Tipo | Descripción |
|---|---|---|
| `productType` | `ProductType` | Fijo en `DIGITAL`. |

**Relaciones.**

- No se asocia a `InventoryItem` ni a `Shipment`.

**Operaciones generadas.**

- `requiresInventory(): boolean` — siempre falso.
- `deliverOnPayment(order)` — entrega inmediata tras la confirmación del pago.

---

### 4.11 ProductVariant

**Descripción.** Diferencia concreta de un producto (color, talla, modelo, etc.). Es la unidad
sobre la que se selecciona y se comercializa.

**Hereda de.** `DomainEntity`

| Atributo | Tipo | Descripción |
|---|---|---|
| `identifier` | `Identifier` | Identifica de forma única a la variante dentro del producto. |
| `productId` | `Identifier` | Producto al que pertenece la variante. |
| `attributes` | `List<VariantAttribute>` | Conjunto de diferencias que definen la variante. |

**Relaciones.**

- Pertenece a un `Product` (N a 1).
- Es referenciada por `CartItem` y `OrderLine`.

**Operaciones generadas.**

- `describe(): String` — composición legible de sus atributos.
- `matches(attributes): boolean`.

---

### 4.12 InventoryItem

**Descripción.** Existencias disponibles para comercialización (Dominio 6). El inventario es
distribuido y debe estar vinculado obligatoriamente a **un producto** y **una bodega específica**.

**Hereda de.** `DomainEntity`

| Atributo | Tipo | Descripción |
|---|---|---|
| `identifier` | `Identifier` | Identifica de forma única la existencia. |
| `productId` | `Identifier` | Producto al que corresponden las existencias. Obligatorio. |
| `warehouseId` | `Identifier` | Bodega donde se encuentran las existencias. Obligatorio. |
| `availableQuantity` | `Quantity` | Existencias disponibles para reservar. Nunca negativa. |
| `reservedQuantity` | `Quantity` | Existencias comprometidas en pedidos aún no despachados. |
| `condition` | `StockCondition` | Condición de las existencias (Disponible, Dañado). |
| `movements` | `List<InventoryMovement>` | Historial de movimientos que afectaron la existencia. |

**Relaciones.**

- Vincula un `Product` con una `Warehouse` (par único).
- Registra `InventoryMovement`.

**Restricciones.**

- No se permitirán existencias negativas bajo ninguna circunstancia.
- No se puede reservar inventario inexistente o marcado como "Dañado".

**Operaciones generadas.**

- `receive(quantity)` — movimiento de Ingreso.
- `reserve(quantity)` — movimiento de Reserva; falla si no hay disponibilidad o si está Dañado.
- `releaseReservation(quantity)`.
- `issueForSale(quantity)` — movimiento de Salida por venta.
- `adjust(quantity, reason)` — movimiento de Ajuste.
- `returnStock(quantity)` — movimiento de Devolución.
- `canReserve(quantity): boolean`.

---

### 4.13 InventoryMovement

**Descripción.** Registro de un cambio en las existencias. Garantiza la trazabilidad del inventario
distribuido.

**Hereda de.** `DomainEntity`

| Atributo | Tipo | Descripción |
|---|---|---|
| `identifier` | `Identifier` | Identifica de forma única al movimiento. |
| `inventoryItemId` | `Identifier` | Existencia afectada por el movimiento. |
| `movementType` | `InventoryMovementType` | Ingreso, Reserva, Salida por venta, Ajuste o Devolución. |
| `quantity` | `Quantity` | Cantidad involucrada en el movimiento. |

**Relaciones.**

- Pertenece a un `InventoryItem` (N a 1).
- Puede originarse en un `OrderLine` (reserva, salida por venta) o en un `ProductReturn`
  (devolución).

**Operaciones generadas.**

- `isInbound(): boolean` — verdadero para Ingreso y Devolución.
- `isOutbound(): boolean` — verdadero para Salida por venta.
- `appliesTo(inventoryItem): boolean`.

---

### 4.14 Cart

**Descripción.** Selección provisional de productos realizada por el comprador (Dominio 7). Es el
estado inicial del ciclo del pedido.

**Hereda de.** `DomainEntity`

| Atributo | Tipo | Descripción |
|---|---|---|
| `identifier` | `Identifier` | Identifica de forma única al carrito. |
| `buyerId` | `Identifier` | Comprador propietario del carrito. |
| `items` | `List<CartItem>` | Productos seleccionados provisionalmente. |

**Relaciones.**

- Pertenece a un `Buyer` (1 a 1 activo).
- Contiene `CartItem`.
- Se confirma como `Order`.

**Operaciones generadas.**

- `addItem(productVariant, quantity)`.
- `removeItem(cartItem)`.
- `updateQuantity(cartItem, quantity)`.
- `clear()`.
- `confirm(): Order` — genera el pedido y lo lleva al estado Pendiente de Pago.

---

### 4.15 CartItem

**Descripción.** Línea de selección provisional dentro del carrito.

**Hereda de.** `DomainEntity`

| Atributo | Tipo | Descripción |
|---|---|---|
| `identifier` | `Identifier` | Identifica de forma única la línea del carrito. |
| `productId` | `Identifier` | Producto seleccionado. |
| `productVariantId` | `Identifier` | Variante seleccionada del producto. |
| `quantity` | `Quantity` | Cantidad seleccionada. |

**Relaciones.**

- Pertenece a un `Cart` (N a 1).
- Referencia un `Product` y una `ProductVariant`.

**Operaciones generadas.**

- `changeQuantity(quantity)`.
- `toOrderLine(): OrderLine` — conversión al confirmar el carrito.

---

### 4.16 Order

**Descripción.** Solicitud de compra realizada por un comprador (Dominio 7). Representa el
compromiso comercial formal; su ciclo de vida es el proceso central del sistema.

**Hereda de.** `DomainEntity`

| Atributo | Tipo | Descripción |
|---|---|---|
| `identifier` | `Identifier` | Identifica de forma única al pedido. |
| `buyerId` | `Identifier` | Comprador que realiza el pedido. |
| `status` | `OrderStatus` | Estado del ciclo: Carrito, Pendiente de Pago, Pagado, Despachado, Entregado/Finalizado. |
| `deliveryAddress` | `Address` | Dirección de entrega seleccionada por el comprador. |
| `lines` | `List<OrderLine>` | Líneas del pedido. |
| `totalAmount` | `Money` | Valor comercial total del pedido. |

**Relaciones.**

- Pertenece a un `Buyer`.
- Contiene `OrderLine`.
- Genera una `Invoice` al confirmarse el pago.
- Origina uno o varios `Shipment` cuando incluye productos físicos.
- Puede originar `ProductReturn` y, en consecuencia, `Refund`.

**Restricción clave.** Un pedido finalizado **no podrá ser modificado bajo ninguna circunstancia**.

**Operaciones generadas.**

- `confirmPayment()` — transición Pendiente de Pago → Pagado; inicia los procesos de alistamiento.
- `dispatch()` — transición Pagado → Despachado.
- `completeDelivery()` — transición Despachado → Entregado/Finalizado.
- `isModifiable(): boolean` — falso una vez finalizado.
- `belongsTo(buyer): boolean` — soporte de RG-03.
- `containsPhysicalProducts(): boolean` — determina si requiere logística.

---

### 4.17 OrderLine

**Descripción.** Línea de un pedido: producto, variante y cantidad comprometidos comercialmente.

**Hereda de.** `DomainEntity`

| Atributo | Tipo | Descripción |
|---|---|---|
| `identifier` | `Identifier` | Identifica de forma única la línea del pedido. |
| `orderId` | `Identifier` | Pedido al que pertenece la línea. |
| `productId` | `Identifier` | Producto solicitado. |
| `productVariantId` | `Identifier` | Variante solicitada. |
| `quantity` | `Quantity` | Cantidad solicitada. |
| `lineAmount` | `Money` | Valor comercial de la línea. |

**Relaciones.**

- Pertenece a un `Order` (N a 1).
- Referencia un `Product` y una `ProductVariant`.
- Origina movimientos de Reserva y Salida por venta sobre `InventoryItem`.

**Operaciones generadas.**

- `reserveStock(inventoryItem)`.
- `releaseStock(inventoryItem)`.
- `requiresShipment(): boolean` — verdadero solo si el producto es físico.

---

### 4.18 Invoice

**Descripción.** Información comercial asociada a la venta (OBJ-09). Documenta la facturación de la
compra.

**Hereda de.** `DomainEntity`

| Atributo | Tipo | Descripción |
|---|---|---|
| `identifier` | `Identifier` | Identifica de forma única a la factura. |
| `orderId` | `Identifier` | Pedido facturado. |
| `buyerId` | `Identifier` | Comprador a quien se factura. |
| `totalAmount` | `Money` | Valor total facturado. |

**Relaciones.**

- Corresponde a un `Order` (1 a 1).
- Referencia al `Buyer`.
- Puede ser referenciada por un `Refund`.

**Operaciones generadas.**

- `issueFor(order): Invoice` — se emite al validarse el pago.
- `matchesOrderTotal(): boolean` — coherencia entre factura y pedido.

---

### 4.19 Shipment

**Descripción.** Proceso logístico para productos físicos (OBJ-10). Cubre el empaque, despacho y
transporte del pedido.

**Hereda de.** `DomainEntity`

| Atributo | Tipo | Descripción |
|---|---|---|
| `identifier` | `Identifier` | Identifica de forma única al envío. |
| `orderId` | `Identifier` | Pedido despachado. |
| `warehouseId` | `Identifier` | Bodega de origen del despacho. |
| `deliveryAddress` | `Address` | Dirección de entrega del pedido. |

**Relaciones.**

- Corresponde a un `Order` (N a 1).
- Sale de una `Warehouse`.
- Es gestionado por usuarios con rol Operador Logístico.

**Nota.** La especificación no detalla atributos adicionales ni un catálogo de estados propio para
el envío; el avance logístico se refleja en el estado del `Order` (Despachado → Entregado).

**Operaciones generadas.**

- `pack()` / `dispatch()` — empaque y salida física de la bodega.
- `confirmDelivery()` — entrega confirmada, cierra el pedido.

---

### 4.20 ProductReturn

**Descripción.** Devolución de productos por parte del comprador (OBJ-11). Proceso de posventa
incluido en el alcance del sistema.

**Hereda de.** `DomainEntity`

| Atributo | Tipo | Descripción |
|---|---|---|
| `identifier` | `Identifier` | Identifica de forma única la devolución. |
| `orderId` | `Identifier` | Pedido sobre el cual se solicita la devolución. |
| `orderLineId` | `Identifier` | Línea del pedido devuelta. |
| `quantity` | `Quantity` | Cantidad devuelta. |

**Relaciones.**

- Corresponde a un `Order` y a un `OrderLine`.
- Genera un `InventoryMovement` de tipo Devolución.
- Da lugar a un `Refund`.

**Nota.** La especificación no detalla atributos adicionales ni un catálogo de estados propio para
la devolución.

**Operaciones generadas.**

- `registerReturn(orderLine, quantity)`.
- `restock(inventoryItem)` — reintegra existencias mediante un movimiento de Devolución.

---

### 4.21 Refund

**Descripción.** Reembolso asociado a una devolución (OBJ-11). Es responsabilidad compartida entre
el Comprador —que lo solicita— y el Administrador —que lo gestiona—.

**Hereda de.** `DomainEntity`

| Atributo | Tipo | Descripción |
|---|---|---|
| `identifier` | `Identifier` | Identifica de forma única el reembolso. |
| `productReturnId` | `Identifier` | Devolución que origina el reembolso. |
| `invoiceId` | `Identifier` | Factura sobre la cual se aplica el reembolso. |
| `amount` | `Money` | Valor reembolsado. |

**Relaciones.**

- Deriva de un `ProductReturn` (1 a 1).
- Se aplica sobre una `Invoice`.

**Nota.** La especificación no detalla atributos adicionales ni un catálogo de estados propio para
el reembolso.

**Operaciones generadas.**

- `issueFor(productReturn): Refund`.
- `amountWithinInvoiceTotal(): boolean`.

---

### 4.22 Consulta de reportes administrativos

El objetivo OBJ-12 —consolidar información administrativa para consulta— y el proceso "Consulta de
reportes administrativos" **no se modelan como entidad**: no poseen identidad propia ni ciclo de
vida, y no modifican el estado del negocio. Se resuelven como consultas de solo lectura sobre las
entidades existentes, disponibles para los roles Administrador y Supervisor.

---

## 5. Ciclo de vida del dominio

### 5.1 Flujo general del negocio

```
  ┌────────────────┐   Administrador registra al vendedor y su primera bodega
  │  Incorporación │
  └───────┬────────┘
          ▼
  ┌────────────────┐   El vendedor registra productos y define sus características
  │    Catálogo    │
  └───────┬────────┘
          ▼
  ┌────────────────┐   Se registran existencias iniciales en las bodegas asociadas
  │   Inventario   │
  └───────┬────────┘
          ▼
  ┌────────────────┐   Los productos se hacen visibles en el catálogo público
  │   Publicación  │
  └───────┬────────┘
          ▼
  ┌────────────────┐   El comprador selecciona productos mediante el carrito
  │     Compra     │   y confirma el pedido
  └───────┬────────┘
          ▼
  ┌────────────────┐   Se valida el pago y se inicia el flujo de preparación
  │   Transacción  │
  └───────┬────────┘
          ▼
  ┌────────────────┐   Empaque, despacho y transporte del pedido
  │    Logística   │
  └───────┬────────┘
          ▼
  ┌────────────────┐   El pedido se marca como finalizado tras la entrega confirmada
  │     Cierre     │
  └────────────────┘
```

### 5.2 Ciclo de vida del pedido (`Order`)

```
   ┌──────────┐   confirm()      ┌───────────────────┐
   │ CARRITO  │─────────────────▶│ PENDIENTE DE PAGO │
   └──────────┘                  └─────────┬─────────┘
   Selección                               │ confirmPayment()
   provisional                             │ (pago validado)
   de productos                            ▼
                                 ┌───────────────────┐
                                 │      PAGADO       │  Inicio de procesos
                                 └─────────┬─────────┘  de alistamiento
                                           │ dispatch()
                                           │ (salida física de la bodega)
                                           ▼
                                 ┌───────────────────┐
                                 │    DESPACHADO     │
                                 └─────────┬─────────┘
                                           │ completeDelivery()
                                           │ (entrega confirmada)
                                           ▼
                                 ┌───────────────────────┐
                                 │ ENTREGADO/FINALIZADO  │  ── ESTADO TERMINAL ──
                                 └───────────────────────┘     inmodificable
```

El ciclo es **secuencial**: no se admiten saltos entre estados ni retrocesos. Una vez alcanzado
`Entregado/Finalizado`, el pedido no podrá ser modificado bajo ninguna circunstancia.

### 5.3 Ciclo de vida del producto en el catálogo (`Product`)

```
        registerProduct()
                │
                ▼
        ┌───────────────┐
        │   PUBLICADO   │◀────────────┐   Visible en el catálogo público
        └───────┬───────┘             │
                │ suspend()           │ publish()
                ▼                     │
        ┌───────────────┐             │
        │  SUSPENDIDO   │─────────────┘   Temporalmente no comercializable
        └───────┬───────┘
                │ discontinue()
                ▼
        ┌────────────────┐
        │ DESCONTINUADO  │  ── ESTADO TERMINAL ──
        └────────────────┘
```

### 5.4 Ciclo de vida de las existencias (`InventoryItem`)

```
                     ┌─────────────┐
   Ingreso ─────────▶│  DISPONIBLE │◀──────── Devolución
                     └──────┬──────┘
                            │ Reserva
                            ▼
                     ┌─────────────┐
                     │  RESERVADO  │
                     └──────┬──────┘
                            │ Salida por venta
                            ▼
                     ┌─────────────┐
                     │  DESPACHADO │
                     └─────────────┘

   Ajuste ──────────▶ corrige la cantidad en cualquier punto del ciclo,
                      sin permitir jamás un saldo negativo.

   Condición "DAÑADO" ──▶ las existencias marcadas como dañadas
                          quedan excluidas de toda reserva.
```

### 5.5 Ciclo de vida del usuario (`User`)

```
    registro
        │
        ▼
   ┌──────────┐   block()     ┌───────────┐
   │  ACTIVO  │──────────────▶│ BLOQUEADO │
   └──────────┘◀──────────────└───────────┘
                  activate()

   Solo un usuario en estado Activo puede ejecutar operaciones (RG-01).
```

### 5.6 Ciclo de posventa

```
   ┌───────────────────────┐
   │ ENTREGADO/FINALIZADO  │
   └───────────┬───────────┘
               │ el comprador solicita devolución
               ▼
   ┌───────────────────────┐    genera movimiento de Devolución
   │    ProductReturn      │───────────────────────────────▶ InventoryItem
   └───────────┬───────────┘
               │ el administrador gestiona el reembolso
               ▼
   ┌───────────────────────┐    se aplica sobre
   │        Refund         │───────────────────────────────▶ Invoice
   └───────────────────────┘
```

---

## 6. Reglas de diseño del dominio

### 6.1 Identidad y unicidad

- **RD-ID-01.** Toda entidad de dominio hereda de `DomainEntity` y se compara por identidad, nunca
  por el valor de sus atributos.
- **RD-ID-02.** El identificador de usuario es único.
- **RD-ID-03.** El correo electrónico del usuario es único en toda la plataforma.
- **RD-ID-04.** El documento de identidad del usuario es único en toda la plataforma.
- **RD-ID-05.** El par (`productId`, `warehouseId`) identifica de forma única un `InventoryItem`:
  no pueden existir dos registros de existencias para el mismo producto en la misma bodega.

### 6.2 Roles y autorización

- **RD-ROL-01 (RG-01).** Toda operación debe ejecutarse por un usuario autenticado. Ninguna
  entidad admite modificaciones anónimas.
- **RD-ROL-02 (RG-02).** Cada usuario tiene un único rol dentro del sistema. El atributo `role` es
  obligatorio y no admite múltiples valores.
- **RD-ROL-03 (RG-03).** Ningún participante puede administrar información fuera de su rol. Las
  entidades exponen operaciones de verificación de pertenencia (`belongsTo`, `ownsProduct`,
  `ownsWarehouse`) para hacer cumplible esta regla desde el propio dominio.
- **RD-ROL-04.** El comprador nunca administra información de otros compradores ni inventarios.
- **RD-ROL-05.** Los vendedores no pueden auto-registrarse: la creación de un `Seller` es una
  operación exclusiva del rol Administrador, y se realiza junto con su primera bodega.
- **RD-ROL-06.** La matriz de responsabilidades define quién puede ejecutar cada proceso:

| Proceso | Comprador | Vendedor | Op. Logístico | Administrador |
|---|:---:|:---:|:---:|:---:|
| Registro de vendedores | | | | ✔ |
| Registro de productos | | ✔ | | |
| Administración de inventario | | ✔ | ✔ | |
| Gestión de pedidos | ✔ | ✔ | ✔ | |
| Gestión de reembolsos | ✔ | | | ✔ |

  El rol Supervisor es un perfil de consulta y seguimiento operativo: no ejecuta procesos de
  modificación.

### 6.3 Catálogo

- **RD-CAT-01.** Todo producto pertenece a un único vendedor y solo ese vendedor lo administra.
- **RD-CAT-02.** El tipo de producto (Físico o Digital) determina el comportamiento del dominio:
  los físicos requieren inventario y despacho; los digitales se entregan de inmediato tras el pago.
  El tipo no cambia durante la vida del producto.
- **RD-CAT-03.** Solo los productos en estado Publicado son visibles en el catálogo público.
- **RD-CAT-04.** Descontinuado es un estado terminal: no admite retorno a Publicado.
- **RD-CAT-05.** Las variantes son la unidad de selección comercial; un producto sin variantes no
  puede comercializarse.

### 6.4 Inventario

- **RD-INV-01.** El inventario es distribuido: toda existencia está vinculada obligatoriamente a un
  producto y a una bodega específica. No existe inventario "global" ni sin bodega.
- **RD-INV-02.** No se permitirán existencias negativas bajo ninguna circunstancia. Cualquier
  operación que produzca un saldo negativo debe rechazarse.
- **RD-INV-03.** No se puede reservar inventario inexistente ni marcado como "Dañado".
- **RD-INV-04.** Todo cambio de existencias se materializa como un `InventoryMovement` de uno de los
  cinco tipos definidos: Ingreso, Reserva, Salida por venta, Ajuste o Devolución. No existen
  modificaciones directas de cantidad sin movimiento asociado.
- **RD-INV-05.** Los productos digitales no generan existencias ni movimientos de inventario.

### 6.5 Pedidos

- **RD-PED-01.** El ciclo de estados del pedido es secuencial y no admite saltos ni retrocesos:
  Carrito → Pendiente de Pago → Pagado → Despachado → Entregado/Finalizado.
- **RD-PED-02.** Un pedido finalizado no podrá ser modificado bajo ninguna circunstancia. Toda
  operación de modificación debe verificar `isModifiable()` antes de aplicarse.
- **RD-PED-03.** La transición a Pagado requiere la validación previa del pago e inicia los
  procesos de alistamiento.
- **RD-PED-04.** El estado Carrito representa una selección provisional: no compromete
  definitivamente el inventario ni genera facturación.
- **RD-PED-05.** Un pedido pertenece a un único comprador, y solo ese comprador puede consultarlo o
  actuar sobre él dentro de su rol.

### 6.6 Facturación, logística y posventa

- **RD-FAC-01.** La facturación se genera a partir del pedido pagado y refleja su valor comercial.
- **RD-LOG-01.** Solo los pedidos que contienen productos físicos generan envíos; los productos
  digitales se entregan de forma inmediata tras el pago.
- **RD-LOG-02.** Todo envío parte de una bodega concreta, que es la que soporta la salida de
  existencias.
- **RD-POS-01.** Toda devolución se origina en una línea de un pedido existente y reintegra
  existencias mediante un movimiento de tipo Devolución.
- **RD-POS-02.** Todo reembolso deriva de una devolución y se aplica sobre la factura del pedido
  correspondiente.

### 6.7 Alcance del modelo

- **RD-ALC-01.** El modelo no contempla interfaces gráficas, aplicaciones móviles, portales web ni
  mecanismos de autenticación técnica: son explícitamente ajenos al dominio.
- **RD-ALC-02.** El modelo no incorpora decisiones de tecnología, arquitectura de software ni
  almacenamiento de la información.
- **RD-ALC-03.** El sistema administrará exclusivamente los procesos descritos en la especificación
  funcional. Cualquier concepto ausente de la especificación queda fuera del modelo de dominio.
