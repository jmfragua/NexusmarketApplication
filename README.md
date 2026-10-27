# NexusMarket

Marketplace multi-vendedor: muchos vendedores, un solo carrito. Proyecto académico para la
asignatura *Construcción de Software 2*.

- **Backend:** Java 17 y Spring Boot 4.1.1, con arquitectura hexagonal (Ports & Adapters) y núcleo
  de dominio DDD. Persiste los 12 agregados de negocio en **MySQL 8.4** y el log de auditoría en
  **MongoDB 7**. Autenticación JWT.
- **Frontend:** React, Vite y TypeScript, en [`frontend/`](frontend/). Tienda para compradores y
  paneles para vendedor, operador logístico, administrador y supervisor.

## Requisitos

- **Docker** con Docker Compose (MySQL, MongoDB y el backend corren en contenedores).
- **Java 17**, solo si vas a correr el backend o sus pruebas fuera de Docker (el wrapper `./mvnw`
  ya trae Maven).
- **Node.js 22.12 o superior** para el frontend. Vite 8 y Vitest 5, que usa el proyecto, no
  arrancan con versiones anteriores (en concreto, no con Node 18).

## Puesta en marcha

### 1. Backend y bases de datos

```bash
cp .env.example .env        # solo la primera vez; escribe SEED_ADMIN_PASSWORD (y JWT_SECRET)
docker compose up -d
```

`SEED_ADMIN_PASSWORD` es obligatoria: crea el administrador inicial la primera vez que la base está
vacía, y Compose se niega a arrancar si falta. Todas las variables, con su valor por defecto, están
en [`.env.example`](.env.example). `.env` está en `.gitignore`: no lo subas al repositorio.

> **No uses `docker compose down -v`.** La opción `-v` borra los volúmenes con todos los datos
> (usuarios, productos, pedidos y stock). Para detener los contenedores basta con
> `docker compose stop` o `docker compose down` sin `-v`.

Comprueba que el backend responde:

```bash
curl http://localhost:8080/actuator/health        # → {"status":"UP", ...}
```

La documentación interactiva de la API está en `http://localhost:8080/swagger-ui.html`.

### 2. Frontend

```bash
cd frontend
npm install                 # solo la primera vez
npm run dev
```

Abre `http://localhost:5173`. Si el backend no responde, la tienda muestra 8 productos de
ejemplo (no se pueden comprar) con el aviso «estás viendo productos de ejemplo».

## Cuentas de prueba (solo local)

> **Estas claves son solo para la demo local. No las uses nunca en un despliegue real.**

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | `admin@nexusmarket.local` | `Admin12345` (la de `SEED_ADMIN_PASSWORD` en `.env`) |
| Operador logístico | `logistica@nexusmarket.local` | *Pendiente:* no está en `.env` ni en el seed; es la que se definió al crearla desde **Admin › Usuarios › Crear personal**. |
| Supervisor | `supervisor@nexusmarket.local` | `Supervisor123` |
| Vendedor | `paramo@nexusmarket.test` | `Vendedor123` (tienda de prueba **Páramo**, dueña de los 21 productos del catálogo) |
| Comprador | — | Se crea desde `/registro`. |

- **Compradores:** son los únicos que se registran solos, en `/registro`.
- **Vendedores:** no se registran solos. Los incorpora el administrador desde
  **Admin › Vendedores › Incorporar vendedor**, junto con su primera bodega.
- **Personal:** el operador logístico y el supervisor los crea el administrador desde
  **Admin › Usuarios › Crear personal**.

## Flujo completo para probar

| # | Paso | Rol | Dónde |
|---|---|---|---|
| 1 | Agregar productos al carrito | Comprador | Catálogo › producto › *Agregar al carrito* |
| 2 | Confirmar el carrito y pagar (el pedido queda **Pagado**) | Comprador | Carrito › *Ir a pagar* |
| 3 | Emitir la factura del pedido pagado | Administrador | Admin › Facturación |
| 4 | Reservar las unidades del pedido | Operador logístico | Logística › pedido › *Reservar* |
| 5 | Salida por venta (las unidades salen de la bodega) | Operador logístico | Logística › pedido › *Salida por venta* |
| 6 | Empacar el envío | Operador logístico | Logística › pedido › *Empacar* |
| 7 | Despachar (el pedido queda **Despachado**) | Operador logístico | Logística › pedido › *Despachar* |
| 8 | Confirmar la entrega (el pedido queda **Entregado**) | Operador logístico | Logística › pedido › *Confirmar entrega* |
| 9 | Ver la factura | Comprador | Mis pedidos › pedido, o Facturas |
| 10 | Solicitar una devolución (solo con el pedido entregado) | Comprador | Mis pedidos › pedido › *Devolver* |
| 11 | Reintegrar al stock lo devuelto (opcional) | Operador logístico | Logística › Bodegas › *reintegrar una devolución* |
| 12 | Emitir el reembolso (nunca mayor que lo facturado) | Administrador | Admin › Devoluciones y reembolsos |

