# Servicios de Carrito

**Paquete:** `application.domain.services.cart`

## 1. Introducción

Este documento define los servicios del subdominio de **Carrito**, que cubre la primera parte del
Dominio 7: la selección provisional de productos realizada por el comprador y su confirmación como
pedido.

La regla que define todo el subdominio es que **el carrito no compromete nada**: es una selección
provisional que no reserva inventario ni genera facturación (RD-PED-04). Sólo al confirmarse nace un
compromiso comercial.

---

## 2. Contexto del modelo de dominio

```text
DomainEntity<CartId>
      │
      └── Cart
             │
             ├── buyerId : BuyerId
             └── items   : List<CartItem>

DomainEntity<CartItemId>
      │
      └── CartItem
             │
             ├── productId        : ProductId
             ├── productVariantId : ProductVariantId
             └── quantity         : Quantity
```

El carrito pertenece a un único comprador (relación 1 a 1 con el carrito activo) y contiene una o
varias líneas de selección.

### 2.1 Ausencia deliberada de referencias a inventario

`Cart` y `CartItem` **no tienen ninguna referencia** a `InventoryItem`, `Warehouse` ni
`InventoryMovement`. Esa ausencia es intencional y estructural: hace imposible que un servicio de
este subdominio comprometa existencias, por descuido o por evolución futura del código.

Ningún servicio de `application.domain.services.cart` inyecta
`InventoryItemRepositoryPort` ni `InventoryMovementRepositoryPort`.

---

## 3. Objetos de valor utilizados

```text
CartItem
 ├── quantity         : Quantity          ── mayor que cero
 ├── productId        : ProductId
 └── productVariantId : ProductVariantId

Cart → Order (al confirmar)
 ├── deliveryAddress : Address            ── debe ser una de las registradas del comprador
 └── lineAmount      : Money              ── aportado por el invocador

Buyer (actor)
 ├── status           : UserStatus              ── ACTIVE para operar
 ├── commercialStatus : BuyerCommercialStatus   ── ENABLED para comprar
 └── role             : UserRole                ── BUYER
```

### 3.1 Validación de `Quantity` en una línea de carrito

`CartItem` rechaza cantidades de cero unidades tanto en construcción como en `changeQuantity`:

```java
if (!quantity.isPositive()) {
    throw new IllegalArgumentException("a cart line cannot be of zero units");
}
```

Retirar una línea se hace con `removeItem`, nunca poniendo su cantidad en cero.

### 3.2 Validación de `BuyerCommercialStatus`

El estado comercial es independiente del estado de usuario. Un comprador puede estar `ACTIVE` en la
plataforma pero `DISABLED` para comprar. `Buyer.canPurchase()` exige **ambos**:

```java
return isActive() && commercialStatus.allowsPurchase();
```

---

## 4. Patrón estándar del subdominio

Los cinco servicios siguen la misma secuencia:

1. **Validar el rol** `BUYER` y el estado activo del actor (RG-01).
2. **Validar la pertenencia del carrito** con `ValidateBuyerOwnershipService.validateCart`, que
   delega en `Cart.belongsTo(buyer)` (RD-ROL-04).
3. Invocar el comportamiento del modelo.
4. Persistir el carrito con `CartRepositoryPort`.

**Rol autorizado:** únicamente `BUYER`. Ningún otro rol manipula el carrito de nadie.

---

# 1. Add Cart Item

**Clase:** `AddCartItemService`

## Descripción

Selecciona una variante de producto dentro del carrito del comprador.

## Entrada

```java
CartItem addItem(Buyer buyer,
                 Cart cart,
                 Product product,
                 ProductVariantId variantId,
                 Quantity quantity,
                 CartItemId cartItemId)
```

| Parámetro | Tipo | Descripción |
|---|---|---|
| `product` | `Product` | Producto completo, no su identificador: el servicio necesita comprobar su visibilidad |
| `variantId` | `ProductVariantId` | Variante concreta seleccionada |
| `cartItemId` | `CartItemId` | Identificador de la nueva línea, si procede crearla |

**Por qué recibe `Product` y no `ProductId`.** La validación de disponibilidad exige consultar el
estado y las variantes del producto. Recibir el modelo completo evita que el servicio tenga que
resolver el producto por su cuenta y mantiene la firma dentro de modelos de dominio (RD-PIN-02).

