# Servicios de Inventario

**Paquete:** `application.domain.services.inventory`

## 1. Introducción

Este documento define los servicios del subdominio de **Inventario** (Dominio 6 de la
especificación funcional). Son los responsables de administrar las existencias disponibles para
comercialización y de garantizar su trazabilidad.

Es el área con las reglas más duras del sistema: la especificación establece de forma explícita que
**no se permitirán existencias negativas bajo ninguna circunstancia** y que **no se puede reservar
inventario inexistente o marcado como "Dañado"**.

Los detalles de implementación —contratos REST, mapeos de persistencia, entidades JPA, documentos
MongoDB— se documentan por separado: este documento describe únicamente el comportamiento de
dominio.

---

## 2. Contexto del modelo de dominio

Las existencias se representan con `InventoryItem`, y cada cambio con `InventoryMovement`:

```text
DomainEntity<InventoryItemId>
      │
      └── InventoryItem
             │
             ├── productId         : ProductId
             ├── warehouseId       : WarehouseId
             ├── availableQuantity : Quantity
             ├── reservedQuantity  : Quantity
             ├── condition         : StockCondition
             └── movements         : List<InventoryMovement>

DomainEntity<InventoryMovementId>
      │
      └── InventoryMovement
             │
             ├── inventoryItemId : InventoryItemId
             ├── movementType    : InventoryMovementType
             └── quantity        : Quantity
```

El inventario es **distribuido**: toda existencia está vinculada obligatoriamente a un producto y a
una bodega específica (RD-INV-01). No existe inventario global ni sin bodega.

El par `(productId, warehouseId)` identifica de forma única un registro de existencias (RD-ID-05):
no pueden existir dos `InventoryItem` para el mismo producto en la misma bodega.

---

## 3. Objetos de valor utilizados

```text
InventoryItem
 │
 ├── availableQuantity : Quantity              ── nunca negativa
 ├── reservedQuantity  : Quantity              ── nunca negativa
 └── condition         : StockCondition        ── AVAILABLE | DAMAGED

InventoryMovement
 │
 ├── quantity     : Quantity
 └── movementType : InventoryMovementType      ── INBOUND | RESERVATION |
                                                  SALE_OUTBOUND | ADJUSTMENT | RETURN

User (actor)
 ├── status : UserStatus
 └── role   : UserRole
```

### 3.1 Validación de `Quantity`

`Quantity` hace **imposible construir un valor negativo**: el constructor rechaza cualquier entero
menor que cero. Esa es la garantía estructural de RD-INV-02.

Adicionalmente, `Quantity.subtract` falla si el resultado quedaría por debajo de cero, de modo que
ninguna operación puede dejar el saldo negativo ni siquiera de forma transitoria.

Los servicios **no** comparan cantidades con enteros sueltos:

```java
// Incorrecto
if (item.getAvailableQuantity().getValue() >= 5) { ... }

// Correcto
if (item.getAvailableQuantity().isGreaterThanOrEqual(quantity)) { ... }
```

### 3.2 Validación de `StockCondition`

`StockCondition` es un catálogo cerrado con una única transición permitida:

```text
AVAILABLE ──markAsDamaged()──▶ DAMAGED
```

No existe transición de regreso. Los servicios nunca comparan la condición con cadenas:

```java
// Incorrecto
item.getCondition().equals("DAMAGED");

// Correcto
item.getCondition().allowsReservation();
```

### 3.3 Validación de `InventoryMovementType`

Catálogo cerrado de **cinco** tipos. Ningún servicio crea un tipo fuera de este conjunto
(RD-INV-04):

| Tipo | Efecto sobre el saldo | Servicio que lo genera |
|---|---|---|
| `INBOUND` | `available += cantidad` | `ReceiveStockService` |
| `RESERVATION` | `available -= q`, `reserved += q` | `ReserveStockService` |
| `SALE_OUTBOUND` | `reserved -= cantidad` | `IssueForSaleService` |
| `ADJUSTMENT` | corrección del disponible | `AdjustStockService`, `ReleaseReservationService` |
| `RETURN` | `available += cantidad` | `RestockReturnService` |

---

## 4. Invariantes del subdominio