Durante todo el flujo, el **supervisor** consulta los reportes en su panel: pedidos completados,
facturación, inventario por bodega, stock dañado, movimientos y reembolsos.

## Cómo funciona el inventario

- **Por producto y bodega, no por variante.** Cada registro de inventario liga un producto a una
  bodega. Las variantes (talla, color…) de un producto comparten ese mismo stock.
- **Pagar no toca el stock.** El inventario cambia solo con las operaciones de bodega:
  - **Reservar** pasa unidades de *disponible* a *reservado* para un pedido.
  - **Salida por venta** saca de la bodega las unidades reservadas. Ahí se descuentan
    definitivamente.
  - Una reserva se puede **liberar**, y una devolución puede **reintegrar** unidades.
- **Nunca hay existencias negativas.** Una reserva o una salida que supere lo que hay se rechaza.
- **Sin stock para productos digitales.** No llevan inventario y el backend no permite abrirles un
  registro.

## Limitaciones conocidas del backend

Están documentadas y no se modifican en esta entrega:

- **El checkout no valida stock.** Se puede pagar un pedido aunque no haya existencias. El stock se
  valida al reservar, en logística.
- **El despacho no exige reserva previa.** Solo exige que el pedido esté pagado.
- **El administrador no tiene listados de usuarios, facturas ni pedidos.**
  - Bloquear, reactivar o cambiar el rol se hace con el identificador del usuario.
  - Una factura se consulta a partir de su pedido.
  - El supervisor sí tiene reportes de pedidos y facturación.
- **No se puede crear un administrador directamente.** «Crear personal» solo acepta operador
  logístico o supervisor. Para tener otro administrador, se crea como personal y luego se le cambia
  el rol en **Admin › Usuarios**.

## Pruebas y verificación

**Backend:** corre contra H2 en memoria (perfil `test`), así que no necesita ninguna base de datos
levantada.

```bash
./mvnw test                                      # toda la suite
./mvnw test -Dtest=ClassName                     # una clase
```

**Frontend:** desde `frontend/`.

```bash
npm test          # pruebas (Vitest + Testing Library)
npm run lint      # oxlint, sin advertencias permitidas
npm run build     # compilación de TypeScript y build de producción
```

## Arquitectura

- **Dominio** (`application.domain`): entidades, value objects, excepciones, puertos y servicios.
  No importa nada de Spring, JPA ni MongoDB.
- **Casos de uso** (`application.adapters.useCases`): implementan los puertos de entrada, uno por
  rol.
- **Persistencia** (`application.adapters.persistence`): JPA para MySQL y un adaptador MongoDB para
  la auditoría.
- **REST** (`application.adapters.rest`): controladores, DTOs y mappers.
- **Seguridad** (`application.infrastructure.security`): autenticación JWT.

La documentación de diseño, con trazabilidad a las reglas de negocio, está en [`SDD/`](SDD/):
[modelo de dominio](<SDD/domain/Domain Model.md>), [value objects](<SDD/domain/Domain Value Objects.md>),
[servicios](<SDD/domain/Domain Services.md>), [puertos de entrada](<SDD/domain/Input-ports.md>) y
[de salida](<SDD/domain/Output-ports.md>), [arquitectura](<SDD/Software Architecture/Software Architecture.md>),
y en [`SDD/Adapters/`](SDD/Adapters/) y [`SDD/Infrastructure/`](SDD/Infrastructure/) los adaptadores,
la seguridad y el [arranque del administrador inicial](<SDD/Infrastructure/Bootstrap.md>).

## Imágenes

Las fotos de los productos están dentro del proyecto, en `frontend/public/productos/`, así que se ven sin
conexión a internet. Vienen de [Unsplash](https://unsplash.com) (licencia gratuita de Unsplash) y se
descargaron a 800 px de ancho.

Para añadir una nueva:

1. Copia el archivo en `frontend/public/productos/` (por ejemplo `gorra.jpg`; usa solo letras, números,
   `-`, `_` y `.`, sin espacios).
2. Al crear la variante desde el panel del vendedor, escribe en el campo **Imagen** la ruta
   `/productos/gorra.jpg`.

El campo también acepta una URL `https://`. Si la imagen no carga, la tienda dibuja una portada generada.

## Autor

José Mario Fragua Rengifo
