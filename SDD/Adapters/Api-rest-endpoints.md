# Contrato del API REST — NexusMarket

**Base:** `/api/v1` · **Autenticación:** `Authorization: Bearer <jwt>`

## 1. Convenciones

| Código | Uso |
|---|---|
| `200` | Consulta o modificación satisfactoria |
| `201` | Recurso creado |
| `204` | Sin contenido (total facturado inexistente) |
| `400` | Petición inconsistente o inválida |
| `401` | Token ausente, inválido o expirado |
| `403` | El rol o la pertenencia no permiten la operación |
| `404` | La entidad referida no existe |
| `409` | El estado actual de la entidad prohíbe la operación |

Todo error devuelve el mismo cuerpo:

```json
{ "status": 409, "rule": "OrderNotModifiableException", "message": "..." }
```

### Paginación

**Todo endpoint `GET` que devuelva una colección está paginado** y acepta dos parámetros de
consulta opcionales:

| Parámetro | Por omisión | Rango |
|---|---|---|
| `page` | `0` | `>= 0` |
| `size` | `20` | `1..200` |

La respuesta no es un arreglo, sino el envoltorio:

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 37,
  "totalPages": 2,
  "first": true,
  "last": false
}
```

Una página más allá del final devuelve `content` vacío con `last: true`, lo que permite avanzar sin
conocer el total. Un `page` negativo o un `size` fuera de rango devuelven **400**. El corte se hace
en el adaptador REST sobre la colección completa; la limitación está documentada en
[Rest-adapters.md](Rest-adapters.md) §5.

### Documentación navegable

`springdoc-openapi` publica el contrato en `/v3/api-docs` y la interfaz en `/swagger-ui.html`, ambas
abiertas sin autenticación. El esquema `bearerAuth` está declarado, así que el botón *Authorize*
permite pegar el JWT obtenido en `/api/v1/auth/login` y probar los endpoints protegidos desde ahí.

---

## 2. Acceso público — sin autenticación

### `POST /api/v1/auth/register`
Autoregistro de comprador. Los vendedores no aparecen aquí: los incorpora el administrador
(RD-ROL-05).

```json
{ "buyerId": "...", "fullName": "...", "email": "...", "identityDocument": "...",
  "password": "...", "primaryAddress": "..." }