| Código | Invariante |
|---|---|
| **RD-INV-01** | Toda existencia está vinculada a un producto y una bodega específica. No hay inventario global. |
| **RD-INV-02** | No se permiten existencias negativas bajo ninguna circunstancia. |
| **RD-INV-03** | No se puede reservar inventario inexistente ni marcado como `DAMAGED`. |
| **RD-INV-04** | Todo cambio de existencias se materializa como un movimiento de uno de los cinco tipos. No hay modificación directa de cantidad sin movimiento asociado. |
| **RD-INV-05** | Los productos digitales no generan existencias ni movimientos. |
| **RD-ID-05** | El par (producto, bodega) es único. |

---

## 5. Ciclo de vida de las existencias

```text
                     ┌─────────────┐
   INBOUND ─────────▶│  DISPONIBLE │◀──────── RETURN
                     └──────┬──────┘
                            │ RESERVATION
                            ▼
                     ┌─────────────┐
                     │  RESERVADO  │
                     └──────┬──────┘
                            │ SALE_OUTBOUND
                            ▼
                     ┌─────────────┐
                     │  DESPACHADO │
                     └─────────────┘

   ADJUSTMENT ──────▶ corrige la cantidad en cualquier punto del ciclo,
                      sin permitir jamás un saldo negativo.

   Condición DAMAGED ──▶ excluye las existencias de toda reserva.
```

---

## 6. Patrón estándar del subdominio

Todo servicio de inventario que modifica estado sigue exactamente esta secuencia:

1. **Validar el rol del actor** con `ValidateRoleAuthorizationService`, que a su vez verifica que el
   usuario esté activo (RG-01).
2. **Verificar las precondiciones explícitamente**, antes de invocar el modelo, para poder lanzar la
   excepción de dominio específica en lugar de dejar escapar la genérica (RD-SRV-06).
3. **Invocar el comportamiento del modelo**, que devuelve el `InventoryMovement` generado. El
   servicio no reimplementa la aritmética del saldo: eso vive en `InventoryItem`.
4. **Persistir ambos**: el `InventoryItem` actualizado y el `InventoryMovement` generado. Nunca uno
   sin el otro, porque no existe cambio de existencias sin movimiento (RD-INV-04).

**Roles autorizados.** La administración del inventario es responsabilidad compartida entre
`SELLER` y `LOGISTICS_OPERATOR` (RD-ROL-06). El rol `BUYER` está deliberadamente ausente de todo
este subdominio: un comprador nunca administra inventarios (RD-ROL-04).

---

# 1. Receive Stock

**Clase:** `ReceiveStockService`

## Descripción

Registra existencias en una bodega. Es la entrada de mercancía al inventario distribuido y el punto
de partida del ciclo de vida de las existencias.

## Entrada

```java
InventoryMovement receive(User<?> actor,
                          InventoryItem inventoryItem,
                          Quantity quantity,
                          InventoryMovementId movementId)
```

| Parámetro | Tipo | Descripción |
|---|---|---|
| `actor` | `User<?>` | Usuario autenticado que registra la entrada |
| `inventoryItem` | `InventoryItem` | Registro de existencias del producto en la bodega |
| `quantity` | `Quantity` | Unidades que ingresan, nunca cero |
| `movementId` | `InventoryMovementId` | Identificador asignado al movimiento resultante |

## Dependencias

`InventoryItemRepositoryPort`, `InventoryMovementRepositoryPort`, `ValidateRoleAuthorizationService`

## Validación del usuario

- El actor debe estar activo (RG-01). Si está bloqueado → `UserNotActiveException`.
- El rol debe ser `SELLER` o `LOGISTICS_OPERATOR` (RD-ROL-06). En caso contrario →
  `UnauthorizedOperationException`.

## Validación de la cantidad

- `quantity` no puede ser nula.
- `quantity` debe ser positiva: un movimiento no puede ser de cero unidades. La verificación vive en
  `InventoryItem.requirePositive`.

## Procesamiento

1. `inventoryItem.receive(quantity, movementId)` genera un movimiento `INBOUND` y lo añade al
   historial del registro.
2. Suma las unidades al disponible: `availableQuantity += quantity`.

## Estado resultante

```text
availableQuantity : anterior + quantity
reservedQuantity  : sin cambios
condition         : sin cambios
movements         : + 1 movimiento INBOUND
```

## Persistencia

Se persisten el `InventoryItem` actualizado y el `InventoryMovement` generado.

## Excepciones

| Excepción | Causa |
|---|---|
| `UserNotActiveException` | El actor está bloqueado |
| `UnauthorizedOperationException` | El rol no administra inventario |
| `IllegalArgumentException` | Cantidad de cero unidades (invariante del modelo) |

