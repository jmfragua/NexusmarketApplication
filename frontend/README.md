# NexusMarket · frontend

Interfaz web del marketplace NexusMarket: tienda para compradores y paneles de trabajo para
vendedores, operación logística, administración y supervisión. Consume el API REST del backend de
este mismo repositorio (`/api/v1`) y no tiene servidor propio.

## Stack

- React 18, Vite y TypeScript estricto (sin `any`).
- React Router para las rutas y TanStack Query para los datos del servidor.
- Tailwind CSS 4 con tokens propios, más Radix UI para los diálogos accesibles.
- react-hook-form y zod para los formularios, y sonner para las notificaciones.
- Vitest y Testing Library para las pruebas, y oxlint para el lint.

## Cómo correrlo

Necesitas Node 20 o superior y el backend en marcha.

```bash
# 1. Backend (desde la raíz del repositorio)
export SEED_ADMIN_PASSWORD=una-contraseña-segura
docker compose up            # o: ./mvnw spring-boot:run con MySQL y Mongo levantados

# 2. Frontend
cd frontend
cp .env.example .env.local   # ajusta VITE_API_URL si el backend no está en :8080
npm install
npm run dev                  # http://localhost:5173
```

El puerto 5173 es fijo (`strictPort`). Es el único origen que el backend acepta por CORS en
desarrollo, así que si está ocupado Vite falla en lugar de moverse a otro puerto.

| Script | Qué hace |
|---|---|
| `npm run dev` | servidor de desarrollo en `http://localhost:5173` |
| `npm run build` | verificación de tipos y build de producción en `dist/` |
| `npm run lint` | oxlint; cualquier advertencia cuenta como error |
| `npm test` | pruebas con Vitest (cliente API, totales, atributos, login y carrito) |
| `npm run preview` | sirve `dist/` en el puerto 5173 |

### Variables de entorno

| Variable | Ejemplo | Uso |
|---|---|---|
| `VITE_API_URL` | `http://localhost:8080` | URL base del backend, **sin** `/api/v1` ni barra final. Si falta, se usa `http://localhost:8080`. |

Las variables `VITE_*` se incrustan al compilar: si las cambias, vuelve a compilar.

## Convención de atributos de variante

El backend no modela nombre, precio ni imagen en el producto. Un producto solo tiene
`id, sellerId, productType, status, visibleInCatalog` y una lista de variantes, y cada variante es
un mapa libre `attributes: Map<String, String>`. Este frontend guarda los datos comerciales en
claves reservadas de ese mapa:

| Clave | Obligatoria | Ejemplo | Uso |
|---|---|---|---|
| `name` | sí | `Morral urbano 22 L` | título del producto en la tienda |
| `price` | sí | `189000` | precio unitario, número sin separadores de miles |
| `currency` | no (por omisión `COP`) | `COP` | código ISO de 3 letras |
| `description` | no | `Lona encerada…` | descripción en el detalle |
| `image` | no | `https://…/foto.jpg` | foto; solo se aceptan URL `http(s)` |
| `brand` | no | `Páramo` | marca, sobre el título |

**Cualquier otra clave** (por ejemplo `Color: Azul` o `Talla: M`) es una opción que el comprador
elige en el detalle del producto. Las claves reservadas se leen sin distinguir mayúsculas.

El formulario del vendedor pide estos campos y los guarda así; el comprador nunca ve las claves.
Reglas que se derivan de la convención:

- La tarjeta del catálogo muestra el precio de la variante más barata, con «desde» si los precios
  varían.
- Si un producto no tiene `image`, se dibuja una portada SVG determinística a partir de su `id`:
  el mismo producto tiene siempre la misma portada, en el catálogo, el carrito y el pedido.
- Una variante sin `price` se muestra con el sticker «sin precio» y no se puede comprar.
- Al confirmar el carrito, el cliente envía por línea `lineAmount = price × quantity` y la
  `currency` de la variante. Si el carrito mezcla monedas, el pago se bloquea con un aviso.

## Roles y rutas

Cada usuario tiene un único rol (RG-02) y nunca ve las rutas de otro: si entra a una, vuelve a su
inicio. El código de cada panel de operación se carga aparte, así que un comprador no lo descarga.

| Rol | Inicio | Qué puede hacer |
|---|---|---|
| Visitante | `/` | navegar el catálogo, ver productos, registrarse |
| Comprador | `/` | carrito, pago, pedidos con línea de estado, facturas, devoluciones, direcciones |
| Vendedor | `/vendedor` | registrar productos, variantes, publicar/suspender/descontinuar, bodegas, existencias |
| Operador logístico | `/logistica` | pedidos por estado, reservas, salidas, empaque, despacho, entrega, ingresos, reintegros |
| Administrador | `/admin` | vendedores, bloqueo y rol de usuarios, bodegas del marketplace, facturas, reembolsos |
| Supervisor | `/supervisor` | reportes de solo lectura: resumen, facturación e inventario |

