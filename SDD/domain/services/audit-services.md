# Servicios de Auditoría

**Paquete:** `application.domain.services.audit`

## 1. Introducción

Este documento define el subdominio de **Auditoría**: el registro inmutable de las operaciones
relevantes del negocio, para trazabilidad y seguimiento posterior.

A diferencia del resto de los subdominios, este **no proviene de la especificación funcional de
NexusMarket**, que no menciona auditoría. Se incorpora como decisión de arquitectura para dar
soporte al requerimiento del curso de usar una base NoSQL además de la relacional: la bitácora es el
único agregado cuya naturaleza —registros heterogéneos, append-only, sin relaciones— justifica
realmente un almacenamiento documental en lugar de uno relacional.

Queda explícitamente documentado como extensión al modelo original (RD-AUD-01), no como un
requisito derivado del enunciado.

---

## 2. Contexto del modelo de dominio

```text
DomainEntity<OperationLogId>
      │
      └── OperationLog
             │
             ├── operationType      : OperationType
             ├── actorId            : Identifier
             ├── actorRole          : UserRole
             ├── affectedEntityType : String
             ├── affectedEntityId   : String
             ├── details            : String
             └── occurredAt         : Instant
```

### 2.1 Sin ciclo de vida

`OperationLog` es el único agregado del dominio **sin ciclo de vida**: es un hecho consumado,
append-only. No tiene estado, no transiciona, y una vez registrado nunca se modifica ni se elimina.
Todos sus campos son `final`.

### 2.2 Referencias por valor, no por relación

La entidad afectada se guarda como dos cadenas (`affectedEntityType`, `affectedEntityId`) en lugar
de una referencia tipada. Es deliberado: una bitácora que referenciara entidades vivas quedaría
inconsistente cuando esas entidades cambien, y dejaría de ser un registro histórico fiel. Lo mismo
aplica al actor, del que se guarda su identificador y el rol **que tenía en el momento** de la
operación.

---

## 3. Objetos de valor utilizados

```text
OperationLog
 ├── operationType : OperationType   ── SELLER_REGISTERED | ORDER_STATUS_CHANGED |
 │                                      INVENTORY_ADJUSTED | REFUND_ISSUED
 ├── actorId       : Identifier      ── identificador del usuario que operó
 └── actorRole     : UserRole        ── rol en el momento de la operación
```

`OperationLogId` se **autogenera** (`UUID.randomUUID()`), a diferencia del resto de identificadores
del dominio, que los aporta quien invoca el caso de uso (RD-POUT-04). La razón está en RD-AUD-02:
es un hecho técnico que ningún caso de uso referencia por identificador, así que exigir que el
invocador lo construya obligaría a añadir un parámetro a seis firmas de servicios que no tienen
nada que ver con auditoría.

---

# 1. Register Operation Log

**Clase:** `RegisterOperationLogService`

## Descripción

Registra una entrada de bitácora a partir del usuario que ejecutó la operación y la entidad que
resultó afectada.

## Entrada

```java
OperationLog register(User<?> actor,
                      OperationType operationType,
                      DomainEntity<?> affectedEntity,
                      String details)
```

| Parámetro | Descripción |
|---|---|
| `actor` | Usuario que ejecutó la operación; de él se extraen identificador y rol |
| `operationType` | Categoría de la operación, del catálogo cerrado de 4 valores |
| `affectedEntity` | Entidad sobre la que se actuó; de ella se extraen tipo e identificador |
| `details` | Descripción corta y legible de qué cambió |

## Dependencias

`OperationLogRepositoryPort`

**No inyecta servicios de autorización.** Es el único servicio del dominio que no valida rol ni
pertenencia, porque no es invocado directamente por ningún caso de uso: solo lo llaman otros
servicios de dominio que ya validaron al actor antes de ejecutar la operación que se está
registrando. Validar dos veces sería redundante y daría falsa sensación de ser un punto de entrada.

## Validaciones

Ninguna de negocio. Cualquier entrada inválida es un error de programación, señalado por los guardas
de `Objects.requireNonNull` en `OperationLog` y sus objetos de valor — **nunca** por una
`DomainException`. No existe regla de negocio que una bitácora pueda violar.

## Procesamiento

1. `OperationLogId.generate()` produce el identificador.
2. `OperationLog.register(...)` extrae identificador y rol del actor, y tipo e identificador de la
   entidad afectada.
3. `Instant.now()` fija el momento.
4. Persiste vía el puerto de salida.

## Excepciones

| Excepción | Causa |
|---|---|
| `NullPointerException` | Algún parámetro obligatorio es nulo (error de programación, no de negocio) |

---

## 4. Puntos de invocación

Seis servicios existentes reciben `RegisterOperationLogService` por constructor e invocan el
registro **después** de su propia persistencia, nunca antes: si el `save` falla, no debe quedar una
entrada de bitácora huérfana describiendo algo que no ocurrió.

| Servicio que registra | Tipo de operación | Entidad afectada |
|---|---|---|
| `RegisterSellerService` | `SELLER_REGISTERED` | `Seller` |
| `ConfirmPaymentService` | `ORDER_STATUS_CHANGED` | `Order` |
| `DispatchOrderService` | `ORDER_STATUS_CHANGED` | `Order` |
| `CompleteDeliveryService` | `ORDER_STATUS_CHANGED` | `Order` |
| `AdjustStockService` | `INVENTORY_ADJUSTED` | `InventoryItem` |
| `IssueRefundService` | `REFUND_ISSUED` | `Refund` |

Este conjunto es deliberadamente mínimo: cubre la incorporación de vendedores (la única alta
restringida al administrador), las tres transiciones del pedido (el proceso central del sistema), la
corrección manual de inventario (la única operación que altera existencias sin un movimiento de
negocio detrás) y la emisión de reembolsos (la única salida de dinero). Ampliarlo es una decisión
consciente, no algo que deba ocurrir por defecto al añadir servicios.

---

## 5. Trazabilidad servicio ↔ código

| Servicio | Clase Java | Puerto consumido |
|---|---|---|
| Register Operation Log | `RegisterOperationLogService` | `OperationLogRepositoryPort` |
