# Servicios de Devolución

**Paquete:** `application.domain.services.returns`

> El paquete se llama `returns` y no `return` porque esta última es palabra reservada de Java.

## 1. Introducción

Este documento define los servicios del subdominio de **Devoluciones**, primera parte de OBJ-11: el
proceso de posventa por el cual un comprador devuelve unidades de un pedido ya entregado.

---

## 2. Contexto del modelo de dominio

```text
DomainEntity<ProductReturnId>
      │
      └── ProductReturn
             │
             ├── orderId     : OrderId
             ├── orderLineId : OrderLineId
             └── quantity    : Quantity
```

La devolución se origina siempre en una **línea concreta** de un pedido existente, no en el pedido
completo: el comprador puede devolver parte de lo comprado.

---

## 3. Invariantes del subdominio

| Código | Invariante |
|---|---|
| **RD-POS-01** | Toda devolución se origina en una línea de un pedido existente y reintegra existencias mediante un movimiento `RETURN`. |
| **RD-PED-05** | El pedido pertenece a un único comprador; sólo él puede devolver. |

Además, el modelo impone que el pedido esté en `DELIVERED`: el ciclo de posventa sólo se abre cuando
el ciclo comercial se ha cerrado.

---

# 1. Request Return

**Clase:** `RequestReturnService`

## Descripción

Registra la devolución de una línea de un pedido entregado.

## Entrada

```java
ProductReturn requestReturn(Buyer buyer,
                            Order order,
                            OrderLine orderLine,
                            Quantity quantity,
                            ProductReturnId returnId)
```

| Parámetro | Descripción |
|---|---|
| `orderLine` | Línea concreta del pedido que se devuelve |
| `quantity` | Unidades devueltas, nunca superiores a las de la línea |

## Dependencias

`ProductReturnRepositoryPort`, `ValidateRoleAuthorizationService`, `ValidateBuyerOwnershipService`

## Validaciones

1. **Usuario**: activo (RG-01), rol `BUYER`.
2. **Pertenencia del pedido**: `ValidateBuyerOwnershipService.validateOrder` → un comprador nunca
   devuelve sobre el pedido de otro (RD-PED-05, RD-ROL-04).
3. **Estado del pedido**: debe ser `DELIVERED`; si no → `ReturnNotAllowedException` (RD-POS-01).
4. **Pertenencia de la línea**: `ProductReturn.registerReturn` verifica
   `orderLine.belongsTo(order)`.
5. **Cantidad**: no puede exceder la de la línea:
   `orderLine.getQuantity().isGreaterThanOrEqual(quantity)`.
6. **Cantidad positiva**: una devolución no puede ser de cero unidades.

## Procesamiento

`buyer.requestReturn(...)` delega en la fábrica `ProductReturn.registerReturn(...)`, que vuelve a
verificar las condiciones 3, 4 y 5 dentro del modelo. La regla vive en el dominio; el servicio la
orquesta y traduce la primera a una excepción específica.

## Efecto posterior

Este servicio **no reintegra existencias**. El reintegro físico lo ejecuta `RestockReturnService`
(subdominio de inventario), que genera el movimiento `RETURN` (RD-POS-01, RD-INV-04).

Tampoco emite el reembolso: eso corresponde a `IssueRefundService`, gestionado por el administrador
(RD-POS-02).

## Excepciones

| Excepción | Causa |
|---|---|
| `ReturnNotAllowedException` | El pedido no está en `DELIVERED` |
| `UnauthorizedOperationException` | El pedido es de otro comprador, o el rol no es `BUYER` |
| `IllegalArgumentException` | La línea no pertenece al pedido, o la cantidad excede la de la línea |
| `UserNotActiveException` | El comprador está bloqueado |

---

# 2. Consult Return

**Clase:** `ConsultReturnService`

## Descripción

Consulta las devoluciones delimitando el alcance de cada rol.

## Entrada

```java
ProductReturn       consult(User<?> actor, ProductReturnId returnId);
List<ProductReturn> consultOwn(Buyer buyer, Order order);
```

## Autorización por operación

| Operación | Roles | Alcance |
|---|---|---|
| `consult` | `ADMINISTRATOR`, `SUPERVISOR`, `LOGISTICS_OPERATOR` | Cualquier devolución |
| `consultOwn` | `BUYER` | Sólo las de sus propios pedidos |

**Por qué esos tres roles.** El administrador accede porque gestiona los reembolsos derivados
(RD-POS-02); el operador logístico porque reintegra físicamente las existencias; el supervisor por
ser perfil de seguimiento operativo (RD-ROL-06).

## Procesamiento de `consultOwn`

1. Valida rol `BUYER` y estado activo.
2. Valida que el pedido pertenezca al comprador.
3. Devuelve las devoluciones registradas sobre ese pedido.

## Excepciones

| Excepción | Causa |
|---|---|
| `EntityNotFoundException` | No existe devolución con ese identificador |
| `UnauthorizedOperationException` | El pedido es de otro comprador, o el rol no consulta devoluciones |

---

## 4. Trazabilidad servicio ↔ código

| Servicio | Clase Java | Efecto |
|---|---|---|
| Request Return | `RequestReturnService` | Registra la `ProductReturn` |
| Consult Return | `ConsultReturnService` | — (solo lectura) |

El reintegro de existencias se documenta en [inventory-services.md](inventory-services.md) y el
reembolso en [refund-services.md](refund-services.md).
