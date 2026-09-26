# Servicios de Facturación

**Paquete:** `application.domain.services.invoice`

## 1. Introducción

Este documento define los servicios del subdominio de **Facturación** (OBJ-09): la información
comercial asociada a la venta.

La regla que gobierna el subdominio es RD-FAC-01: **la facturación se genera a partir del pedido
pagado** y refleja su valor comercial.

---

## 2. Contexto del modelo de dominio

```text
DomainEntity<InvoiceId>
      │
      └── Invoice
             │
             ├── orderId     : OrderId
             ├── buyerId     : BuyerId
             └── totalAmount : Money
```

La relación `Order → Invoice` es **1 a 1**: un pedido se factura una sola vez.

La factura copia el comprador y el total del pedido en el momento de su emisión, de modo que queda
como documento inmutable de la venta. `Invoice.matchesOrderTotal(order)` permite verificar en
cualquier momento la coherencia entre ambos.

---

# 1. Issue Invoice

**Clase:** `IssueInvoiceService`

## Descripción

Emite la factura de un pedido una vez validado su pago.

## Entrada

```java
Invoice issue(User<?> actor, Order order, InvoiceId invoiceId)
```

| Parámetro | Descripción |
|---|---|
| `order` | Pedido a facturar, con el pago ya validado |
| `invoiceId` | Identificador asignado a la nueva factura |

## Dependencias

`InvoiceRepositoryPort`, `ValidateRoleAuthorizationService`

## Validaciones

| # | Validación | Dónde | Excepción |
|---|---|---|---|
| 1 | Actor activo | Servicio (RG-01) | `UserNotActiveException` |
| 2 | Rol `SELLER` o `ADMINISTRATOR` | Servicio | `UnauthorizedOperationException` |
| 3 | El pedido no está en `CART` ni `PENDING_PAYMENT` | Servicio (RD-FAC-01) | `InvalidOrderTransitionException` |
| 4 | Misma verificación de estado | `Invoice.issueFor` | `IllegalStateException` |

La validación 3 es la traducción explícita de RD-FAC-01: sólo se factura lo pagado. La 4 es la
defensa redundante del modelo.

## Procesamiento

`Invoice.issueFor(invoiceId, order)` construye la factura copiando `buyerId` y `totalAmount` del
pedido. El dominio nunca recalcula el total: lo toma del pedido, que ya lo calculó sumando sus
líneas.

## Salida

La factura emitida, cuyo total coincide con el del pedido por construcción.

## Excepciones

| Excepción | Causa |
|---|---|
| `InvalidOrderTransitionException` | El pago del pedido no ha sido validado |
| `UnauthorizedOperationException` | El rol no emite facturas |
| `UserNotActiveException` | El actor está bloqueado |

---

# 2. Consult Invoice

**Clase:** `ConsultInvoiceService`

## Descripción

Consulta la facturación, delimitando el alcance de cada rol.

## Entrada

```java
Invoice       consultByOrder(User<?> actor, Order order);
Invoice       consultOwn(Buyer buyer, Order order);
List<Invoice> consultOwn(Buyer buyer);
```

## Dependencias

`InvoiceRepositoryPort`, `ValidateRoleAuthorizationService`, `ValidateBuyerOwnershipService`

## Autorización por operación

| Operación | Roles | Alcance |
|---|---|---|
| `consultByOrder` | `SELLER`, `ADMINISTRATOR`, `SUPERVISOR` | Cualquier factura |
| `consultOwn(Buyer, Order)` | `BUYER` | Sólo las propias |
| `consultOwn(Buyer)` | `BUYER` | Todas las propias |

## Validaciones de `consultOwn(Buyer, Order)`

Aplica una **doble verificación de pertenencia** deliberada:

1. Rol `BUYER`, actor activo.
2. `validateBuyerOwnershipService.validateOrder(buyer, order)` — el pedido es del comprador.
3. Se recupera la factura del pedido; si no existe → `InvoiceNotIssuedException`.
4. `validateBuyerOwnershipService.validateInvoice(buyer, invoice)` — la factura está emitida a ese
   comprador.

El paso 4 no es redundante en sentido estricto: protege ante una eventual inconsistencia de datos en
la que la factura de un pedido estuviera emitida a otro comprador.

## Salida

| Operación | Salida |
|---|---|
| `consultByOrder` / `consultOwn(Buyer, Order)` | La factura del pedido |
| `consultOwn(Buyer)` | Todas las facturas emitidas al comprador |

## Excepciones

| Excepción | Causa |
|---|---|
| `InvoiceNotIssuedException` | El pedido aún no ha sido facturado |
| `UnauthorizedOperationException` | El pedido o la factura son de otro comprador, o el rol no consulta facturación |

---

## 3. Trazabilidad servicio ↔ código

| Servicio | Clase Java | Rol requerido |
|---|---|---|
| Issue Invoice | `IssueInvoiceService` | `SELLER`, `ADMINISTRATOR` |
| Consult Invoice | `ConsultInvoiceService` | Según operación |

La factura es además la base sobre la que se aplica el reembolso, documentado en
[refund-services.md](refund-services.md).