```
→ `201` `AuthResponse` · `409` correo o documento duplicados

### `POST /api/v1/auth/login`
```json
{ "email": "...", "password": "..." }
```
→ `200` `AuthResponse` · `403` credenciales inválidas o usuario bloqueado

### `GET /api/v1/catalog/products`
Catálogo público: solo productos publicados **con** al menos una variante.
→ `200` `ProductResponse[]`

### `GET /api/v1/catalog/products/{productId}`
→ `200` `ProductResponse` · `404` no existe · `409` existe pero no es visible

---

## 3. Comprador — rol `BUYER`

| Método | Ruta | Operación |
|---|---|---|
| `POST` | `/buyer/me/addresses` | Añadir dirección adicional |
| `PUT` | `/buyer/me/addresses/primary` | Promover dirección a principal |
| `GET` | `/buyer/me/cart` | Consultar carrito propio |
| `POST` | `/buyer/me/cart/items` | Seleccionar variante (`201`) |
| `PATCH` | `/buyer/me/cart/items/{cartItemId}` | Cambiar cantidad |
| `DELETE` | `/buyer/me/cart/items/{cartItemId}` | Retirar línea |
| `DELETE` | `/buyer/me/cart` | Vaciar carrito |
| `POST` | `/buyer/me/orders` | Confirmar carrito como pedido (`201`) |
| `POST` | `/buyer/me/orders/{orderId}/payment` | Confirmar pago |
| `GET` | `/buyer/me/orders` | Pedidos propios |
| `GET` | `/buyer/me/orders/{orderId}` | Pedido propio |
| `GET` | `/buyer/me/invoices` | Facturas propias |
| `GET` | `/buyer/me/orders/{orderId}/invoice` | Factura del pedido |
| `POST` | `/buyer/me/returns` | Solicitar devolución (`201`) |
| `GET` | `/buyer/me/orders/{orderId}/returns` | Devoluciones del pedido |

**Errores característicos:** `403` si el recurso es de otro comprador · `409` carrito vacío,
producto no disponible, pedido no modificable, transición inválida · `403` comprador
comercialmente inhabilitado.

---

## 4. Vendedor — rol `SELLER`

| Método | Ruta | Operación |
|---|---|---|
| `POST` | `/seller/products` | Registrar producto (`201`) |
| `GET` | `/seller/products` | Catálogo propio, en cualquier estado |
| `POST` | `/seller/products/{productId}/publication` | Publicar |
| `DELETE` | `/seller/products/{productId}/publication` | Suspender |
| `DELETE` | `/seller/products/{productId}` | Descontinuar (terminal) |
| `POST` | `/seller/products/{productId}/variants` | Añadir variante (`201`) |
| `DELETE` | `/seller/products/{productId}/variants/{variantId}` | Retirar variante |
| `GET` | `/seller/warehouses` | Bodegas propias |
| `GET` | `/seller/warehouses/{warehouseId}` | Bodega propia |
| `GET` | `/seller/products/{productId}/stock` | Existencias del producto |
| `POST` | `/seller/stock/receipts` | Registrar ingreso (`201`) |
| `POST` | `/seller/stock/adjustments` | Ajustar existencias (`201`, auditado) |
| `POST` | `/seller/stock/{inventoryItemId}/damage` | Marcar como dañado |

**Nota sobre publicar/suspender.** Se modelan como creación y borrado del subrecurso
`publication`, no como un `PATCH` de estado, porque son transiciones con reglas propias y no una
edición libre del campo.

**Errores característicos:** `403` producto o bodega de otro vendedor · `409` descontinuar sin
suspender primero, o modificar un producto descontinuado.

---

## 5. Operador logístico — rol `LOGISTICS_OPERATOR`

| Método | Ruta | Operación |
|---|---|---|
| `GET` | `/logistics/warehouses` | Todas las bodegas |
| `GET` | `/logistics/warehouses/{warehouseId}/stock` | Existencias de la bodega |
| `POST` | `/logistics/stock/receipts` | Ingreso (`201`) |
| `POST` | `/logistics/stock/reservations` | Reserva por línea de pedido (`201`) |
| `POST` | `/logistics/stock/releases` | Liberar reserva (`201`) |
| `POST` | `/logistics/stock/sale-outbounds` | Salida por venta (`201`) |
| `POST` | `/logistics/stock/returns/{returnId}` | Reintegrar devolución (`201`) |
| `POST` | `/logistics/shipments/packaging` | Empacar envío |
| `POST` | `/logistics/shipments/dispatches` | Despachar |
| `POST` | `/logistics/shipments/deliveries` | Confirmar entrega |
| `GET` | `/logistics/orders?status=PAID` | Pedidos por estado |

Las operaciones de reserva, liberación y salida **no reciben cantidad**: la toman de la línea del
pedido, de modo que el compromiso nunca puede desalinearse de lo ordenado.

**Errores característicos:** `409` existencias insuficientes, existencias dañadas, pedido sin
productos físicos, transición de pedido inválida.

---

## 6. Administrador — rol `ADMINISTRATOR`

| Método | Ruta | Operación |
|---|---|---|
| `POST` | `/admin/sellers` | Incorporar vendedor + primera bodega (`201`) |
| `GET` | `/admin/sellers` · `/admin/sellers/{sellerId}` | Consultar vendedores |
| `POST` | `/admin/users/{userId}/block` | Bloquear usuario |
| `POST` | `/admin/users/{userId}/activation` | Reactivar usuario |
| `PUT` | `/admin/users/{userId}/role` | Reasignar rol |
| `POST` | `/admin/warehouses` | Registrar bodega del marketplace (`201`) |
| `POST` | `/admin/invoices` | Emitir factura (`201`) |
| `GET` | `/admin/returns/{returnId}` | Consultar devolución |
| `POST` | `/admin/refunds` | Emitir reembolso (`201`, auditado) |
| `GET` | `/admin/refunds` | Consultar reembolsos |

**Errores característicos:** `409` correo o documento duplicados · `400` reembolso superior al total
facturado · `409` facturar un pedido cuyo pago no está validado.

---

## 7. Supervisor — rol `SUPERVISOR`

**Solo lectura.** Ningún verbo de escritura, por RD-ROL-06.

| Método | Ruta | Operación |
|---|---|---|
| `GET` | `/supervisor/orders?status=...` | Pedidos por estado |
| `GET` | `/supervisor/reports/completed-orders` | Pedidos entregados |
| `GET` | `/supervisor/reports/invoices` | Facturación emitida |
| `GET` | `/supervisor/reports/invoiced-total` | Total facturado (`204` si no hay nada) |
| `GET` | `/supervisor/reports/warehouses/{warehouseId}/stock` | Existencias por bodega |
| `GET` | `/supervisor/reports/damaged-stock` | Existencias dañadas |
| `GET` | `/supervisor/reports/inventory-movements` | Trazabilidad completa |
| `GET` | `/supervisor/reports/refunds` | Reembolsos |

**Por qué `204` en el total facturado.** No existe un `Money` neutro sin moneda, y sumar una
colección vacía no tiene resultado posible. El dominio devuelve vacío en lugar de inventar un cero
con moneda arbitraria (RD-ALC-03); el API lo traduce a "sin contenido".

**No existe un endpoint de stock total sin bodega**: el inventario es distribuido y una cifra
agregada contradiría RD-INV-01.

---

## 8. Rutas abiertas en la configuración de seguridad

```text
/api/v1/auth/**              permitAll
GET /api/v1/catalog/**       permitAll
/actuator/health, /info      permitAll
cualquier otra               authenticated
```

El catálogo abierto es la única lectura que se sale de RG-01, y es deliberada: un visitante debe
poder navegarlo antes de registrarse.
