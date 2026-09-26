# Servicios de Catálogo

**Paquete:** `application.domain.services.catalog`

## 1. Introducción

Este documento define los servicios del subdominio de **Catálogo** (Dominio 5 de la especificación
funcional): el registro y administración de los productos ofrecidos en la plataforma, sus variantes
y su ciclo de vida.

Dos reglas estructuran todo el subdominio:

- **Todo producto pertenece a un único vendedor y solo ese vendedor lo administra** (RD-CAT-01).
- **El tipo de producto —físico o digital— se fija al registrarlo y no cambia durante su vida**
  (RD-CAT-02), porque determina el comportamiento del producto en todo el resto del dominio.

---

## 2. Contexto del modelo de dominio

```text
DomainEntity<ProductId>
      │
      └── Product  (abstracta)
             │
             ├── sellerId    : SellerId
             ├── productType : ProductType
             ├── status      : ProductStatus
             ├── variants    : List<ProductVariant>
             │
             ├── PhysicalProduct    requiresInventory() = true
             └── DigitalProduct     requiresInventory() = false

DomainEntity<ProductVariantId>
      │
      └── ProductVariant
             │
             ├── productId  : ProductId
             └── attributes : List<VariantAttribute>
```

### 2.1 El tipo como jerarquía, no como bandera

El tipo no se modela como un atributo mutable sino como **especialización de clase**. Un
`PhysicalProduct` nunca puede convertirse en `DigitalProduct`, porque son tipos distintos. Eso hace
RD-CAT-02 estructuralmente imposible de violar.

La diferencia de comportamiento se expresa con `requiresInventory()`:

| Tipo | `requiresInventory()` | Consecuencias |
|---|---|---|
| `PhysicalProduct` | `true` | Requiere existencias (RD-INV-01) y genera envío (RD-LOG-01) |
| `DigitalProduct` | `false` | No genera existencias (RD-INV-05) ni envío; entrega inmediata tras el pago |

---

## 3. Objetos de valor utilizados

```text
Product
 ├── productType : ProductType      ── PHYSICAL | DIGITAL   (fijo de por vida)
 ├── status      : ProductStatus    ── PUBLISHED | SUSPENDED | DISCONTINUED
 └── sellerId    : SellerId

ProductVariant
 └── attributes : List<VariantAttribute>
                    ├── name  : String   ── no vacío, no repetido en la variante
                    └── value : String   ── no vacío

Seller (actor)
 ├── status : UserStatus
 └── role   : UserRole  ── SELLER
```

### 3.1 Validación de `ProductStatus`

Los servicios nunca comparan el estado con cadenas:

```java
// Incorrecto
product.getStatus().equals("PUBLISHED");

// Correcto
product.isVisibleInCatalog();
product.getStatus().canTransitionTo(ProductStatus.SUSPENDED);
```

### 3.2 Validación de `VariantAttribute`

Una variante nunca declara dos veces la misma característica. `ProductVariant.addAttribute` lo
rechaza comparando por nombre:

```java
boolean duplicated = attributes.stream().anyMatch(d -> d.hasSameName(attribute));
```

Además, toda variante declara **al menos una** característica: una variante sin atributos no
distingue nada y se rechaza en construcción.

---

## 4. Ciclo de vida del producto

```text
        registerProduct()
                │
                ▼
        ┌───────────────┐
        │   PUBLISHED   │◀────────────┐   Visible en el catálogo público
        └───────┬───────┘             │
                │ suspend()           │ publish()
                ▼                     │
        ┌───────────────┐             │
        │   SUSPENDED   │─────────────┘   Temporalmente no comercializable
        └───────┬───────┘
                │ discontinue()
                ▼
        ┌────────────────┐
        │  DISCONTINUED  │  ── TERMINAL ──
        └────────────────┘
```

### 4.1 Transiciones permitidas

`ProductStatus.allowedTargets()` las declara de forma explícita (RD-VO-12):

| Desde | Hacia |
|---|---|
| `PUBLISHED` | `SUSPENDED` |
| `SUSPENDED` | `PUBLISHED`, `DISCONTINUED` |
| `DISCONTINUED` | — (conjunto vacío) |

Consecuencias que conviene subrayar:

- **No se puede descontinuar directamente un producto publicado**: hay que suspenderlo primero.
- **`DISCONTINUED` es terminal** (RD-CAT-04, RD-VO-13): no admite regreso a `PUBLISHED` ni a ningún
  otro estado.
- Un producto descontinuado tampoco admite nuevas variantes ni modificaciones de las existentes.

### 4.2 Estado inicial

`Product` nace en `PUBLISHED`. Sin embargo, no es visible en el catálogo público hasta que declare
al menos una variante, porque `isVisibleInCatalog()` exige ambas condiciones.

---

## 5. Visibilidad en el catálogo público

```java
public boolean isVisibleInCatalog() {
    return status.isVisibleInCatalog() && !variants.isEmpty();
}
```

