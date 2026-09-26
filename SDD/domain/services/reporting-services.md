# Servicios de Reportes

**Paquete:** `application.domain.services.reporting`

## 1. Introducción

Este documento define los servicios del subdominio de **Reportes** (OBJ-12): consolidar información
administrativa para consulta.

## 1.1 Decisión de modelado: el reporte no es una entidad

El objetivo OBJ-12 y el proceso "Consulta de reportes administrativos" **no se modelan como
entidad** (sección 4.22 del modelo de dominio), porque:

- No poseen identidad propia.
- No tienen ciclo de vida.
- No modifican el estado del negocio.

Se resuelven como **consultas de solo lectura** sobre las entidades existentes. Por eso no existe
`Report`, `ReportId` ni `ReportRepositoryPort`: estos servicios componen sus resultados a partir de
los puertos de salida ya declarados para otros agregados.

**Roles autorizados en toda el área:** `ADMINISTRATOR` y `SUPERVISOR` (RD-ROL-06). El supervisor es
perfil de consulta y seguimiento operativo: nunca ejecuta procesos de modificación.

---

# 1. Consult Sales Report

**Clase:** `ConsultSalesReportService`

## Descripción

Consolida la información comercial de la plataforma.

## Entrada

```java
List<Order>    consultCompletedOrders(User<?> actor);
List<Invoice>  consultIssuedInvoices(User<?> actor);
Optional<Money> consultInvoicedTotal(User<?> actor);
```

## Dependencias

`OrderRepositoryPort`, `InvoiceRepositoryPort`, `ValidateRoleAuthorizationService`

Consume puertos de **dos agregados distintos** sin introducir uno propio: es la consecuencia
práctica de no modelar el reporte como entidad.

## Validaciones

Las tres operaciones comparten la misma verificación, centralizada en el método privado
`validateReporting`:

1. Actor activo (RG-01).
2. Rol `ADMINISTRATOR` o `SUPERVISOR`.

## Procesamiento y salida

| Operación | Procesamiento | Salida |
|---|---|---|
| `consultCompletedOrders` | Pedidos en estado `DELIVERED` | Pedidos que alcanzaron el estado terminal |
| `consultIssuedInvoices` | Toda la facturación emitida | Lista de facturas |
| `consultInvoicedTotal` | Reduce los totales con `Money::add` | Total facturado, o vacío |

## Por qué `consultInvoicedTotal` devuelve `Optional<Money>`

`Money` sólo permite sumar importes de la misma moneda y **no existe un `Money` neutro sin moneda**:
`Money.zero(currency)` exige declarar una. Una suma sobre una colección vacía no tiene resultado
posible.

Devolver `Optional.empty()` es más honesto que inventar una moneda por defecto, que sería un
concepto ausente de la especificación (RD-ALC-03). El invocador decide cómo presentar el caso "aún
no hay facturación".

Si existieran facturas en monedas distintas, `Money.add` lanzaría `IllegalArgumentException`: el
objeto de valor impide sumar peras con manzanas.

## Excepciones

| Excepción | Causa |
|---|---|
| `UnauthorizedOperationException` | El rol no es `ADMINISTRATOR` ni `SUPERVISOR` |
| `UserNotActiveException` | El actor está bloqueado |
| `IllegalArgumentException` | Hay facturas en monedas distintas al calcular el total |

---

# 2. Consult Inventory Report

**Clase:** `ConsultInventoryReportService`

## Descripción

Consolida la información del inventario distribuido.

## Entrada

```java
List<InventoryItem>     consultStockByWarehouse(User<?> actor, WarehouseId warehouseId);
List<InventoryItem>     consultDamagedStock(User<?> actor);
List<InventoryMovement> consultMovements(User<?> actor);
```

## Dependencias

`InventoryItemRepositoryPort`, `InventoryMovementRepositoryPort`, `ValidateRoleAuthorizationService`

## Validaciones

Las tres operaciones comparten `validateReporting`: actor activo, rol `ADMINISTRATOR` o
`SUPERVISOR`.

## Procesamiento y salida

| Operación | Procesamiento | Regla que refleja |
|---|---|---|
| `consultStockByWarehouse` | Existencias de una bodega concreta | RD-INV-01: el inventario es distribuido, no hay cifra global |
| `consultDamagedStock` | Filtra por `condition == DAMAGED` | RD-INV-03: quedan excluidas de toda reserva |
| `consultMovements` | Trazabilidad completa | RD-INV-04: todo cambio quedó registrado como movimiento |

### Por qué no existe una consulta de "stock total"

`consultStockByWarehouse` exige el identificador de bodega porque **no existe inventario global**
(RD-INV-01). Ofrecer una cifra agregada de existencias sin bodega contradiría la naturaleza
distribuida del inventario, así que el servicio no la expone.

## Excepciones

| Excepción | Causa |
|---|---|
| `UnauthorizedOperationException` | El rol no es `ADMINISTRATOR` ni `SUPERVISOR` |
| `UserNotActiveException` | El actor está bloqueado |

---

## 3. Trazabilidad servicio ↔ código

| Servicio | Clase Java | Puertos consumidos |
|---|---|---|
| Consult Sales Report | `ConsultSalesReportService` | `OrderRepositoryPort`, `InvoiceRepositoryPort` |
| Consult Inventory Report | `ConsultInventoryReportService` | `InventoryItemRepositoryPort`, `InventoryMovementRepositoryPort` |

Ningún servicio de este subdominio modifica estado: son exclusivamente de lectura, conforme al
perfil del supervisor (RD-ROL-06).