## Validación del usuario

- Activo (RG-01), rol `BUYER`.
- El carrito debe pertenecer al comprador.

## Validación de disponibilidad del producto

```java
if (!product.isVisibleInCatalog()) {
    throw new ProductNotAvailableException(product.getIdentifier());
}
```

`isVisibleInCatalog()` exige **dos** condiciones simultáneas:

```text
status == PUBLISHED        (RD-CAT-03)
&& !variants.isEmpty()     (RD-CAT-05)
```

Un producto suspendido, descontinuado o sin variantes no puede seleccionarse. La segunda condición
implementa que las variantes son la unidad de selección comercial: un producto sin variantes no
puede comercializarse.

## Procesamiento

`Cart.addItem` aplica una regla de acumulación:

1. Busca una línea existente que seleccione el mismo par (producto, variante).
2. **Si existe**: acumula la cantidad sobre esa línea (`quantity.add(nueva)`) y devuelve la línea
   existente. El `cartItemId` recibido no se usa.
3. **Si no existe**: crea una línea nueva con el `cartItemId` recibido.

Esto evita líneas duplicadas de la misma variante dentro de un mismo carrito.

## Estado resultante

```text
items : línea acumulada, o una línea nueva
```

Ninguna existencia se reserva (RD-PED-04).

## Excepciones

| Excepción | Causa |
|---|---|
| `ProductNotAvailableException` | El producto no está publicado o no tiene variantes |
| `UnauthorizedOperationException` | El carrito es de otro comprador, o el rol no es `BUYER` |
| `UserNotActiveException` | El comprador está bloqueado |
| `IllegalArgumentException` | Cantidad de cero unidades |

---

# 2. Remove Cart Item

**Clase:** `RemoveCartItemService`

## Descripción

Retira una línea de la selección provisional.

## Entrada

```java
Cart removeItem(Buyer buyer, Cart cart, CartItem item)
```

## Validación del usuario

Rol `BUYER`, activo, propietario del carrito.

## Procesamiento

`Cart.removeItem(item)` elimina la línea de la colección. La operación es idempotente: retirar una
línea que ya no está no produce error.

## Nota sobre el inventario

Retirar una línea **no libera nada**, porque el carrito nunca había comprometido existencias
(RD-PED-04). La liberación de reservas es una operación del subdominio de inventario
(`ReleaseReservationService`) y actúa sobre líneas de *pedido*, no de carrito.

## Excepciones

| Excepción | Causa |
|---|---|
| `UnauthorizedOperationException` | El carrito es de otro comprador |
| `UserNotActiveException` | El comprador está bloqueado |

---

# 3. Update Cart Item Quantity

**Clase:** `UpdateCartItemQuantityService`

## Descripción

Cambia las unidades seleccionadas en una línea del carrito.

## Entrada

```java
Cart updateQuantity(Buyer buyer, Cart cart, CartItem item, Quantity quantity)
```

## Validación del usuario

Rol `BUYER`, activo, propietario del carrito.

## Validación de pertenencia de la línea

`Cart.updateQuantity` verifica que la línea pertenezca a **este** carrito antes de modificarla:

```java
if (item == null || !items.contains(item)) {
    throw new IllegalArgumentException("the line does not belong to this cart");
}
```

## Validación de la cantidad

`CartItem.changeQuantity` exige cantidad positiva. Para dejar una línea en cero se usa
`RemoveCartItemService`, no este servicio.

## Excepciones

| Excepción | Causa |
|---|---|
| `IllegalArgumentException` | La línea no pertenece a este carrito, o la cantidad es cero |
| `UnauthorizedOperationException` | El carrito es de otro comprador |

---

# 4. Clear Cart

**Clase:** `ClearCartService`

## Descripción

Vacía por completo la selección provisional del comprador.

## Entrada

```java
Cart clear(Buyer buyer, Cart cart)
```

## Validación del usuario

Rol `BUYER`, activo, propietario del carrito.

## Procesamiento

`Cart.clear()` vacía la colección de líneas. El carrito sigue existiendo y asociado al comprador:
se vacía, no se elimina.

## Relación con la confirmación

`Cart.confirm` invoca internamente `clear()` tras generar el pedido, porque la selección ya quedó
comprometida. Por eso `ConfirmCartService` persiste el carrito además del pedido.

## Excepciones

