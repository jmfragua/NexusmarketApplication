# Servicios de Reembolso

**Paquete:** `application.domain.services.refund`

## 1. Introducción

Este documento define los servicios del subdominio de **Reembolso**, segunda parte de OBJ-11.

La regla que gobierna el subdominio es RD-POS-02: **todo reembolso deriva de una devolución y se
aplica sobre la factura del pedido correspondiente**.

Es una responsabilidad **compartida** (RD-ROL-06): el comprador solicita la devolución; el
administrador gestiona el reembolso.

---

## 2. Contexto del modelo de dominio

```text
DomainEntity<RefundId>
      │
      └── Refund
             │
             ├── productReturnId : ProductReturnId
             ├── invoiceId       : InvoiceId
             └── amount          : Money
```

La relación `ProductReturn → Refund` es **1 a 1**. El reembolso enlaza dos entidades de subdominios
distintos: la devolución que lo origina y la factura sobre la que se aplica.

---

# 1. Issue Refund

**Clase:** `IssueRefundService`

## Descripción

Emite el reembolso de una devolución, aplicado sobre la factura del pedido que la origina.

## Entrada

```java
Refund issue(User<?> actor,
             ProductReturn productReturn,
             Invoice invoice,
             Money amount,
             RefundId refundId)
```

| Parámetro | Descripción |
|---|---|
| `productReturn` | Devolución que da lugar al reembolso |
| `invoice` | Factura del pedido sobre la que se aplica |
| `amount` | Valor reembolsado |

### Por qué el monto es un parámetro

La especificación no define una fórmula de cálculo del reembolso —podría ser total, parcial o estar
sujeto a criterio administrativo—, así que el dominio no la inventa (RD-ALC-03). Lo que sí impone es
el límite: nunca puede exceder el total facturado.

## Dependencias

`RefundRepositoryPort`, `ValidateRoleAuthorizationService`

## Validaciones

| # | Validación | Dónde | Excepción |
|---|---|---|---|
| 1 | Actor activo | Servicio (RG-01) | `UserNotActiveException` |
| 2 | Rol `ADMINISTRATOR` | Servicio (RD-ROL-06) | `UnauthorizedOperationException` |
| 3 | El monto no excede el total facturado | Servicio (RD-POS-02) | `RefundExceedsInvoiceException` |
| 4 | La factura documenta el pedido de la devolución | `Refund.issueFor` | `IllegalArgumentException` |
| 5 | El monto no excede el total (revalidación) | `Refund.issueFor` | `IllegalArgumentException` |

La validación 4 es clave: impide aplicar el reembolso de una devolución sobre la factura de un
pedido distinto.

## Validación de moneda

`Money.isGreaterThan` sólo compara importes de la **misma moneda** y lanza `IllegalArgumentException`
si difieren. Un reembolso expresado en una moneda distinta a la de la factura queda rechazado por el
propio objeto de valor, sin necesidad de una verificación explícita en el servicio.

## Salida

El reembolso emitido, enlazado a la devolución y a la factura.

## Excepciones

| Excepción | Causa |
|---|---|
| `RefundExceedsInvoiceException` | El monto supera el total facturado |
| `UnauthorizedOperationException` | El actor no es administrador |
| `IllegalArgumentException` | La factura no documenta el pedido de la devolución, o las monedas difieren |

---

# 2. Consult Refund

**Clase:** `ConsultRefundService`

## Descripción

Consulta los reembolsos de la plataforma.

## Entrada

```java
Refund       consult(User<?> actor, RefundId refundId);
Refund       consultByReturn(User<?> actor, ProductReturn productReturn);
List<Refund> consultAll(User<?> actor);
```

## Dependencias

`RefundRepositoryPort`, `ValidateRoleAuthorizationService`

## Validaciones

1. Actor activo (RG-01).
2. Rol `ADMINISTRATOR` o `SUPERVISOR` en las tres operaciones. El administrador gestiona los
   reembolsos; el supervisor los consulta como perfil de seguimiento (RD-ROL-06).

## Comportamiento de `consultByReturn`

Lanza `EntityNotFoundException` mientras la devolución no haya sido reembolsada todavía. Esto es
deliberado: la relación devolución → reembolso es 1 a 1 y **sólo existe una vez emitido**. Una
devolución registrada pero aún no reembolsada es un estado legítimo del negocio, no un error de
datos.

## Salida

| Operación | Salida |
|---|---|
| `consult` | El reembolso con ese identificador |
| `consultByReturn` | El reembolso derivado de esa devolución |
| `consultAll` | Todos los reembolsos de la plataforma |

## Excepciones

| Excepción | Causa |
|---|---|
| `EntityNotFoundException` | No existe ese reembolso, o la devolución aún no ha sido reembolsada |
| `UnauthorizedOperationException` | El rol no es `ADMINISTRATOR` ni `SUPERVISOR` |

---

## 3. Trazabilidad servicio ↔ código

| Servicio | Clase Java | Rol requerido |
|---|---|---|
| Issue Refund | `IssueRefundService` | `ADMINISTRATOR` |
| Consult Refund | `ConsultRefundService` | `ADMINISTRATOR`, `SUPERVISOR` |

La devolución que origina el reembolso se documenta en [return-services.md](return-services.md), y
la factura sobre la que se aplica en [invoice-services.md](invoice-services.md).
