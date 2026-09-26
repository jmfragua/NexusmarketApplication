# Servicios de Usuario

**Paquete:** `application.domain.services.user`

## 1. Introducción

Este documento define los servicios del subdominio de **Usuario** (Dominio 1 de la especificación
funcional): la base de autenticación e identificación del marketplace. Cubren el alta de
compradores, el estado operativo de los usuarios, su rol y su identificación al operar.

---

## 2. Contexto del modelo de dominio

```text
DomainEntity<ID extends Identifier>
      │
      └── User<ID>  (abstracta)
             │
             ├── fullName         : FullName
             ├── email            : EmailAddress
             ├── identityDocument : IdentityDocument
             ├── role             : UserRole
             ├── status           : UserStatus
             │
             ├── Buyer   (User<BuyerId>)
             │      ├── primaryAddress      : Address
             │      ├── additionalAddresses : List<Address>
             │      └── commercialStatus    : BuyerCommercialStatus
             │
             └── Seller  (User<SellerId>)
```

### 2.1 Los cinco roles, dos especializaciones

La especificación define cinco participantes, pero sólo **Comprador** y **Vendedor** reciben
atributos propios. Los roles `LOGISTICS_OPERATOR`, `ADMINISTRATOR` y `SUPERVISOR` no aportan
atributos adicionales: se representan mediante el objeto de valor `UserRole` sobre `User`, sin clase
propia (RD-ALC-03).

---

## 3. Objetos de valor utilizados

```text
User
 ├── fullName         : FullName           ── obligatorio, no vacío
 ├── email            : EmailAddress       ── formato válido, único en la plataforma
 ├── identityDocument : IdentityDocument   ── no vacío, único en la plataforma
 ├── role             : UserRole           ── obligatorio, único por usuario
 └── status           : UserStatus         ── ACTIVE | BLOCKED
```

### 3.1 La frontera entre formato y unicidad

Esta distinción gobierna todo el subdominio (RD-VO-09):

| Restricción | Dónde se valida |
|---|---|
| El correo tiene formato válido | `EmailAddress`, en construcción |
| El documento no está en blanco | `IdentityDocument`, en construcción |
| El correo es **único** en la plataforma | `RegisterBuyerService` / `RegisterSellerService` |
| El documento es **único** en la plataforma | `RegisterBuyerService` / `RegisterSellerService` |

La unicidad depende del conjunto completo de datos y por eso no cabe dentro de un objeto de valor
inmutable: se verifica al incorporar el usuario al dominio.

### 3.2 Ciclo de vida de `UserStatus`

```text
    ┌──────────┐    block()     ┌───────────┐
    │  ACTIVE  │───────────────▶│  BLOCKED  │
    └──────────┘◀───────────────└───────────┘
                   activate()
```

Sólo un usuario `ACTIVE` puede ejecutar operaciones (RG-01). `User.changeStatus` es idempotente:
aplicar el estado que ya se tiene retorna sin cambios.

---

# 1. Register Buyer

**Clase:** `RegisterBuyerService`

## Descripción

Incorpora un comprador a la plataforma. A diferencia del vendedor, el comprador **sí puede
autoregistrarse**: se expone a través de `PublicAccessPort`.

## Entrada

```java
Buyer register(Buyer buyer)
```

El comprador llega ya construido y validado por el modelo: `FullName`, `EmailAddress`,
`IdentityDocument`, dirección principal y estado comercial ya garantizaron su validez individual.

## Dependencias

`UserRepositoryPort`

**No inyecta validación de rol**: es una operación pública, sin actor autenticado previo.

## Validaciones

1. **Unicidad del correo**: `existsByEmail` → `DuplicateEmailException` (RD-ID-03).
2. **Unicidad del documento**: `existsByIdentityDocument` → `DuplicateIdentityDocumentException`
   (RD-ID-04).

## Procesamiento

Persiste el comprador con `UserRepositoryPort.save`.

## Excepciones

| Excepción | Causa |
|---|---|
| `DuplicateEmailException` | El correo ya está registrado |
| `DuplicateIdentityDocumentException` | El documento ya está registrado |

---

# 2. Block User

**Clase:** `BlockUserService`

## Descripción

Bloquea un usuario, que deja de poder ejecutar operaciones.

## Entrada

```java
User<?> block(User<?> actor, User<?> target)
```

## Dependencias

`UserRepositoryPort`, `ValidateRoleAuthorizationService`

