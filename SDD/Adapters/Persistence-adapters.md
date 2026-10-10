# Adaptadores de Persistencia — NexusMarket

**Paquetes:** `application.adapters.persistence.jpa`, `application.adapters.persistence.mongodb`

## 1. Introducción

Los adaptadores de persistencia implementan los trece puertos de salida del dominio. Son los únicos
que conocen MySQL y MongoDB; el dominio nunca sabe que existen (RD-ALC-02).

**Reparto de tecnología:** doce agregados de negocio en la base relacional, y uno —la bitácora de
auditoría— en la documental. La bitácora es append-only, no mantiene relaciones y lleva un campo de
texto libre: exactamente el perfil que justifica un almacén documental en lugar de una tabla.

---

## 2. Patrón de cuatro archivos por agregado

Cada agregado relacional se compone de:

```text
<Agregado>JpaAdapter          @Repository, implementa el puerto de salida
entities/<Agregado>JpaEntity  @Entity, columnas planas
mappers/<Agregado>JpaMapper   clase final estática, dominio ↔ entidad
repositories/SpringDataJpa<Agregado>Repository   JpaRepository + queries derivadas
```

### 2.1 Ejemplo trabajado: `Order`

**Entidad.** Columnas planas; los value objects se descomponen (`Money` → importe + moneda) y los
enums se guardan por su `code()`.

```java
@Entity @Table(name = "orders")
public class OrderJpaEntity {
    @Id String id;
    String buyerId;          // referencia ENTRE agregados: columna plana, sin relación
    String status;
    String deliveryAddress;
    BigDecimal totalAmount;
    String currency;

    @OneToMany(mappedBy = "order", cascade = ALL, orphanRemoval = true)
    List<OrderLineJpaEntity> lines;   // composición DENTRO del agregado: relación real
}
```

**Mapper.** Reconstruye con la fábrica, no con el constructor:

```java
public static Order toDomain(OrderJpaEntity entity) {
    List<OrderLine> lines = entity.getLines().stream().map(OrderJpaMapper::toLineDomain).toList();
    return Order.reconstruct(
            OrderId.of(entity.getId()),
            BuyerId.of(entity.getBuyerId()),
            Address.of(entity.getDeliveryAddress()),
            lines,
            OrderStatus.valueOf(entity.getStatus()));
}
```

**Adaptador.** Delegación directa, una línea por método del puerto.

---

## 3. Reglas de diseño de la persistencia

- **RD-PER-01.** Relaciones JPA reales **dentro** de un agregado (`Order↔OrderLine`,
  `Cart↔CartItem`, `Product↔ProductVariant`, `InventoryItem↔InventoryMovement`); columnas de
  identificador planas **entre** agregados (`Order.buyerId`, `InventoryItem.productId`). Así un
  repositorio nunca hace cascada hacia otro agregado, pero la composición interna no hay que
  reinventarla a mano.
- **RD-PER-02.** Los adaptadores devuelven **el mismo agregado que recibieron** en `save`, no una
  relectura. El agregado ya trae su identidad, y reconstruirlo desde la fila perdería el subtipo
  concreto que el invocador entregó.
- **RD-PER-03.** Los mappers son clases `final` con constructor privado y métodos estáticos: no
  tienen estado y no deben instanciarse.
- **RD-PER-04.** Los enumerados se persisten por su `code()` (`String`), nunca por su `ordinal()`:
  reordenar el enum no debe corromper los datos guardados.
- **RD-PER-05.** La reconstrucción usa las fábricas `reconstruct(...)` donde existan (RD-ALC-04),
  jamás las operaciones de negocio.

---

## 4. Catálogo de adaptadores relacionales

| Adaptador | Puerto implementado | Particularidad |
|---|---|---|
| `UserJpaAdapter` | `UserRepositoryPort` | Tabla única con discriminador `user_type` |
| `SellerJpaAdapter` | `SellerRepositoryPort` | **Comparte la tabla `users`**, filtrada por tipo |
| `WarehouseJpaAdapter` | `WarehouseRepositoryPort` | Discriminador `warehouse_type`, `seller_id` nulo para bodegas del marketplace |
| `ProductJpaAdapter` | `ProductRepositoryPort` | Discriminador `product_type`; `findVisibleInCatalog` filtra solo por estado |
| `InventoryItemJpaAdapter` | `InventoryItemRepositoryPort` | Constraint único sobre (producto, bodega) |
| `InventoryMovementJpaAdapter` | `InventoryMovementRepositoryPort` | `save` no persiste: los movimientos llegan en cascada |
| `CartJpaAdapter` | `CartRepositoryPort` | — |
| `OrderJpaAdapter` | `OrderRepositoryPort` | — |
| `InvoiceJpaAdapter` | `InvoiceRepositoryPort` | `order_id` único: un pedido se factura una vez |
| `ShipmentJpaAdapter` | `ShipmentRepositoryPort` | Sin columna de estado (RD-ALC-03) |
| `ProductReturnJpaAdapter` | `ProductReturnRepositoryPort` | — |
| `RefundJpaAdapter` | `RefundRepositoryPort` | `product_return_id` único: relación 1 a 1 |