---

# 2. Reserve Stock

**Clase:** `ReserveStockService`

## Descripción

Compromete las existencias de una línea de pedido. Es la operación con más precondiciones del
subdominio, porque concentra RD-INV-02 y RD-INV-03.

## Entrada

```java
InventoryMovement reserve(User<?> actor,
                          OrderLine orderLine,
                          InventoryItem inventoryItem,
                          InventoryMovementId movementId)
```

| Parámetro | Tipo | Descripción |
|---|---|---|
| `orderLine` | `OrderLine` | Línea del pedido que compromete las existencias |
| `inventoryItem` | `InventoryItem` | Registro de existencias del producto de esa línea |

La cantidad **no** se recibe como parámetro: la aporta la propia línea del pedido
(`orderLine.getQuantity()`), de modo que la reserva nunca puede desalinearse del compromiso
comercial.

## Validación del usuario

Rol `SELLER` o `LOGISTICS_OPERATOR`, activo.

## Validación de la condición de las existencias

```java
if (!inventoryItem.getCondition().allowsReservation()) {
    throw new DamagedStockException(inventoryItem.getProductId());
}
```

Las existencias marcadas como `DAMAGED` quedan excluidas de toda reserva (RD-INV-03), **aunque el
saldo disponible sea suficiente**.

## Validación de la disponibilidad

```java
if (!inventoryItem.canReserve(orderLine.getQuantity())) {
    throw new InsufficientStockException(productId, available, requested);
}
```

`canReserve` exige simultáneamente: cantidad positiva, condición que permita reserva, y saldo
disponible mayor o igual al solicitado.

## Validación de correspondencia producto–existencia

`OrderLine.reserveStock` rechaza el registro si no corresponde al producto de la línea:

```text
inventoryItem.productId == orderLine.productId
```

## Procesamiento

1. `orderLine.reserveStock(inventoryItem, movementId)` genera un movimiento `RESERVATION`.
2. Descuenta del disponible y suma al reservado.

## Estado resultante

```text
availableQuantity : anterior - quantity
reservedQuantity  : anterior + quantity
movements         : + 1 movimiento RESERVATION
```

El total físico no cambia: las unidades se mueven de disponible a reservado.

## Persistencia

`InventoryItem` actualizado + `InventoryMovement` generado.

## Excepciones

| Excepción | Causa |
|---|---|
| `DamagedStockException` | Las existencias están en condición `DAMAGED` (RD-INV-03) |
| `InsufficientStockException` | El disponible no cubre la cantidad de la línea (RD-INV-02) |
| `IllegalArgumentException` | El registro de existencias no es el del producto de la línea |
| `UnauthorizedOperationException` | Rol no autorizado |

---

# 3. Release Reservation

**Clase:** `ReleaseReservationService`

## Descripción

Libera las existencias comprometidas por una línea de pedido y las devuelve al saldo disponible.

## Entrada

```java
InventoryMovement release(User<?> actor,
                          OrderLine orderLine,
                          InventoryItem inventoryItem,
                          InventoryMovementId movementId)
```

## Validación del usuario

Rol `SELLER` o `LOGISTICS_OPERATOR`, activo.

## Validación del saldo reservado

`InventoryItem.releaseReservation` exige que haya suficiente reservado:

```text
reservedQuantity >= quantity
```

De lo contrario se rechaza: liberar más de lo comprometido dejaría el saldo reservado en negativo,
lo que RD-INV-02 prohíbe.

## Procesamiento

1. `orderLine.releaseStock(inventoryItem, movementId)` genera un movimiento `ADJUSTMENT`.
2. Descuenta del reservado y devuelve las unidades al disponible.

## Decisión de modelado: por qué `ADJUSTMENT`

La especificación define un catálogo **cerrado** de cinco tipos de movimiento (RD-INV-04) y no
incluye un tipo "liberación de reserva". Se registra como `ADJUSTMENT`, que es el tipo que el
negocio define para corregir las existencias registradas. No se inventa un sexto tipo, porque
cualquier concepto ausente de la especificación queda fuera del modelo (RD-ALC-03).

## Estado resultante

```text
availableQuantity : anterior + quantity
reservedQuantity  : anterior - quantity
movements         : + 1 movimiento ADJUSTMENT
```

## Excepciones

| Excepción | Causa |
|---|---|
| `IllegalStateException` | No hay suficiente saldo reservado para liberar |
| `UnauthorizedOperationException` | Rol no autorizado |

