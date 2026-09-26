# Servicios de Vendedor

**Paquete:** `application.domain.services.seller`

## 1. Introducción

Este documento define los servicios del subdominio de **Vendedor** (Dominio 3): la incorporación de
vendedores a la plataforma, que es la puerta de entrada de todo el catálogo.

La regla que gobierna el subdominio es que **los vendedores no pueden autoregistrarse**: son
incorporados por el administrador, y siempre junto con su primera bodega (RD-ROL-05).

---

## 2. Contexto del modelo de dominio

```text
User<SellerId>
      │
      └── Seller
             │
             ├── fullName         : FullName
             ├── email            : EmailAddress
             ├── identityDocument : IdentityDocument
             ├── role             : UserRole  ── fijo en SELLER
             └── status           : UserStatus

Warehouse
      │
      └── SellerWarehouse
             └── sellerId : SellerId
```

### 2.1 Constructor privado como garantía estructural

`Seller` tiene **constructor privado**. La única forma de crear uno es la fábrica:

```java
Seller.register(identifier, fullName, email, identityDocument,
                status, firstWarehouse, administrator)
```

que exige obligatoriamente un administrador activo y la primera bodega. Esto hace RD-ROL-05
imposible de violar: no existe ninguna vía de código para producir un vendedor autoregistrado o sin
bodega.

---

# 1. Register Seller

**Clase:** `RegisterSellerService`

## Descripción

Incorpora un vendedor junto con su primera bodega.

## Entrada

```java
Seller register(User<?> actor,
                SellerId sellerId,
                FullName fullName,
                EmailAddress email,
                IdentityDocument identityDocument,
                UserStatus status,
                SellerWarehouse firstWarehouse)
```

### Por qué recibe objetos de valor sueltos y no un `Seller`

A diferencia de `RegisterBuyerService`, que recibe el `Buyer` ya construido, aquí no es posible: el
constructor de `Seller` es privado y la fábrica exige el administrador. El servicio recibe los
objetos de valor y delega la construcción en el modelo, que es donde vive la regla.

## Dependencias

`SellerRepositoryPort`, `WarehouseRepositoryPort`, `UserRepositoryPort`,
`ValidateRoleAuthorizationService`

## Validaciones

| # | Validación | Dónde | Excepción |
|---|---|---|---|
| 1 | Actor activo | Servicio (RG-01) | `UserNotActiveException` |
| 2 | Rol `ADMINISTRATOR` | Servicio (RD-ROL-06) | `UnauthorizedOperationException` |
| 3 | Correo único en la plataforma | Servicio (RD-ID-03) | `DuplicateEmailException` |
| 4 | Documento único en la plataforma | Servicio (RD-ID-04) | `DuplicateIdentityDocumentException` |
| 5 | El actor es administrador | `Seller.register` (RD-ROL-05) | `IllegalStateException` |
| 6 | El administrador está activo | `Seller.register` | `IllegalStateException` |
| 7 | La bodega pertenece a este vendedor | `Seller.register` | `IllegalArgumentException` |

Las validaciones 5-7 son la defensa del modelo, deliberadamente redundante con 1-2.

## Procesamiento

1. Valida rol y unicidad.
2. `Seller.register(...)` construye el vendedor tras revalidar sus precondiciones.
3. Persiste **la bodega primero**, luego el vendedor.

## Salida

El vendedor incorporado, con rol fijo `SELLER` y su primera bodega ya registrada.

## Excepciones

| Excepción | Causa |
|---|---|
| `UnauthorizedOperationException` | El actor no es administrador |
| `DuplicateEmailException` | El correo ya está registrado |
| `DuplicateIdentityDocumentException` | El documento ya está registrado |
| `IllegalArgumentException` | La primera bodega pertenece a otro vendedor |
| `IllegalStateException` | El administrador está bloqueado o no tiene rol de administrador |

---

# 2. Consult Seller

**Clase:** `ConsultSellerService`

## Descripción

Consulta los vendedores de la plataforma.

## Entrada

```java
Seller       consult(User<?> actor, SellerId sellerId);
List<Seller> consultAll(User<?> actor);
```

## Dependencias

`SellerRepositoryPort`, `ValidateRoleAuthorizationService`

## Validaciones

1. Actor activo (RG-01).
2. Rol `ADMINISTRATOR` o `SUPERVISOR` (RD-ROL-06). El administrador administra vendedores; el
   supervisor los consulta como perfil de seguimiento operativo.
3. Existencia del vendedor en `consult`.

## Salida

| Operación | Salida |
|---|---|
| `consult` | El vendedor con ese identificador |
| `consultAll` | Todos los vendedores de la plataforma |

## Excepciones

| Excepción | Causa |
|---|---|
| `EntityNotFoundException` | No existe vendedor con ese identificador |
| `UnauthorizedOperationException` | El rol no es `ADMINISTRATOR` ni `SUPERVISOR` |
| `UserNotActiveException` | El actor está bloqueado |

---

## 3. Trazabilidad servicio ↔ código

| Servicio | Clase Java | Rol requerido |
|---|---|---|
| Register Seller | `RegisterSellerService` | `ADMINISTRATOR` |
| Consult Seller | `ConsultSellerService` | `ADMINISTRATOR`, `SUPERVISOR` |

La administración del catálogo del vendedor se documenta en
[catalog-services.md](catalog-services.md), y sus bodegas en
[warehouse-services.md](warehouse-services.md).
