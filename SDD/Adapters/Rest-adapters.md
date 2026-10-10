# Adaptadores REST — NexusMarket

**Paquete:** `application.adapters.rest`

## 1. Introducción

La capa REST es el mecanismo de entrega: traduce HTTP a modelos de dominio, invoca el puerto de
entrada del rol correspondiente, y traduce el resultado de vuelta a JSON.

```text
adapters/rest/
├── controllers/        7 controladores
├── dtos/requests/     18 DTOs de entrada
├── dtos/responses/    18 DTOs de salida
├── mappers/            6 mappers estáticos
└── exception/          GlobalExceptionHandler
```

---

## 2. Reglas de diseño

- **RD-REST-01.** Un controlador por **rol**, no por entidad, en correspondencia uno a uno con los
  puertos de entrada. `AuthRestController` y `CatalogRestController` son la excepción: cubren el
  puerto público, que no corresponde a ningún rol autenticado.
- **RD-REST-02.** Los controladores inyectan **puertos de entrada**, nunca servicios de dominio ni
  casos de uso concretos. (Una excepción documentada en §5.)
- **RD-REST-03.** Los controladores **no capturan excepciones**. Las dejan subir hasta
  `GlobalExceptionHandler`, que concentra en una sola tabla la traducción regla → código HTTP.
- **RD-REST-04.** Los DTOs son `record` inmutables con validación `jakarta.validation`. Esa
  validación solo guarda la **forma** de la petición; las reglas reales las revalidan los objetos de
  valor y los servicios de dominio (RD-VO-07).
- **RD-REST-05.** El actor autenticado llega como `@AuthenticationPrincipal DomainUserPrincipal`,
  que transporta el `User` de dominio real, no un principal del framework.

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

### 4.2 Resolución mediante puerto de salida (documentada)

`SellerRestController`, `LogisticOperatorRestController` y `AdministratorRestController` inyectan
puertos de salida **solo para resolver** la entidad. Los puertos de entrada no exponen un
"buscar por identificador" para cada caso, y ampliarlos habría modificado contratos ya
especificados.

**Por qué no abre un agujero de seguridad:** cargar no decide nada. La operación posterior pasa por
el puerto de entrada, y el servicio de dominio revalida rol y pertenencia antes de tocar el modelo.
Un vendedor que resuelva el producto de otro recibe igualmente `ProductNotOwnedException` de
`ValidateSellerOwnershipService`.

Queda registrado como deuda consciente: lo correcto a futuro es ampliar los puertos de entrada con
las consultas que faltan.

---

## 5. Excepción documentada: `IssueInvoiceService` inyectado directamente

La facturación corresponde al vendedor y al administrador (RD-FAC-01), pero no figura en
`AdministratorPort`. `AdministratorRestController` inyecta `IssueInvoiceService` directamente, en
lugar de ampliar el puerto. El servicio sigue validando el rol, que es donde vive la regla. Es la
única desviación de RD-REST-02 y se corrige ampliando el puerto de entrada.

---

## 6. Traducción de excepciones a HTTP

`GlobalExceptionHandler` (`@RestControllerAdvice`) mantiene la tabla. El agrupamiento es deliberado:

| Código | Significado | Excepciones |
|---|---|---|
| **403** | El actor no puede hacer esto | `UnauthorizedOperationException`, `UserNotActiveException`, `ProductNotOwnedException`, `WarehouseNotOwnedException`, `BuyerNotAllowedToPurchaseException` |
| **404** | La entidad referida no existe | `EntityNotFoundException`, `InvoiceNotIssuedException` |
| **409** | Existe, pero su estado lo prohíbe | `OrderNotModifiableException`, `InvalidOrderTransitionException`, `InsufficientStockException`, `DamagedStockException`, `ProductNotAvailableException`, `ReturnNotAllowedException`, `EmptyCartException`, `DuplicateEmailException`, `DuplicateIdentityDocumentException`, `DuplicateInventoryItemException` |
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
