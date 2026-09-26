# Servicios de Autorización

**Paquete:** `application.domain.services.authorization`

## 1. Introducción

Este documento define los servicios del subdominio de **Autorización**, que centralizan las
verificaciones transversales exigidas por las reglas generales del negocio:

- **RG-01.** Toda operación debe ejecutarse por un usuario autenticado. Ninguna entidad admite
  modificaciones anónimas.
- **RG-02.** Cada usuario tiene un único rol dentro del sistema.
- **RG-03.** Ningún participante puede administrar información fuera de su rol.

Estos cuatro servicios son los únicos del dominio que **no acceden a ningún puerto de salida**: no
consultan ni persisten nada. Operan exclusivamente sobre los modelos que reciben.

Todos los demás servicios de `domain/services/` los inyectan.

---

## 2. Objetos de valor utilizados

```text
User (actor)
 ├── status : UserStatus   ── ACTIVE | BLOCKED
 └── role   : UserRole     ── BUYER | SELLER | LOGISTICS_OPERATOR |
                              ADMINISTRATOR | SUPERVISOR
```

Las verificaciones nunca comparan roles ni estados con cadenas:

```java
// Incorrecto
user.getRole().name().equals("ADMINISTRATOR");
user.getStatus().toString().equals("ACTIVE");

// Correcto
user.isActive();
user.getRole().canModifyBusinessData();
```

---

## 3. Matriz de responsabilidades (RD-ROL-06)

Es la fuente de verdad que decide qué rol puede ejecutar qué proceso:

| Proceso | Comprador | Vendedor | Op. Logístico | Administrador |
|---|:---:|:---:|:---:|:---:|
| Registro de vendedores | | | | ✔ |
| Registro de productos | | ✔ | | |
| Administración de inventario | | ✔ | ✔ | |
| Gestión de pedidos | ✔ | ✔ | ✔ | |
| Gestión de reembolsos | ✔ | | | ✔ |

El rol **Supervisor** es perfil de consulta y seguimiento operativo: no ejecuta procesos de
modificación.

---

## 4. Estrategia de defensa en dos capas

La autorización se aplica en dos niveles complementarios, deliberadamente redundantes:

| Capa | Qué verifica | Dónde vive |
|---|---|---|
| **Servicio** | Rol del actor y pertenencia del recurso | `domain/services/authorization/` |
| **Modelo** | Pertenencia, mediante `belongsTo`, `ownsProduct`, `ownsWarehouse` | `domain/models/` |

El modelo ya expone operaciones de verificación de pertenencia para hacer cumplible RG-03 desde el
propio dominio (RD-ROL-03). Los servicios de autorización las invocan y traducen el resultado a la
excepción de dominio correspondiente.

La redundancia es intencional: aunque un futuro adaptador olvidara pasar por el servicio, el modelo
seguiría rechazando la operación.

---

# 1. Validate User Active Status

**Clase:** `ValidateUserActiveStatusService`

## Descripción

Verifica que el actor de una operación está activo en la plataforma. Es la verificación más básica
del sistema y la primera que se ejecuta en prácticamente todos los servicios.

## Entrada

```java
void validate(User<?> actor)
```

## Dependencias

Ninguna.

## Validaciones

1. `actor` no puede ser nulo: ninguna entidad admite modificaciones anónimas (RG-01).
2. `actor.isActive()`, que delega en `UserStatus.canOperate()`.

## Excepciones

| Excepción | Causa |
|---|---|
| `NullPointerException` | El actor es nulo |
| `UserNotActiveException` | El usuario está bloqueado |

---

# 2. Validate Role Authorization

**Clase:** `ValidateRoleAuthorizationService`

## Descripción

Verifica que el rol del actor cubre la operación solicitada, según la matriz de responsabilidades.
Es el servicio más invocado del dominio: prácticamente todos los demás lo usan como primera línea.

## Entrada

```java
void validate(User<?> actor, String operation, UserRole... allowedRoles);
void validateCanModify(User<?> actor, String operation);
```

| Parámetro | Descripción |
|---|---|
| `operation` | Nombre del proceso, reportado en el mensaje de la excepción |
| `allowedRoles` | Roles que la matriz autoriza para ese proceso |

Cada servicio declara su nombre de operación como constante:

```java
private static final String OPERATION = "reserve stock";
```

Esto hace que el mensaje de error identifique con precisión qué proceso fue rechazado.

## Dependencias

`ValidateUserActiveStatusService`

## Validaciones