### 4.1 Por qué vendedor y usuario comparten tabla

Un vendedor **es** un usuario en el modelo (`Seller extends User<SellerId>`). Guardarlo en una
tabla propia duplicaría las columnas de identidad y abriría la puerta a que las dos copias se
desincronizaran. `SellerJpaAdapter` consulta la misma tabla filtrando `user_type = 'SELLER'`.

### 4.2 Por qué `InventoryMovementJpaAdapter.save` no escribe

Los movimientos pertenecen al agregado `InventoryItem` y se persisten en cascada cuando se guarda
el registro de existencias. Escribirlos de nuevo desde su propio adaptador produciría duplicados.
El adaptador existe por sus **consultas**, que son las que el dominio necesita sobre la
trazabilidad (RD-INV-04).

### 4.3 La mitad de la visibilidad que el repositorio no puede filtrar

`findVisibleInCatalog()` filtra únicamente por `status = PUBLISHED`. La segunda condición de
visibilidad —tener al menos una variante (RD-CAT-05)— la aplica `ConsultCatalogService` con
`Product.isVisibleInCatalog()`. La regla se queda en el dominio en lugar de duplicarse como una
query, a costa de traer alguna fila de más.

---

## 5. Adaptador documental: `OperationLog`

```text
OperationLogMongoAdapter                 @Repository, implementa OperationLogRepositoryPort
documents/OperationLogDocument           @Document(collection = "operation_logs")
mappers/OperationLogMongoMapper          dominio ↔ documento
repositories/OperationLogMongoRepository MongoRepository
```

El puerto **no declara operaciones de actualización ni de borrado**: un hecho de auditoría no se
modifica una vez registrado.

### 5.1 La reconstrucción del identificador del actor

`OperationLog.actorId` es de tipo `Identifier`, cuya igualdad compara **valor y clase concreta**
(RD-VO-03). El mapper reconstruye la especialización que el rol almacenado implica:

```java
private static Identifier actorIdentifierOf(UserRole actorRole, String actorId) {
    return switch (actorRole) {
        case BUYER -> BuyerId.of(actorId);
        case SELLER -> SellerId.of(actorId);
        case LOGISTICS_OPERATOR, ADMINISTRATOR, SUPERVISOR -> UserId.of(actorId);
    };
}
```

Devolver un identificador genérico compilaría, pero `findByActor(buyerId)` no encontraría nunca
nada: el tipo concreto es parte de la identidad.

---

## 6. Configuración

Declarada en `application.properties` mediante marcadores `${VAR:valor}` para que el mismo artefacto
sirva en local y en contenedor:

| Propiedad | Defecto local |
|---|---|
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/nexusmarket` |
| `spring.jpa.hibernate.ddl-auto` | `update` (auto-generación del esquema) |
| `spring.jpa.open-in-view` | `false` |
| `spring.mongodb.uri` | `mongodb://localhost:27017/nexusmarket_logs` |

`docker-compose.yml` en la raíz levanta MySQL 8.4, MongoDB 7 y la aplicación, con *healthchecks* y
`depends_on: condition: service_healthy`.

### 6.1 Cuidado con el nombre de la propiedad de Mongo

En Spring Boot 4 la propiedad es **`spring.mongodb.uri`**, no `spring.data.mongodb.uri`. La segunda
se ignora **en silencio**: la aplicación arranca, los repositorios funcionan y los documentos se
escriben… en la base `test` por defecto. El fallo no produce error alguno, solo datos en el lugar
equivocado, y únicamente se detecta inspeccionando MongoDB.

---

## 7. Verificación

`PersistenceReconstructionTest` cubre los cuatro caminos donde un mapper ingenuo corrompería datos
en silencio:

| Caso | Qué verifica |
|---|---|
| Pedido despachado | Vuelve despachado, no reiniciado a `PENDING_PAYMENT` |
| Pedido entregado | Vuelve inmodificable (RD-PED-02) |
| Producto descontinuado | Vuelve descontinuado y con sus variantes |
| Vendedor | Se recarga sin exigir administrador ni primera bodega |
| Registro de existencias | Conserva saldos y condición **sin fabricar movimientos** |
| Par (producto, bodega) | Resuelve el registro único (RD-ID-05) |
