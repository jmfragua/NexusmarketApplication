# Adaptadores REST — NexusMarket

**Paquete:** `application.adapters.rest`

## 1. Introducción

La capa REST es el mecanismo de entrega: traduce HTTP a modelos de dominio, invoca el puerto de
entrada del rol correspondiente, y traduce el resultado de vuelta a JSON.

```text
adapters/rest/
├── controllers/        7 controladores
├── dtos/requests/     18 DTOs de entrada
├── dtos/responses/    19 DTOs de salida (18 de recurso + PageResponse)
├── mappers/            6 mappers estáticos
└── exception/          GlobalExceptionHandler
```

---

## 2. Reglas de diseño

- **RD-REST-01.** Un controlador por **rol**, no por entidad, en correspondencia uno a uno con los
  puertos de entrada. `AuthRestController` y `CatalogRestController` son la excepción: cubren el
  puerto público, que no corresponde a ningún rol autenticado.
- **RD-REST-02.** Los controladores inyectan **puertos de entrada**, nunca servicios de dominio,
  casos de uso concretos ni puertos de salida. Sin excepciones: la única que hubo quedó saldada
  (§4.2). `AdministratorRestController` inyecta además `CredentialService`, que no es dominio sino
  infraestructura de seguridad y no tiene puerto que implementar (ver `SDD/Infrastructure/Security.md`).
- **RD-REST-03.** Los controladores **no capturan excepciones**. Las dejan subir hasta
  `GlobalExceptionHandler`, que concentra en una sola tabla la traducción regla → código HTTP.
- **RD-REST-04.** Los DTOs son `record` inmutables con validación `jakarta.validation`. Esa
  validación solo guarda la **forma** de la petición; las reglas reales las revalidan los objetos de
  valor y los servicios de dominio (RD-VO-07).
- **RD-REST-05.** El actor autenticado llega como `@AuthenticationPrincipal DomainUserPrincipal`,
  que transporta el `User` de dominio real, no un principal del framework.
- **RD-REST-06.** Todo endpoint de consulta que devuelva una colección responde paginado, con el
  envoltorio `PageResponse<T>` (§5). La paginación no baja de esta capa.

---

## 3. Catálogo de controladores

| Controlador | Ruta base | Puerto | Autenticación |
|---|---|---|---|
| `AuthRestController` | `/api/v1/auth` | `PublicAccessPort` | Abierta |
| `CatalogRestController` | `/api/v1/catalog` | `PublicAccessPort` | Abierta |
| `BuyerRestController` | `/api/v1/buyer` | `BuyerPort` | Rol `BUYER` |
| `SellerRestController` | `/api/v1/seller` | `SellerPort` | Rol `SELLER` |
| `LogisticOperatorRestController` | `/api/v1/logistics` | `LogisticOperatorPort` | Rol `LOGISTICS_OPERATOR` |
| `AdministratorRestController` | `/api/v1/admin` | `AdministratorPort` | Rol `ADMINISTRATOR` |
| `SupervisorRestController` | `/api/v1/supervisor` | `SupervisorPort` | Rol `SUPERVISOR` |

`SupervisorRestController` expone **únicamente verbos `GET`**. El supervisor es perfil de consulta y
nunca ejecuta procesos de modificación (RD-ROL-06): la ausencia de verbos de escritura es esa regla
hecha visible en la superficie del API.

---

## 4. El problema de resolver entidades desde identificadores

Los puertos de entrada reciben **modelos de dominio** (`Order`, `Product`, `Cart`), pero HTTP solo
trae identificadores. Alguien tiene que resolverlos, y dónde se haga tiene consecuencias de
seguridad.

### 4.1 Resolución a través del propio puerto (preferida)

`BuyerRestController` resuelve todo mediante las operaciones de consulta del mismo puerto:

```java
Order order = buyerPort.consultMyOrder(buyer, OrderId.of(orderId));
return ResponseEntity.ok(OrderRestMapper.toResponse(buyerPort.confirmPayment(buyer, order)));
```

Así la validación de pertenencia corre **al cargar**: un comprador que pida el pedido de otro es
rechazado mientras lo busca, no después (RD-PED-05).

### 4.2 Deuda saldada: ningún controlador inyecta puertos de salida

Durante una etapa anterior, `SellerRestController`, `LogisticOperatorRestController` y
`AdministratorRestController` inyectaban puertos de salida solo para resolver la entidad desde el
identificador de la URL. No abría un agujero de seguridad —cargar no decide nada, y el servicio de
dominio revalidaba rol y pertenencia después—, pero rompía RD-REST-02: el adaptador conocía la capa
de persistencia sin necesitarla.

La deuda se cerró ampliando los puertos de entrada con las consultas que faltaban, en lugar de
dejar que el controlador supliera la carencia:

