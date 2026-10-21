# Servicios de Logística

**Paquete:** `application.domain.services.shipment`

## 1. Introducción

Este documento define los servicios del subdominio de **Logística** (OBJ-10): el empaque, despacho y
transporte de los pedidos que contienen productos físicos.

---

## 2. Contexto del modelo de dominio

```text
DomainEntity<ShipmentId>
      │
      └── Shipment
             │
             ├── orderId         : OrderId
             ├── warehouseId     : WarehouseId
             └── deliveryAddress : Address
```

### 2.1 Por qué `Shipment` no tiene estado propio

La especificación funcional **no declara un catálogo de estados para el envío**. El avance logístico
se refleja en el estado del `Order` (`DISPATCHED`, `DELIVERED`). Siguiendo RD-ALC-03 —ningún
concepto ausente de la especificación entra al modelo— no se inventa un `ShipmentStatus`.

La consecuencia práctica es que `DispatchShipmentService` y `ConfirmShipmentDeliveryService`
persisten **el pedido y el envío**, porque el cambio de estado ocurre en el primero.

---

## 3. Invariantes del subdominio

| Código | Invariante |
|---|---|
| **RD-LOG-01** | Sólo los pedidos con productos físicos generan envíos. Los digitales se entregan de inmediato tras el pago. |
| **RD-LOG-02** | Todo envío parte de una bodega concreta, la que soporta la salida de existencias. |
| **RD-PED-03** | El empaque requiere un pago validado. |
| **RD-PED-02** | Un pedido entregado es inmodificable. |

**Rol autorizado en toda el área:** `LOGISTICS_OPERATOR` (RD-ROL-06).

---

# 1. Pack Shipment

**Clase:** `PackShipmentService`

## Descripción

Prepara el empaque del pedido, primer paso del proceso logístico.

## Entrada

```java
Shipment pack(User<?> actor, Shipment shipment, Order order, Collection<Product> products)
```

| Parámetro | Descripción |
|---|---|
| `products` | Productos referenciados por las líneas del pedido |

### Por qué recibe `products`

`OrderLine` guarda el `productId` pero **no el tipo de producto**. Para decidir si el pedido
requiere logística hay que consultar los productos referenciados. El servicio los recibe en lugar de
resolverlos por su cuenta, manteniendo el dominio sin dependencias de consulta (RD-ALC-03).

## Dependencias

`ShipmentRepositoryPort`, `ValidateRoleAuthorizationService`

## Validaciones

1. **Usuario**: activo (RG-01) y rol `LOGISTICS_OPERATOR`.
2. **Productos físicos**: `order.containsPhysicalProducts(products)` debe ser verdadero; si no →
   `ShipmentNotAllowedException` (RD-LOG-01).
3. **Estado del pedido**: `Shipment.pack` exige `OrderStatus.PAID`; sólo un pedido pagado puede
   empacarse (RD-PED-03).
4. **Correspondencia**: `Shipment.pack` verifica que el pedido sea el de este envío.

## Procesamiento

`shipment.pack(order)` valida las precondiciones. No cambia el estado del pedido: el empaque es
preparación previa al despacho.

## Excepciones

| Excepción | Causa |
|---|---|
| `ShipmentNotAllowedException` | El pedido no contiene productos físicos |
| `IllegalStateException` | El pedido no está en `PAID` |
| `IllegalArgumentException` | El pedido no corresponde a este envío |
| `UnauthorizedOperationException` | El rol no es `LOGISTICS_OPERATOR` |
| `DuplicateIdentifierException` | El identificador es el del envío de **otro** pedido (`Shipment.isFor`). Reempacar el envío del mismo pedido lo reutiliza (RD-ID-06) |

---

# 2. Dispatch Shipment

**Clase:** `DispatchShipmentService`

## Descripción

Registra la salida física del envío desde la bodega, lo que mueve el pedido a `DISPATCHED`.

## Entrada

```java
Shipment dispatch(User<?> actor, Shipment shipment, Order order)
```

## Dependencias

`ShipmentRepositoryPort`, `OrderRepositoryPort`, `ValidateRoleAuthorizationService`

## Validaciones

1. **Usuario**: activo, rol `LOGISTICS_OPERATOR`.
2. **Modificabilidad**: `order.isModifiable()` → si es falso, `OrderNotModifiableException`
   (RD-PED-02).