---

# 4. Issue For Sale

**Clase:** `IssueForSaleService`

## Descripción

Retira físicamente de la bodega las existencias comprometidas, porque el pedido fue despachado. Es
la salida de mercancía que soporta el envío (RD-LOG-02).

## Entrada

```java
InventoryMovement issue(User<?> actor,
                        OrderLine orderLine,
                        InventoryItem inventoryItem,
                        InventoryMovementId movementId)
```

## Validación del usuario

**Únicamente** rol `LOGISTICS_OPERATOR`. A diferencia del resto del subdominio, el vendedor no
ejecuta esta operación: la salida física de la bodega es responsabilidad exclusiva del operador
logístico (RD-ROL-06).

## Validación del saldo reservado

`InventoryItem.issueForSale` exige:

```text
reservedQuantity >= quantity
```

No se puede despachar lo que no fue reservado previamente. Esto obliga a que el ciclo
`RESERVATION → SALE_OUTBOUND` se respete en orden.

## Procesamiento

1. `inventoryItem.issueForSale(orderLine.getQuantity(), movementId)` genera un movimiento
   `SALE_OUTBOUND`.
2. Descuenta del reservado. El disponible **no** se toca: esas unidades ya habían salido del
   disponible al reservarse.

## Estado resultante

```text
availableQuantity : sin cambios
reservedQuantity  : anterior - quantity
movements         : + 1 movimiento SALE_OUTBOUND
```

Es el único movimiento que reduce el total físico de la bodega.

## Excepciones

| Excepción | Causa |
|---|---|
| `IllegalStateException` | No hay suficiente saldo reservado para despachar |
| `UnauthorizedOperationException` | El rol no es `LOGISTICS_OPERATOR` |

---

# 5. Adjust Stock

**Clase:** `AdjustStockService`

## Descripción

Corrige el saldo disponible registrado, por ejemplo tras un conteo físico que no coincide con el
sistema.

## Entrada

```java
InventoryMovement adjust(User<?> actor,
                         InventoryItem inventoryItem,
                         Quantity newAvailableQuantity,
                         InventoryMovementId movementId)
```

| Parámetro | Descripción |
|---|---|
| `newAvailableQuantity` | Saldo disponible corregido, **no** la diferencia |

## Validación del usuario

Rol `SELLER` o `LOGISTICS_OPERATOR`, activo.

## Validación de la cantidad

`newAvailableQuantity` no puede ser negativa, garantía estructural de `Quantity` (RD-INV-02). El
servicio no necesita verificarlo: el objeto de valor lo hace imposible.

## Procesamiento

1. `InventoryItem.adjust` calcula la **diferencia** entre el saldo nuevo y el actual, en valor
   absoluto.
2. Genera un movimiento `ADJUSTMENT` por esa diferencia.
3. Reemplaza el disponible por el valor corregido.

## Decisión de modelado: el motivo del ajuste

La especificación funcional nombra esta operación con un motivo (`adjust(quantity, reason)`), pero
el modelo de `InventoryMovement` **no declara ningún atributo capaz de almacenarlo**. No se añade un
campo no contemplado por la especificación (RD-ALC-03), así que el motivo queda fuera del dominio y
corresponderá al registro de auditoría cuando se implemente.

## Estado resultante

```text
availableQuantity : newAvailableQuantity
reservedQuantity  : sin cambios
movements         : + 1 movimiento ADJUSTMENT (por la diferencia)
```

## Excepciones

| Excepción | Causa |
|---|---|
| `IllegalArgumentException` | La cantidad es nula |
| `UnauthorizedOperationException` | Rol no autorizado |

---

# 6. Mark Damaged

**Clase:** `MarkDamagedService`

## Descripción

Marca las existencias como dañadas, lo que las excluye de toda reserva futura.

## Entrada

```java
InventoryItem markAsDamaged(User<?> actor, InventoryItem inventoryItem)
```

## Dependencias

`InventoryItemRepositoryPort`, `ValidateRoleAuthorizationService`

Nótese que **no** inyecta `InventoryMovementRepositoryPort`: esta operación cambia la condición de
las existencias, no su cantidad, por lo que no genera movimiento alguno. RD-INV-04 exige movimiento
para todo cambio de *existencias*, no para un cambio de *condición*.

## Validación del usuario

Rol `SELLER` o `LOGISTICS_OPERATOR`, activo.

## Validación de la transición