| Condición | Regla |
|---|---|
| `status == PUBLISHED` | RD-CAT-03: sólo los productos publicados son visibles |
| `!variants.isEmpty()` | RD-CAT-05: las variantes son la unidad de selección comercial |

Esta doble condición es la que `AddCartItemService` verifica antes de permitir una selección.

---

## 6. Patrón estándar del subdominio

Los seis servicios de modificación siguen exactamente la misma secuencia:

1. **Validar el rol** `SELLER` y el estado activo del actor (RG-01).
2. **Validar la pertenencia del producto** con `ValidateSellerOwnershipService.validateProduct`,
   que delega en `Seller.ownsProduct(product)` (RD-CAT-01, RG-03).
3. **Invocar el comportamiento del modelo**, que valida la transición.
4. **Persistir** con `ProductRepositoryPort`.

Las transiciones se invocan a través del vendedor (`seller.publishProduct(product)`) y no
directamente sobre el producto, de modo que la verificación de propiedad queda además duplicada
dentro del propio modelo.

**Rol autorizado:** únicamente `SELLER`. El registro y la administración de productos son
responsabilidad exclusiva del vendedor (RD-ROL-06).

---

# 1. Register Product

**Clase:** `RegisterProductService`

## Descripción

Da de alta un producto en el catálogo propio del vendedor.

## Entrada

```java
Product register(Seller seller, Product product)
```

El producto llega ya construido como `PhysicalProduct` o `DigitalProduct`, con su `sellerId`
asignado. El tipo queda fijado en ese momento y no cambiará (RD-CAT-02).

## Dependencias

`ProductRepositoryPort`, `ValidateRoleAuthorizationService`, `ValidateSellerOwnershipService`

## Validación del usuario

- Activo (RG-01), rol `SELLER`.

## Validación de propiedad

Doble verificación deliberada:

1. `ValidateSellerOwnershipService.validateProduct` → `ProductNotOwnedException`.
2. `Seller.registerProduct(product)`, que vuelve a comprobar `ownsProduct` dentro del modelo.

El producto no puede registrarse bajo un vendedor distinto del declarado en su `sellerId`.

## Estado resultante

```text
status   : PUBLISHED
variants : vacío (todavía no visible en el catálogo público)
```

## Excepciones

| Excepción | Causa |
|---|---|
| `ProductNotOwnedException` | El `sellerId` del producto no es el del vendedor |
| `UnauthorizedOperationException` | El rol no es `SELLER` |
| `UserNotActiveException` | El vendedor está bloqueado |

---

# 2. Publish Product

**Clase:** `PublishProductService`

## Descripción

Publica un producto, haciéndolo visible en el catálogo público.

## Entrada

```java
Product publish(Seller seller, Product product)
```

## Validación de la transición

Sólo `SUSPENDED → PUBLISHED` está permitida. Intentar publicar un producto descontinuado se rechaza,
porque `DISCONTINUED` no tiene transiciones de salida (RD-CAT-04).

Publicar un producto que ya está publicado es una operación **idempotente**: `Product.changeStatus`
retorna sin cambios si el estado destino coincide con el actual.

## Efecto sobre la visibilidad

La publicación es condición necesaria pero **no suficiente**: el producto sólo aparecerá en el
catálogo público si además tiene al menos una variante (RD-CAT-05).

## Excepciones

| Excepción | Causa |
|---|---|
| `IllegalStateException` | El producto está descontinuado |
| `ProductNotOwnedException` | El producto es de otro vendedor |

---

# 3. Suspend Product

**Clase:** `SuspendProductService`

## Descripción

Retira temporalmente el producto de la comercialización.

## Entrada

```java
Product suspend(Seller seller, Product product)
```

## Validación de la transición

Sólo `PUBLISHED → SUSPENDED`. Un producto descontinuado no puede suspenderse.

## Efecto

El producto deja de ser visible en el catálogo público y no puede añadirse al carrito
(`AddCartItemService` lo rechazará con `ProductNotAvailableException`). La suspensión es
**reversible**: el producto puede volver a publicarse.

## Excepciones

| Excepción | Causa |
|---|---|
| `IllegalStateException` | El producto está descontinuado |
| `ProductNotOwnedException` | El producto es de otro vendedor |

---

# 4. Discontinue Product

**Clase:** `DiscontinueProductService`

## Descripción

Retira el producto de forma **definitiva** del catálogo.

## Entrada

```java
Product discontinue(Seller seller, Product product)
```

## Validación de la transición

Sólo `SUSPENDED → DISCONTINUED`. Un producto publicado **debe suspenderse primero**: el modelo no
permite el atajo `PUBLISHED → DISCONTINUED`.

## Consecuencias del estado terminal

Tras esta operación, de forma permanente:

- No hay transición de regreso a `PUBLISHED` ni a `SUSPENDED` (RD-CAT-04, RD-VO-13).
- `Product.addVariant` lanza `IllegalStateException`: un producto descontinuado no admite nuevas
  variantes.
- `Product.removeVariant` lanza `IllegalStateException`: no puede modificarse.