1. **Estado activo** primero, delegando en `ValidateUserActiveStatusService` (RG-01). El orden
   importa: un usuario bloqueado es rechazado por bloqueo, no por rol.
2. **Pertenencia al conjunto de roles permitidos** (RG-03, RD-ROL-06).

## Variante `validateCanModify`

Atiende el caso particular del supervisor:

```java
if (!actor.getRole().canModifyBusinessData()) {
    throw new UnauthorizedOperationException(actor.getRole(), operation);
}
```

`UserRole.canModifyBusinessData()` devuelve `false` únicamente para `SUPERVISOR`. Permite rechazar
cualquier intento de modificación por parte del perfil de consulta sin tener que enumerar los roles
permitidos.

## Excepciones

| Excepción | Causa |
|---|---|
| `UserNotActiveException` | El actor está bloqueado |
| `UnauthorizedOperationException` | El rol no está entre los permitidos para esa operación |

---

# 3. Validate Buyer Ownership

**Clase:** `ValidateBuyerOwnershipService`

## Descripción

Verifica que el carrito, pedido o factura sobre el que actúa el comprador es suyo. Implementa
RD-ROL-04: el comprador **nunca** administrará información de otros compradores.

## Entrada

```java
void validateCart(Buyer buyer, Cart cart);
void validateOrder(Buyer buyer, Order order);
void validateInvoice(Buyer buyer, Invoice invoice);
```

## Dependencias

Ninguna.

## Validaciones

Cada método delega en el comportamiento ya encapsulado en el modelo:

| Método | Delega en | Regla |
|---|---|---|
| `validateCart` | `Cart.belongsTo(buyer)` | RD-ROL-04 |
| `validateOrder` | `Order.belongsTo(buyer)` | RD-PED-05 |
| `validateInvoice` | `Invoice.belongsTo(buyer)` | RD-ROL-04 |

El servicio no reimplementa la comparación de identificadores: el modelo ya sabe a quién pertenece
cada cosa.

## Orden de invocación recomendado

Los servicios consultores recuperan primero la entidad y validan la pertenencia **después**, de modo
que un comprador que solicite un recurso ajeno reciba `UnauthorizedOperationException` en lugar de
una pista sobre su existencia.

## Excepciones

| Excepción | Causa |
|---|---|
| `UnauthorizedOperationException` | El recurso pertenece a otro comprador |
| `NullPointerException` | El comprador o el recurso son nulos |

---

# 4. Validate Seller Ownership

**Clase:** `ValidateSellerOwnershipService`

## Descripción

Verifica que el producto o la bodega sobre la que actúa el vendedor es suya.

## Entrada

```java
void validateProduct(Seller seller, Product product);
void validateWarehouse(Seller seller, Warehouse warehouse);
```

## Dependencias

Ninguna.

## Validaciones

| Método | Delega en | Regla | Excepción |
|---|---|---|---|
| `validateProduct` | `Seller.ownsProduct(product)` | RD-CAT-01 | `ProductNotOwnedException` |
| `validateWarehouse` | `Seller.ownsWarehouse(warehouse)` | RG-03 | `WarehouseNotOwnedException` |

## Nota sobre las bodegas del marketplace

`Seller.ownsWarehouse` sólo acepta instancias de `SellerWarehouse`:

```java
return warehouse instanceof SellerWarehouse sw && sw.belongsTo(this);
```

Una `MarketplaceWarehouse` nunca pertenece a un vendedor, porque no tiene `sellerId`. La
verificación de tipo es la que hace estructuralmente imposible que un vendedor opere sobre una
bodega de la plataforma.

## Excepciones específicas, no genéricas

Este servicio usa dos excepciones distintas en lugar de una `UnauthorizedOperationException`
genérica, porque identifican con precisión qué recurso fue denegado — lo que facilita el diagnóstico
y la trazabilidad de RG-03.

---

## 5. Trazabilidad servicio ↔ código

| Servicio | Clase Java | Regla principal | Puertos de salida |
|---|---|---|---|
| Validate User Active Status | `ValidateUserActiveStatusService` | RG-01 | ninguno |
| Validate Role Authorization | `ValidateRoleAuthorizationService` | RG-02, RG-03, RD-ROL-06 | ninguno |
| Validate Buyer Ownership | `ValidateBuyerOwnershipService` | RD-ROL-04, RD-PED-05 | ninguno |
| Validate Seller Ownership | `ValidateSellerOwnershipService` | RD-CAT-01, RG-03 | ninguno |