| Excepción | Causa |
|---|---|
| `UnauthorizedOperationException` | El carrito es de otro comprador |

---

# 5. Confirm Cart

**Clase:** `ConfirmCartService`

## Descripción

Confirma la selección provisional como pedido. Es la **primera transición del ciclo secuencial del
pedido** (RD-PED-01) y el punto donde una intención se convierte en compromiso comercial.

## Entrada

```java
Order confirm(Buyer buyer,
              Cart cart,
              OrderId orderId,
              Address deliveryAddress,
              List<OrderLine> lines)
```

| Parámetro | Descripción |
|---|---|
| `orderId` | Identificador asignado al nuevo pedido |
| `deliveryAddress` | Dirección de entrega, elegida entre las registradas del comprador |
| `lines` | Líneas valoradas, exactamente una por línea del carrito |

## Dependencias

`CartRepositoryPort`, `OrderRepositoryPort`, `ValidateRoleAuthorizationService`,
`ValidateBuyerOwnershipService`

## Decisión de modelado: por qué el invocador aporta `lines`

La especificación funcional **no modela precio sobre el producto**: sólo define el importe de la
línea de pedido (`lineAmount`) y el total del pedido. El dominio no puede calcular un precio que no
existe en el modelo, y no inventa conceptos ausentes de la especificación (RD-ALC-03).

Por eso las líneas valoradas las construye quien invoca el caso de uso, con
`CartItem.toOrderLine(lineId, orderId, lineAmount)`, y el dominio se limita a verificar que
correspondan exactamente a la selección.

## Validación del usuario

- Activo (RG-01), rol `BUYER`.
- Propietario del carrito.

## Validación del estado comercial

```java
if (!buyer.canPurchase()) {
    throw new BuyerNotAllowedToPurchaseException(buyer.getIdentifier());
}
```

Exige `UserStatus.ACTIVE` **y** `BuyerCommercialStatus.ENABLED`.

## Validación del carrito

```java
if (cart.isEmpty()) {
    throw new EmptyCartException(cart.getIdentifier());
}
```

Un pedido contiene al menos una línea; un carrito vacío no puede confirmarse.

## Validación de la dirección de entrega

`Buyer.placeOrder` verifica que la dirección sea una de las registradas del comprador:

```java
if (!knowsAddress(deliveryAddress)) {
    throw new IllegalArgumentException("the delivery address is not registered for this buyer");
}
```

Acepta la dirección principal o cualquiera de las adicionales. No se admite una dirección arbitraria
no registrada.

## Validación de correspondencia carrito ↔ líneas

`Cart.confirm` exige correspondencia **uno a uno**:

1. `lines.size() == items.size()`, o se rechaza.
2. Para cada línea del carrito debe existir una línea de pedido que coincida en producto, variante
   **y** cantidad.

Esto impide que la valoración altere silenciosamente la selección del comprador.

## Procesamiento

1. Se construye el `Order`, que nace en `PENDING_PAYMENT` y calcula su `totalAmount` sumando los
   importes de las líneas.
2. Se vacía el carrito (`clear()`), porque su selección ya quedó comprometida.

## Estado resultante

```text
Order.status : PENDING_PAYMENT
Order.total  : suma de los importes de las líneas
Cart.items   : vacío
```

## Persistencia

Se persisten **ambos**: el carrito vacío y el pedido nuevo.

## Excepciones

| Excepción | Causa |
|---|---|
| `BuyerNotAllowedToPurchaseException` | El comprador está bloqueado o comercialmente inhabilitado |
| `EmptyCartException` | El carrito no tiene líneas |
| `IllegalArgumentException` | La dirección no está registrada, o las líneas no corresponden a la selección |
| `UnauthorizedOperationException` | El carrito es de otro comprador |

---

## 5. Trazabilidad servicio ↔ código

| Servicio | Clase Java | Efecto |
|---|---|---|
| Add Cart Item | `AddCartItemService` | Añade o acumula línea |
| Remove Cart Item | `RemoveCartItemService` | Retira línea |
| Update Cart Item Quantity | `UpdateCartItemQuantityService` | Cambia unidades |
| Clear Cart | `ClearCartService` | Vacía la selección |
| Confirm Cart | `ConfirmCartService` | Genera el `Order` en `PENDING_PAYMENT` |

El resto del ciclo del pedido se documenta en [order-services.md](order-services.md).