## Excepciones

| Excepción | Causa |
|---|---|
| `IllegalStateException` | El producto está publicado (falta suspenderlo) o ya descontinuado |
| `ProductNotOwnedException` | El producto es de otro vendedor |

---

# 5. Add Product Variant

**Clase:** `AddProductVariantService`

## Descripción

Declara una nueva diferencia del producto: color, talla, modelo.

## Entrada

```java
Product addVariant(Seller seller, Product product, ProductVariant variant)
```

## Validación de pertenencia de la variante

`Product.addVariant` verifica que la variante declare este producto como propietario:

```java
if (!variant.getProductId().equals(getIdentifier())) {
    throw new IllegalArgumentException("the variant belongs to another product");
}
```

## Validación del estado del producto

```java
if (status.isTerminal()) {
    throw new IllegalStateException("a discontinued product does not admit new variants");
}
```

## Comportamiento idempotente

Añadir una variante ya presente no la duplica: `addVariant` retorna sin cambios si la colección ya
la contiene.

## Importancia comercial

Este servicio es el que habilita la venta: un producto publicado pero sin variantes **no es visible
en el catálogo público** (RD-CAT-05). La primera variante es la que lo hace efectivamente
comercializable.

## Excepciones

| Excepción | Causa |
|---|---|
| `IllegalArgumentException` | La variante pertenece a otro producto |
| `IllegalStateException` | El producto está descontinuado |
| `ProductNotOwnedException` | El producto es de otro vendedor |

---

# 6. Remove Product Variant

**Clase:** `RemoveProductVariantService`

## Descripción

Retira una diferencia declarada del producto.

## Entrada

```java
Product removeVariant(Seller seller, Product product, ProductVariant variant)
```

## Validación del estado del producto

`Product.removeVariant` rechaza la operación si el producto está descontinuado: un producto en
estado terminal no puede modificarse (RD-CAT-04).

## Efecto colateral sobre la visibilidad

Retirar la **última** variante deja el producto sin variantes, y por tanto **fuera del catálogo
público** aunque su estado siga siendo `PUBLISHED` (RD-CAT-05). El producto no cambia de estado,
pero deja de ser comercializable.

## Excepciones

| Excepción | Causa |
|---|---|
| `IllegalStateException` | El producto está descontinuado |
| `ProductNotOwnedException` | El producto es de otro vendedor |

---

# 7. Consult Catalog

**Clase:** `ConsultCatalogService`

## Descripción

Consulta el catálogo, distinguiendo la vista pública de la vista privada del vendedor.

## Entrada

```java
List<Product> consultPublicCatalog();
Product       consultPublished(ProductId productId);
List<Product> consultOwnCatalog(Seller seller);
```

## Autorización por operación

| Operación | Autenticación | Alcance |
|---|---|---|
| `consultPublicCatalog` | **Ninguna** | Sólo productos visibles |
| `consultPublished` | **Ninguna** | Un producto visible |
| `consultOwnCatalog` | Rol `SELLER` | Todos los productos del vendedor, en cualquier estado |

### Por qué las dos primeras no exigen usuario autenticado

El catálogo es la cara pública del marketplace: un visitante debe poder verlo antes de registrarse.
Esta es la única excepción a RG-01 dentro del dominio, y es deliberada — se expone a través de
`PublicAccessPort`.

## Procesamiento

`consultPublicCatalog` aplica un **doble filtro**:

1. El repositorio devuelve los candidatos con `findVisibleInCatalog()`.
2. El servicio vuelve a filtrar en memoria con `Product::isVisibleInCatalog`.

El segundo filtro no es redundante: garantiza que la condición de "al menos una variante"
(RD-CAT-05) se cumpla aunque el adaptador de persistencia sólo haya filtrado por estado.

`consultPublished` lanza `ProductNotAvailableException` si el producto existe pero no es visible,
distinguiéndolo de `EntityNotFoundException` cuando no existe en absoluto.

## Excepciones

| Excepción | Causa |
|---|---|
| `EntityNotFoundException` | No existe producto con ese identificador |
| `ProductNotAvailableException` | El producto existe pero no es visible en el catálogo |
| `UnauthorizedOperationException` | El rol no es `SELLER` en `consultOwnCatalog` |

---

## 7. Trazabilidad servicio ↔ código

| Servicio | Clase Java | Transición / efecto |
|---|---|---|
| Register Product | `RegisterProductService` | Alta en `PUBLISHED` |
| Publish Product | `PublishProductService` | `SUSPENDED → PUBLISHED` |
| Suspend Product | `SuspendProductService` | `PUBLISHED → SUSPENDED` |
| Discontinue Product | `DiscontinueProductService` | `SUSPENDED → DISCONTINUED` (terminal) |
| Add Product Variant | `AddProductVariantService` | + 1 variante |
| Remove Product Variant | `RemoveProductVariantService` | − 1 variante |
| Consult Catalog | `ConsultCatalogService` | — (solo lectura) |