## Errores y sesión

- Todos los errores del API llegan como `{status, rule, message}`. `src/api/errors.ts` traduce las
  reglas conocidas a español y, si no conoce la regla, muestra el `message` del backend.
- La sesión vive en memoria y en `localStorage` (cada acceso va envuelto en `try/catch`). El token
  nunca va en la URL ni en los logs.
- La sesión se cierra cuando el token expira (se lee el claim `exp`) o cuando el backend la rechaza.
- Si el backend no responde o el catálogo está vacío, la tienda muestra productos de ejemplo con un
  aviso visible. Esos productos no se pueden comprar.

## Despliegue en Vercel

1. Importa el repositorio en Vercel y define **Root Directory = `frontend`**. El framework se
   detecta como Vite; `vercel.json` ya incluye el rewrite de SPA a `index.html`.
2. En *Environment Variables* define `VITE_API_URL` con la URL pública del backend, por ejemplo
   `https://api.tu-dominio.com`.
3. **En el backend**, añade la URL del frontend a `CORS_ALLOWED_ORIGINS`. Sin esto, el navegador
   bloquea todas las llamadas al API:

   ```bash
   CORS_ALLOWED_ORIGINS=http://localhost:5173,https://nexusmarket.vercel.app
   ```

   El backend nunca acepta un comodín, porque el API se autentica con token bearer.
4. El backend debe servirse por HTTPS: un sitio HTTPS no puede llamar a un API HTTP.

## Estructura

```
src/
  api/          cliente HTTP tipado, tipos espejo de los DTO, endpoints por rol, traducción de errores
  app/          rutas, cliente de consultas, 404
  components/   UI base (botón, campos, diálogo, estados, paginación…)
  features/     una carpeta por funcionalidad: auth, catalog, cart, checkout, orders, account,
                seller, inventory, logistics, admin, supervisor
  layouts/      tienda (StoreLayout) y panel de trabajo (WorkspaceLayout)
  lib/          dinero, ids, almacenamiento seguro, etiquetas en español
  styles/       tokens de diseño y capas de Tailwind
```

## Fallos y limitaciones del backend

Encontré estos puntos durante la integración. Están reportados aquí y no se esquivan en silencio;
la interfaz explica cada caso cuando el usuario lo encuentra.

1. **No existe ninguna operación que cree un registro de inventario** (producto + bodega).
   `new InventoryItem(...)` no se llama en ningún punto del código de producción, y los ingresos,
   ajustes, reservas, salidas y daños exigen un `inventoryItemId` ya existente. Mientras no haya un
   endpoint de alta, las pantallas de existencias muestran un estado vacío que lo explica. La
   compra no depende del inventario, así que funciona igual.
2. **No se pueden crear operadores logísticos ni supervisores.** El registro público crea
   compradores, el administrador solo incorpora vendedores y `PUT /admin/users/{id}/role` solo
   intercambia roles entre usuarios de personal ya existentes. El único usuario de personal es el
   administrador que crea el seeder.
3. **El backend nunca responde 401.** Sin token, o con un token inválido o expirado, devuelve
   `403` con cuerpo vacío, aunque el contrato dice 401. El frontend distingue los dos casos: un 403
   sin `{status, rule, message}` en una petición autenticada se trata como sesión rechazada.
4. **`docker-compose.yml` no le pasa `SEED_ADMIN_PASSWORD` al contenedor.** Exportarla antes de
   `docker compose up`, como indica el README de la raíz, no tiene efecto. Hay que añadir
   `SEED_ADMIN_PASSWORD: ${SEED_ADMIN_PASSWORD:-}` (y las demás `SEED_*`) al servicio `app`.
5. **Faltan consultas de lectura.** No hay `GET` del perfil del comprador, de un producto propio
   por id, de envíos, ni listados de usuarios, facturas o bodegas para el administrador. El
   frontend lo resuelve así:
   - recuerda en el navegador las direcciones que el backend confirmó;
   - lee el catálogo propio del vendedor (máximo 200 productos);
   - deriva el id del envío del id del pedido (`shp-<pedido>`, un envío por pedido);
   - recuerda localmente las facturas y bodegas que se crearon desde ese navegador.

## Pruebas

```bash
npm test
```

- `src/api/client.test.ts`: token bearer, cuerpo de error, sesión rechazada (401 y 403 sin
  cuerpo), error de red y 204.
- `src/features/cart/totals.test.ts`: precios por línea, total, productos retirados, variantes sin
  precio, monedas mezcladas y líneas del pedido.
- `src/features/catalog/variant.test.ts`: lectura de precios y la convención de atributos.
- `src/features/auth/LoginPage.test.tsx` y `src/features/cart/CartPage.test.tsx`: los flujos de
  login y carrito contra un backend simulado.