```text
AVAILABLE ──▶ DAMAGED    permitida
DAMAGED   ──▶ AVAILABLE  prohibida
DAMAGED   ──▶ DAMAGED    prohibida
```

`StockCondition.canTransitionTo` sólo acepta `AVAILABLE → DAMAGED` (RD-VO-12). Marcar como dañado
algo ya dañado se rechaza.

## Estado resultante

```text
condition         : DAMAGED
availableQuantity : sin cambios
reservedQuantity  : sin cambios
```

Las unidades siguen contabilizadas, pero `canReserve` devolverá `false` para cualquier cantidad.

## Excepciones

| Excepción | Causa |
|---|---|
| `IllegalStateException` | Las existencias ya estaban en condición `DAMAGED` |
| `UnauthorizedOperationException` | Rol no autorizado |

---

# 7. Restock Return

**Clase:** `RestockReturnService`

## Descripción

Reintegra a la bodega las existencias procedentes de una devolución de posventa.

## Entrada

```java
InventoryMovement restock(User<?> actor,
                          ProductReturn productReturn,
                          InventoryItem inventoryItem,
                          InventoryMovementId movementId)
```

La cantidad la aporta la devolución (`productReturn.getQuantity()`), que a su vez fue validada
contra la cantidad de la línea del pedido al registrarse.

## Validación del usuario

Rol `LOGISTICS_OPERATOR` o `SELLER`, activo.

## Procesamiento

1. `productReturn.restock(inventoryItem, movementId)` delega en
   `InventoryItem.returnStock(quantity, movementId)`.
2. Genera un movimiento `RETURN` y suma las unidades al disponible.

## Estado resultante

```text
availableQuantity : anterior + quantity
reservedQuantity  : sin cambios
movements         : + 1 movimiento RETURN
```

## Relación con el ciclo de posventa

Este servicio es el eslabón físico de RD-POS-01: la devolución se registra en
`RequestReturnService` (subdominio de devoluciones) y es aquí donde efectivamente reintegra
existencias.

## Excepciones

| Excepción | Causa |
|---|---|
| `IllegalArgumentException` | Cantidad de cero unidades |
| `UnauthorizedOperationException` | Rol no autorizado |

---

# 8. Consult Inventory

**Clase:** `ConsultInventoryService`

## Descripción

Consulta las existencias distribuidas y su trazabilidad. Es el único servicio de solo lectura del
subdominio.

## Entrada

```java
InventoryItem       consultStock(User<?> actor, ProductId productId, WarehouseId warehouseId);
List<InventoryItem> consultByWarehouse(User<?> actor, WarehouseId warehouseId);
List<InventoryItem> consultByProduct(User<?> actor, ProductId productId);
List<InventoryMovement> consultMovements(User<?> actor, InventoryItem inventoryItem);
```

## Validación del usuario

Roles autorizados: `SELLER`, `LOGISTICS_OPERATOR`, `ADMINISTRATOR`, `SUPERVISOR`.

El rol `BUYER` está **deliberadamente ausente**: el comprador nunca administrará información de
inventarios (RD-ROL-04). No es un olvido, es la regla.

## Procesamiento

- `consultStock` usa la clave única del inventario distribuido, el par (producto, bodega)
  (RD-ID-05). Si el producto no tiene registro en esa bodega → `EntityNotFoundException`.
- `consultMovements` devuelve el historial completo del registro: todo cambio quedó materializado
  como movimiento (RD-INV-04).

## Excepciones

| Excepción | Causa |
|---|---|
| `EntityNotFoundException` | El producto no tiene existencias en esa bodega |
| `UnauthorizedOperationException` | El rol no consulta inventarios (por ejemplo, `BUYER`) |

---

## 9. Trazabilidad servicio ↔ código

| Servicio | Clase Java | Movimiento generado |
|---|---|---|
| Receive Stock | `ReceiveStockService` | `INBOUND` |
| Reserve Stock | `ReserveStockService` | `RESERVATION` |
| Release Reservation | `ReleaseReservationService` | `ADJUSTMENT` |
| Issue For Sale | `IssueForSaleService` | `SALE_OUTBOUND` |
| Adjust Stock | `AdjustStockService` | `ADJUSTMENT` |
| Mark Damaged | `MarkDamagedService` | — (cambia condición) |
| Restock Return | `RestockReturnService` | `RETURN` |
| Consult Inventory | `ConsultInventoryService` | — (solo lectura) |