## Validaciones

1. **Actor activo** (RG-01) y rol `ADMINISTRATOR` (RD-ROL-06).
2. **Transición**: `UserStatus.canTransitionTo` valida el cambio; bloquear a alguien ya bloqueado
   retorna sin cambios.

## Efecto

A partir del bloqueo, `target.isActive()` devuelve `false` y **toda** operación del sistema le será
rechazada con `UserNotActiveException`, porque `ValidateUserActiveStatusService` es la primera
verificación de cada servicio.

## Excepciones

| Excepción | Causa |
|---|---|
| `UnauthorizedOperationException` | El actor no es administrador |
| `UserNotActiveException` | El propio administrador está bloqueado |

---

# 3. Activate User

**Clase:** `ActivateUserService`

## Descripción

Reactiva un usuario bloqueado.

## Entrada

```java
User<?> activate(User<?> actor, User<?> target)
```

## Validaciones

1. Actor activo, rol `ADMINISTRATOR`.
2. Transición `BLOCKED → ACTIVE`, idempotente si ya estaba activo.

## Nota sobre el estado comercial del comprador

Reactivar a un comprador **no** lo habilita automáticamente para comprar: `UserStatus` y
`BuyerCommercialStatus` son independientes. `Buyer.canPurchase()` exige ambos, de modo que un
comprador reactivado pero con estado comercial `DISABLED` sigue sin poder confirmar pedidos.

## Excepciones

| Excepción | Causa |
|---|---|
| `UnauthorizedOperationException` | El actor no es administrador |

---

# 4. Change User Role

**Clase:** `ChangeUserRoleService`

## Descripción

Reasigna el rol de un usuario.

## Entrada

```java
User<?> changeRole(User<?> actor, User<?> target, UserRole newRole)
```

## Validaciones

1. Actor activo, rol `ADMINISTRATOR`.
2. `newRole` no puede ser nulo: el rol es obligatorio (RG-02).

## Regla de unicidad del rol

El rol sigue siendo **único por usuario** porque el atributo admite un solo valor: no existe una
colección de roles. Cambiar el rol sustituye el anterior; no acumula (RD-ROL-02).

## Excepciones

| Excepción | Causa |
|---|---|
| `UnauthorizedOperationException` | El actor no es administrador |
| `NullPointerException` | El rol es nulo (invariante del modelo) |

---

# 5. Login

**Clase:** `LoginService`

## Descripción

Identifica al usuario que va a operar y garantiza que puede hacerlo.

## Entrada

```java
User<?> login(EmailAddress email)
```

## Dependencias

`UserRepositoryPort`, `ValidateUserActiveStatusService`

## Validaciones

1. **Existencia**: se resuelve el usuario por su correo; si no existe →
   `EntityNotFoundException`.
2. **Estado activo**: `ValidateUserActiveStatusService` → si está bloqueado,
   `UserNotActiveException` (RG-01).

## Alcance deliberadamente limitado

La especificación deja los **mecanismos de autenticación técnica explícitamente fuera del dominio**
(RD-ALC-01). Por eso este servicio:

- **No** verifica contraseñas.
- **No** emite tokens JWT.
- **No** gestiona sesiones.

Todo eso corresponde al adaptador de seguridad (`infrastructure/security/`), que se implementará en
una etapa posterior. Lo que sí es del dominio es resolver *quién* es el usuario y si *puede operar*,
que es exactamente lo que este servicio hace.

Por la misma razón, `domain/ports/out/` no declara `JwtServicePort` ni `PasswordServicePort` en esta
etapa.

## Excepciones

| Excepción | Causa |
|---|---|
| `EntityNotFoundException` | No hay usuario registrado con ese correo |
| `UserNotActiveException` | El usuario está bloqueado |

---

## 4. Trazabilidad servicio ↔ código

| Servicio | Clase Java | Rol requerido |
|---|---|---|
| Register Buyer | `RegisterBuyerService` | — (público) |
| Block User | `BlockUserService` | `ADMINISTRATOR` |
| Activate User | `ActivateUserService` | `ADMINISTRATOR` |
| Change User Role | `ChangeUserRoleService` | `ADMINISTRATOR` |
| Login | `LoginService` | — (público) |

El alta de vendedores no vive aquí: es una operación del administrador documentada en
[seller-services.md](seller-services.md), porque exige la primera bodega (RD-ROL-05).
