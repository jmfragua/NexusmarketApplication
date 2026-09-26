# Servicios de Pedido

**Paquete:** `application.domain.services.order`

## 1. Introducción

Este documento define los servicios del subdominio de **Pedido** (Dominio 7 de la especificación
funcional). El pedido representa el compromiso comercial formal entre comprador y plataforma, y su
ciclo de vida es **el proceso central del sistema**.

Dos reglas gobiernan todo el subdominio y ningún servicio puede eludirlas:

- El ciclo es **secuencial**: no se admiten saltos entre estados ni retrocesos.
- Un pedido finalizado **no podrá ser modificado bajo ninguna circunstancia**.

---

## 2. Contexto del modelo de dominio

```text
DomainEntity<OrderId>
      │
      └── Order
             │
             ├── buyerId         : BuyerId
             ├── status          : OrderStatus
             ├── deliveryAddress : Address
             ├── lines           : List<OrderLine>
             └── totalAmount     : Money

DomainEntity<OrderLineId>
      │
      └── OrderLine
             │
             ├── orderId          : OrderId
             ├── productId        : ProductId
             ├── productVariantId : ProductVariantId
             ├── quantity         : Quantity
             └── lineAmount       : Money
```

El `Order` se construye a partir de un carrito confirmado, por lo que **nace en
`PENDING_PAYMENT`**: el estado `CART` del ciclo es el que sostiene la entidad `Cart`, no el pedido
(RD-PED-04).

`totalAmount` se calcula sumando los importes de las líneas en el constructor y es inmutable.

---

## 3. Objetos de valor utilizados

```text
Order
 ├── status          : OrderStatus    ── CART | PENDING_PAYMENT | PAID | DISPATCHED | DELIVERED
 ├── deliveryAddress : Address        ── no vacía
 └── totalAmount     : Money          ── no negativo, con moneda

OrderLine
 ├── quantity   : Quantity            ── mayor que cero
 └── lineAmount : Money

User (actor)
 ├── status : UserStatus
 └── role   : UserRole
```

### 3.1 Validación de `OrderStatus`

Los servicios **nunca** comparan el estado con cadenas ni con ordinales:

```java
// Incorrecto
order.getStatus().equals("PAID");
order.getStatus().ordinal() == 2;

// Correcto
order.getStatus().canTransitionTo(OrderStatus.PAID);
order.isModifiable();
```

`OrderStatus` expone el comportamiento que el dominio necesita: `canTransitionTo`, `isTerminal`,
`isModifiable`.

### 3.2 Validación de `Money`

`Money` sólo permite sumar y comparar importes de la **misma moneda**. El total del pedido se
obtiene reduciendo los importes de las líneas con `Money::add`, de modo que un pedido con líneas en
monedas distintas es rechazado por el propio objeto de valor.

---

## 4. Ciclo de vida del pedido

```text
   ┌──────────┐   Cart.confirm()   ┌───────────────────┐
   │  CARRITO │───────────────────▶│ PENDIENTE DE PAGO │
   └──────────┘                    └─────────┬─────────┘
   (entidad Cart)                            │ confirmPayment()
                                             ▼
                                   ┌───────────────────┐
                                   │      PAGADO       │  Inicia alistamiento
                                   └─────────┬─────────┘
                                             │ dispatch()
                                             ▼
                                   ┌───────────────────┐
                                   │    DESPACHADO     │
                                   └─────────┬─────────┘
                                             │ completeDelivery()
                                             ▼
                                   ┌───────────────────────┐
                                   │ ENTREGADO/FINALIZADO  │ ── TERMINAL ──
                                   └───────────────────────┘    inmodificable
```

### 4.1 Reglas de transición

| Regla | Enunciado |
|---|---|
| **RD-PED-01** | El ciclo es secuencial. `OrderStatus.canTransitionTo` sólo acepta el estado inmediatamente siguiente. |
| **RD-PED-02** | `DELIVERED` es terminal e inmodificable. |
| **RD-PED-03** | La transición a `PAID` requiere validación previa del pago e inicia el alistamiento. |
| **RD-PED-05** | Un pedido pertenece a un único comprador. |

La implementación de `canTransitionTo` es deliberadamente estricta:

```java
return target != null && target.ordinal() == this.ordinal() + 1;
```

Esto hace imposible por construcción saltar de `PENDING_PAYMENT` a `DISPATCHED`, o retroceder de
`PAID` a `PENDING_PAYMENT`.

---

## 5. Patrón estándar de transición

Los tres servicios que mueven el pedido siguen exactamente esta secuencia:

1. **Validar el rol del actor** (y su estado activo, RG-01).
2. **Validar la pertenencia** cuando el actor es el comprador (RD-PED-05).
3. **Verificar `order.isModifiable()`** → si es falso, `OrderNotModifiableException` (RD-PED-02).
4. **Verificar `order.getStatus().canTransitionTo(destino)`** → si es falso,
   `InvalidOrderTransitionException` (RD-PED-01).
5. **Invocar la transición del modelo** y persistir.

Los pasos 3 y 4 son la aplicación de RD-SRV-06: el modelo ya protege la regla lanzando
`IllegalStateException`, pero el servicio la verifica antes para lanzar una excepción de dominio
específica y trazable.

---

# 1. Confirm Payment

**Clase:** `ConfirmPaymentService`

## Descripción

Valida el pago del pedido e inicia los procesos de alistamiento. Es la transición que convierte una
intención de compra en un compromiso firme.

## Entrada

```java
Order confirmPayment(Buyer buyer, Order order)
```

| Parámetro | Tipo | Descripción |
|---|---|---|
| `buyer` | `Buyer` | Comprador propietario del pedido, autenticado |
| `order` | `Order` | Pedido en estado `PENDING_PAYMENT` |

## Dependencias

`OrderRepositoryPort`, `ValidateRoleAuthorizationService`, `ValidateBuyerOwnershipService`

## Validación del usuario

- Activo (RG-01).
- Rol `BUYER`.

## Validación de pertenencia

```java
validateBuyerOwnershipService.validateOrder(buyer, order);
```

Delega en `Order.belongsTo(buyer)`. Un comprador nunca confirma el pago de un pedido ajeno
(RD-PED-05, RD-ROL-04).

## Validación de estado

| Verificación | Excepción |
|---|---|
| `order.isModifiable()` | `OrderNotModifiableException` |
| `status.canTransitionTo(PAID)` | `InvalidOrderTransitionException` |

Sólo un pedido en `PENDING_PAYMENT` puede pasar a `PAID`.

## Transición

```text
PENDING_PAYMENT ──▶ PAID
```

## Efectos posteriores en el negocio

A partir de `PAID` se habilitan procesos de otros subdominios:

- **Facturación**: `IssueInvoiceService` puede emitir la factura (RD-FAC-01).
- **Productos digitales**: `DigitalProduct.deliverOnPayment` permite la entrega inmediata
  (RD-LOG-01).
- **Logística**: si el pedido contiene productos físicos, `PackShipmentService` puede empacar
  (RD-PED-03).

Este servicio **no** dispara esos procesos: los habilita. La orquestación entre subdominios
corresponde a la capa de casos de uso.

## Persistencia

`OrderRepositoryPort.save(order)`.

## Excepciones

| Excepción | Causa |
|---|---|
| `UserNotActiveException` | El comprador está bloqueado |
| `UnauthorizedOperationException` | El rol no es `BUYER`, o el pedido es de otro comprador |
| `OrderNotModifiableException` | El pedido ya fue entregado (RD-PED-02) |
| `InvalidOrderTransitionException` | El pedido no está en `PENDING_PAYMENT` |

---

# 2. Dispatch Order

**Clase:** `DispatchOrderService`

## Descripción

Registra la salida física del pedido desde la bodega.

## Entrada

```java
Order dispatch(User<?> actor, Order order)
```

## Dependencias

`OrderRepositoryPort`, `ValidateRoleAuthorizationService`

## Validación del usuario

Rol `LOGISTICS_OPERATOR`, activo. La operación física de bodegas y despachos es responsabilidad
exclusiva del operador logístico (RD-ROL-06).

No hay validación de pertenencia: el operador logístico opera sobre cualquier pedido de la
plataforma dentro de su rol.

## Validación de estado

| Verificación | Excepción |
|---|---|
| `order.isModifiable()` | `OrderNotModifiableException` |
| `status.canTransitionTo(DISPATCHED)` | `InvalidOrderTransitionException` |

Sólo un pedido `PAID` puede despacharse: no se despacha lo que no se ha pagado.

## Transición

```text
PAID ──▶ DISPATCHED
```

## Relación con el inventario

El despacho del pedido y la salida de existencias son operaciones distintas y deliberadamente
separadas:

- `DispatchOrderService` mueve el **estado del pedido**.
- `IssueForSaleService` (subdominio de inventario) genera el movimiento `SALE_OUTBOUND` que reduce
  el saldo de la bodega.

Cuando existe un `Shipment`, se usa `DispatchShipmentService`, que actualiza envío y pedido a la
vez.

