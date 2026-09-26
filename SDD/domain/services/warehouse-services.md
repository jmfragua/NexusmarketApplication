# Servicios de Bodega

**Paquete:** `application.domain.services.warehouse`

## 1. Introducción

Este documento define los servicios del subdominio de **Bodega** (Dominio 4): los espacios físicos
de almacenamiento donde vive el inventario distribuido.

---

## 2. Contexto del modelo de dominio

```text
DomainEntity<WarehouseId>
      │
      └── Warehouse  (abstracta)
             │
             ├── type           : WarehouseType
             ├── inventoryItems : List<InventoryItem>
             │
             ├── MarketplaceWarehouse    type = MARKETPLACE, sin propietario
             └── SellerWarehouse         type = SELLER, sellerId obligatorio
```

### 2.1 La titularidad como jerarquía

El tipo de bodega no es una bandera mutable sino una especialización de clase, y se fija al crearla
(RD-VO-14). `MarketplaceWarehouse` **no tiene** atributo `sellerId`: es estructuralmente imposible
que pertenezca a un vendedor.

### 2.2 Unicidad del par producto–bodega

`Warehouse.addInventoryItem` rechaza un segundo registro de existencias para el mismo producto:

```java
if (stockOf(item.getProductId()).isPresent()) {
    throw new IllegalArgumentException("product already has stock in warehouse");
}
```

Es la aplicación de RD-ID-05 desde el lado de la bodega.

---

# 1. Register Marketplace Warehouse

**Clase:** `RegisterMarketplaceWarehouseService`

## Descripción

Registra una bodega perteneciente al marketplace, operada directamente por la plataforma.

## Entrada

```java
MarketplaceWarehouse register(User<?> actor, WarehouseId warehouseId)
```

## Dependencias

`WarehouseRepositoryPort`, `ValidateRoleAuthorizationService`

## Validaciones

1. Actor activo (RG-01).
2. Rol `ADMINISTRATOR`: la administración de bodegas de la plataforma le corresponde (RD-ROL-06).

## Procesamiento

Construye `MarketplaceWarehouse`, cuyo `WarehouseType` queda fijo en `MARKETPLACE`, y lo persiste.

## Alcance: por qué no registra bodegas de vendedor

Las `SellerWarehouse` **no** se crean aquí. La primera se registra junto con la incorporación del
vendedor, en `RegisterSellerService` (RD-ROL-05). Separarlas evita que exista una vía para crear un
vendedor sin bodega.

## Salida

La bodega registrada, sin existencias.

## Excepciones

| Excepción | Causa |
|---|---|
| `UnauthorizedOperationException` | El actor no es administrador |
| `UserNotActiveException` | El administrador está bloqueado |

---

# 2. Consult Warehouse

**Clase:** `ConsultWarehouseService`

## Descripción

Consulta las bodegas, delimitando qué rol alcanza cuáles.

## Entrada

```java
Warehouse             consult(User<?> actor, WarehouseId warehouseId);
Warehouse             consultOwn(Seller seller, WarehouseId warehouseId);
List<SellerWarehouse> consultOwn(Seller seller);
List<Warehouse>       consultAll(User<?> actor);
```

## Dependencias

`WarehouseRepositoryPort`, `ValidateRoleAuthorizationService`, `ValidateSellerOwnershipService`

## Autorización por operación

| Operación | Roles | Alcance |
|---|---|---|
| `consult` | `LOGISTICS_OPERATOR`, `ADMINISTRATOR`, `SUPERVISOR` | Cualquier bodega |
| `consultOwn(Seller, WarehouseId)` | `SELLER` | Sólo las propias |
| `consultOwn(Seller)` | `SELLER` | Todas las propias |
| `consultAll` | `LOGISTICS_OPERATOR`, `ADMINISTRATOR`, `SUPERVISOR` | Todas |

## Validaciones de `consultOwn(Seller, WarehouseId)`

1. Actor activo, rol `SELLER`.
2. Existencia de la bodega → `EntityNotFoundException`.
3. **Pertenencia**: `ValidateSellerOwnershipService.validateWarehouse` (RG-03).

El orden importa: se recupera primero y se valida pertenencia después, de modo que un vendedor que
pida la bodega de otro reciba `WarehouseNotOwnedException` y no información sobre bodegas ajenas.

Un vendedor nunca alcanza una `MarketplaceWarehouse`, porque `ownsWarehouse` exige que sea una
`SellerWarehouse` suya.

## Salida

| Operación | Salida |
|---|---|
| `consult` / `consultOwn(Seller, WarehouseId)` | Una bodega |
| `consultOwn(Seller)` | Lista de `SellerWarehouse` del vendedor |
| `consultAll` | Todas las bodegas |

`consultOwn(Seller)` devuelve `SellerWarehouse` y no `Warehouse` porque, por definición, un vendedor
sólo puede tener bodegas de ese tipo.

## Excepciones

| Excepción | Causa |
|---|---|
| `EntityNotFoundException` | No existe bodega con ese identificador |
| `WarehouseNotOwnedException` | La bodega pertenece a otro vendedor o al marketplace |
| `UnauthorizedOperationException` | El rol no consulta bodegas |

---

## 3. Trazabilidad servicio ↔ código

| Servicio | Clase Java | Rol requerido |
|---|---|---|
| Register Marketplace Warehouse | `RegisterMarketplaceWarehouseService` | `ADMINISTRATOR` |
| Consult Warehouse | `ConsultWarehouseService` | Según operación |

Las existencias que contienen estas bodegas se documentan en
[inventory-services.md](inventory-services.md).