3. **Transición**: `status.canTransitionTo(DISPATCHED)` → si es falso,
   `InvalidOrderTransitionException` (RD-PED-01). Sólo un pedido `PAID` puede despacharse.
4. **Correspondencia**: `Shipment.dispatch` verifica que el pedido sea el de este envío.

## Procesamiento

`shipment.dispatch(order)` delega en `order.dispatch()`.

## Transición

```text
PAID ──▶ DISPATCHED
```

## Persistencia

Se persisten **pedido y envío**, porque la operación cambia el estado del primero.

## Relación con el inventario

Esta operación **no descuenta existencias**. La salida física del saldo la ejecuta
`IssueForSaleService` con un movimiento `SALE_OUTBOUND` (RD-LOG-02). Son operaciones separadas y la
capa de casos de uso las coordina.

## Excepciones

| Excepción | Causa |
|---|---|
| `OrderNotModifiableException` | El pedido ya fue entregado |
| `InvalidOrderTransitionException` | El pedido no está en `PAID` |
| `UnauthorizedOperationException` | El rol no es `LOGISTICS_OPERATOR` |

---

# 3. Confirm Shipment Delivery

**Clase:** `ConfirmShipmentDeliveryService`

## Descripción

Confirma la entrega del envío, lo que cierra el pedido de forma definitiva.

## Entrada

```java
Shipment confirmDelivery(User<?> actor, Shipment shipment, Order order)
```

## Validaciones

1. **Usuario**: activo, rol `LOGISTICS_OPERATOR`.
2. **Modificabilidad**: `order.isModifiable()` → `OrderNotModifiableException`.
3. **Transición**: `status.canTransitionTo(DELIVERED)` → `InvalidOrderTransitionException`. Sólo un
   pedido `DISPATCHED` puede entregarse.
4. **Correspondencia** pedido ↔ envío.

## Transición

```text
DISPATCHED ──▶ DELIVERED    (terminal)
```

## Consecuencias

- El pedido queda **inmodificable bajo cualquier circunstancia** (RD-PED-02).
- Se **habilita el ciclo de posventa**: `RequestReturnService` exige precisamente el estado
  `DELIVERED` (RD-POS-01).

## Persistencia

Pedido y envío.

## Excepciones

| Excepción | Causa |
|---|---|
| `OrderNotModifiableException` | El pedido ya estaba entregado |
| `InvalidOrderTransitionException` | El pedido no está en `DISPATCHED` |
| `UnauthorizedOperationException` | El rol no es `LOGISTICS_OPERATOR` |

---

# 4. Consult Shipment

**Clase:** `ConsultShipmentService`

## Descripción

Consulta los envíos de un pedido. Es de solo lectura.

## Entrada

```java
List<Shipment> consultByOrder(User<?> actor, Order order);
List<Shipment> consultOwn(Buyer buyer, Order order);
```

## Dependencias

`ShipmentRepositoryPort`, `ValidateRoleAuthorizationService`, `ValidateBuyerOwnershipService`

## Validaciones

- `consultByOrder`: rol `LOGISTICS_OPERATOR`, `ADMINISTRATOR` o `SUPERVISOR`, activo. Son los roles
  que operan o supervisan la logística.
- `consultOwn`: rol `BUYER` y el pedido debe ser suyo (RD-PED-05, RD-ROL-04) →
  `UnauthorizedOperationException` si es de otro comprador.

## Procesamiento

Devuelve los envíos del pedido (`findByOrder`). La lista está **vacía mientras el pedido no se ha
empacado**: así es como se distingue un pedido pendiente de alistamiento de uno empacado.

Como el envío no tiene estado propio (§2.1), lo que esta consulta aporta es el identificador del
envío —el que despacho y entrega necesitan—, la bodega de origen y la dirección de destino. El
avance logístico se lee en el estado del pedido.

## Excepciones

| Excepción | Causa |
|---|---|
| `UnauthorizedOperationException` | Rol no autorizado, o pedido de otro comprador |
| `UserNotActiveException` | El actor está bloqueado |

---

## 5. Trazabilidad servicio ↔ código

| Servicio | Clase Java | Transición del pedido |
|---|---|---|
| Pack Shipment | `PackShipmentService` | — (exige `PAID`) |
| Dispatch Shipment | `DispatchShipmentService` | `PAID → DISPATCHED` |
| Confirm Shipment Delivery | `ConfirmShipmentDeliveryService` | `DISPATCHED → DELIVERED` |
| Consult Shipment | `ConsultShipmentService` | — (solo lectura) |