## Excepciones

| Excepción | Causa |
|---|---|
| `UnauthorizedOperationException` | El rol no es `LOGISTICS_OPERATOR` |
| `OrderNotModifiableException` | El pedido ya fue entregado |
| `InvalidOrderTransitionException` | El pedido no está en `PAID` |

---

# 3. Complete Delivery

**Clase:** `CompleteDeliveryService`

## Descripción

Cierra el pedido una vez confirmada la entrega. Es la transición terminal del ciclo.

## Entrada

```java
Order completeDelivery(User<?> actor, Order order)
```

## Validación del usuario

Rol `LOGISTICS_OPERATOR`, activo.

## Validación de estado

| Verificación | Excepción |
|---|---|
| `order.isModifiable()` | `OrderNotModifiableException` |
| `status.canTransitionTo(DELIVERED)` | `InvalidOrderTransitionException` |

Sólo un pedido `DISPATCHED` puede marcarse como entregado.

## Transición

```text
DISPATCHED ──▶ DELIVERED    (terminal)
```

## Consecuencias del estado terminal

Tras esta operación:

- `order.isModifiable()` devuelve `false` de forma permanente. Cualquier intento posterior de
  cambiar dirección de entrega o estado será rechazado (RD-PED-02).
- `OrderStatus.DELIVERED.isTerminal()` devuelve `true`: no existe transición de salida (RD-VO-13).
- **Se habilita el ciclo de posventa**: `RequestReturnService` exige precisamente que el pedido esté
  en `DELIVERED` (RD-POS-01).

La inmodificabilidad y la apertura de la posventa son las dos caras de la misma transición.

## Excepciones

| Excepción | Causa |
|---|---|
| `UnauthorizedOperationException` | El rol no es `LOGISTICS_OPERATOR` |
| `OrderNotModifiableException` | El pedido ya estaba entregado |
| `InvalidOrderTransitionException` | El pedido no está en `DISPATCHED` |

---

# 4. Consult Order

**Clase:** `ConsultOrderService`

## Descripción

Consulta pedidos delimitando estrictamente el alcance de cada rol. Es el servicio donde RD-ROL-04
("el comprador nunca administrará información de otros compradores") se hace efectivo en la lectura.

## Entrada

```java
Order       consultOwn(Buyer buyer, OrderId orderId);
List<Order> consultOwn(Buyer buyer);
List<Order> consultByStatus(User<?> actor, OrderStatus status);
```

## Dependencias

`OrderRepositoryPort`, `ValidateRoleAuthorizationService`, `ValidateBuyerOwnershipService`

## Autorización por operación

| Operación | Roles | Alcance |
|---|---|---|
| `consultOwn(Buyer, OrderId)` | `BUYER` | Sólo pedidos propios |
| `consultOwn(Buyer)` | `BUYER` | Sólo pedidos propios |
| `consultByStatus` | `SELLER`, `LOGISTICS_OPERATOR`, `ADMINISTRATOR`, `SUPERVISOR` | Todos los pedidos en ese estado |

## Procesamiento de `consultOwn(Buyer, OrderId)`

1. Valida rol `BUYER` y estado activo.
2. Recupera el pedido; si no existe → `EntityNotFoundException`.
3. **Valida la pertenencia** con `ValidateBuyerOwnershipService.validateOrder`.

El orden importa: se recupera primero y se valida pertenencia después, de modo que un comprador que
solicite el pedido de otro reciba `UnauthorizedOperationException` y no una pista sobre la
existencia del pedido ajeno.

## Excepciones

| Excepción | Causa |
|---|---|
| `EntityNotFoundException` | No existe pedido con ese identificador |
| `UnauthorizedOperationException` | El pedido pertenece a otro comprador, o el rol no consulta pedidos |

---

## 6. Trazabilidad servicio ↔ código

| Servicio | Clase Java | Transición | Rol |
|---|---|---|---|
| Confirm Payment | `ConfirmPaymentService` | `PENDING_PAYMENT → PAID` | `BUYER` |
| Dispatch Order | `DispatchOrderService` | `PAID → DISPATCHED` | `LOGISTICS_OPERATOR` |
| Complete Delivery | `CompleteDeliveryService` | `DISPATCHED → DELIVERED` | `LOGISTICS_OPERATOR` |
| Consult Order | `ConsultOrderService` | — (solo lectura) | Todos según alcance |

La transición inicial `CART → PENDING_PAYMENT` no vive en este subdominio: la ejecuta
`ConfirmCartService`, documentado en [cart-services.md](cart-services.md).