| Puerto | Operaciones añadidas |
|---|---|
| `SellerPort` | `consultMyProduct`, `consultStockRecord` |
| `LogisticOperatorPort` | `consultOrder`, `consultStockRecord`, y variantes por identificador de `restockReturn`, `packShipment`, `dispatchShipment`, `confirmShipmentDelivery` |
| `AdministratorPort` | `consultUser`, `issueInvoice`, `consultInvoice` |

`SellerPort.consultMyProduct` es el caso que mejor muestra la ganancia: valida la pertenencia
**mientras carga**, así que un vendedor que pida el producto de otro es rechazado en la resolución y
no después. Lo mismo que ya hacía `BuyerRestController` en §4.1.

Las variantes por identificador de las operaciones de envío existen porque el controlador de
logística construía el `Shipment` cuando no existía y recopilaba los productos del pedido para
decidir si requería logística (RD-LOG-01). Ambas cosas son del caso de uso, no del transporte, y
ahora viven en `LogisticOperatorUseCaseImpl`.

**Verificación:** `grep -rn "RepositoryPort" src/main/java/application/adapters/rest/` no devuelve
nada, igual que `grep -rn "import application.domain.services" src/main/java/application/adapters/rest/`.

---

## 5. Paginación de los listados

Los 19 endpoints de consulta que devuelven colecciones responden con `PageResponse<T>`
(`content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`) y aceptan los parámetros
`page` (por omisión `0`) y `size` (por omisión `20`, máximo `200`).

**La paginación vive solo en el adaptador REST.** Los puertos de salida siguen devolviendo la
colección completa: el dominio no expresa "la página dos de los pedidos" como concepto, y bajar
parámetros de página hasta los repositorios metería una preocupación de transporte en el modelo
(RD-ALC-01).

La consecuencia se documenta en lugar de disimularse: **la consulta lee todas las filas que
coinciden antes de cortar la página**, así que esto acota el tamaño de la *respuesta*, no el trabajo
de la base de datos. Sirve para que un cliente no tenga que digerir una lista sin límite, que es su
propósito; no es una optimización de rendimiento. Un catálogo lo bastante grande como para
necesitarla exigiría puertos de salida paginados.

`page` negativo, `size` menor que 1 o `size` mayor que 200 se rechazan con
`IllegalArgumentException`, que `GlobalExceptionHandler` traduce a **400**. Una página más allá del
final no es un error: devuelve una página vacía con `last: true`, lo que permite a un cliente
avanzar sin conocer el total de antemano.

---

## 6. Traducción de excepciones a HTTP

`GlobalExceptionHandler` (`@RestControllerAdvice`) mantiene la tabla. El agrupamiento es deliberado:

| Código | Significado | Excepciones |
|---|---|---|
| **403** | El actor no puede hacer esto | `UnauthorizedOperationException`, `UserNotActiveException`, `ProductNotOwnedException`, `WarehouseNotOwnedException`, `BuyerNotAllowedToPurchaseException` |
| **404** | La entidad referida no existe | `EntityNotFoundException`, `InvoiceNotIssuedException` |
| **409** | Existe, pero su estado lo prohíbe | `OrderNotModifiableException`, `InvalidOrderTransitionException`, `InsufficientStockException`, `DamagedStockException`, `ProductNotAvailableException`, `ReturnNotAllowedException`, `EmptyCartException`, `DuplicateEmailException`, `DuplicateIdentityDocumentException`, `DuplicateInventoryItemException`, `InvalidRoleChangeException` |
| **400** | La petición es inconsistente | `RefundExceedsInvoiceException`, validación de DTOs, guardas de objetos de valor |

El cuerpo del error incluye el campo `rule` con el nombre de la excepción, lo que mantiene la regla
de negocio trazable desde la respuesta HTTP hasta el modelo:

```json
{ "status": 409, "rule": "InsufficientStockException", "message": "product ... has not enough stock: available 1, requested 2" }
```

Los `IllegalStateException` que lanza el propio modelo —una transición que la entidad rechaza— se
traducen a **409**: la petición estaba bien formada, el estado no.

---

## 7. Mappers

Seis clases finales con métodos estáticos, agrupadas por afinidad de dominio y no una por
controlador:

| Mapper | Traduce |
|---|---|
| `UserRestMapper` | `User`, `Buyer`, `Seller` |
| `ProductRestMapper` | `Product`, `ProductVariant` |
| `CartRestMapper` | `Cart`, `CartItem` |
| `OrderRestMapper` | `Order`, `OrderLine` |
| `InventoryRestMapper` | `InventoryItem`, `InventoryMovement`, `Warehouse` |
| `AfterSalesRestMapper` | `Invoice`, `Shipment`, `ProductReturn`, `Refund` |

### 7.1 Campos calculados en las respuestas

Dos respuestas exponen un campo que no es un atributo del modelo sino el resultado de una regla,
para que el cliente no tenga que volver a derivarla:

- `ProductResponse.visibleInCatalog` — exige estado publicado **y** al menos una variante
  (RD-CAT-03 + RD-CAT-05).
- `OrderResponse.modifiable` — falso en cuanto el pedido alcanza el estado terminal (RD-PED-02).
