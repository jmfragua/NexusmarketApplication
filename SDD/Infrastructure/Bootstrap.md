# Arranque del sistema — NexusMarket

**Paquete:** `application.infrastructure.seed`

## 1. El problema del primer administrador

Las reglas del negocio encadenan a los participantes de esta forma:

```text
Administrador ──incorpora──▶ Vendedor ──registra──▶ Producto ──▶ Inventario ──▶ Pedido
```

Solo los **compradores** se autoregistran. Los **vendedores** no pueden hacerlo: los incorpora el
administrador junto con su primera bodega (RD-ROL-05). Y el **administrador** no tiene ninguna vía
de creación en el modelo.

El resultado sobre una base de datos vacía es un bloqueo total: sin administrador no hay vendedores,
sin vendedores no hay catálogo, sin catálogo no hay compras. La plataforma arranca correctamente y
queda inservible.

Esto no es un hueco de la especificación funcional —que describe los procesos del negocio, no su
puesta en marcha— sino una consecuencia de implementarla literalmente. Se resuelve en
infraestructura, que es donde corresponde.

---

## 2. `DatabaseSeeder`

Un `CommandLineRunner` que crea un administrador inicial **si y solo si** la plataforma no tiene
ninguno.

```java
if (!userRepositoryPort.findByRole(UserRole.ADMINISTRATOR).isEmpty()) {
    return;
}
```

### 2.1 Propiedades

| Propiedad | Variable de entorno | Defecto |
|---|---|---|
| `nexusmarket.seed.admin.id` | `SEED_ADMIN_ID` | `admin-001` |
| `nexusmarket.seed.admin.email` | `SEED_ADMIN_EMAIL` | `admin@nexusmarket.local` |
| `nexusmarket.seed.admin.password` | `SEED_ADMIN_PASSWORD` | *(vacío)* |
| `nexusmarket.seed.admin.document` | `SEED_ADMIN_DOCUMENT` | `ADMIN-0001` |

### 2.2 Reglas de diseño

- **RD-BOOT-01. Idempotente.** No hace nada si ya existe un administrador. Reiniciar la aplicación
  nunca duplica usuarios ni reescribe credenciales.
- **RD-BOOT-02. Sin contraseña por defecto.** Si `SEED_ADMIN_PASSWORD` está vacía, el seeder **no
  crea nada** y lo advierte en el log. Una contraseña por defecto en el código fuente sería una
  credencial conocida en todo despliegue que olvidara cambiarla.
- **RD-BOOT-03. Solo el primer administrador.** No siembra productos, vendedores, personal ni datos de
  ejemplo. Los operadores logísticos y los supervisores los incorpora después el administrador con
  `POST /api/v1/admin/staff` (RD-ROL-09). Crea lo mínimo para que el sistema sea operable; el resto se da de alta por el API, que
  es donde las reglas se aplican.
- **RD-BOOT-06. Nunca sobre un usuario existente.** Si no queda ningún administrador pero el
  identificador o el correo configurados ya pertenecen a otro usuario —por ejemplo, porque al
  administrador inicial se le cambió el rol—, el seeder no crea nada y lo registra como error. Crear
  el administrador con ese identificador reemplazaría al usuario existente (RD-ID-06). Además crea
  con `create`, nunca con `save`.
- **RD-BOOT-04. Credencial después del usuario.** Primero se persiste el administrador, después su
  credencial, igual que en el registro de compradores: un fallo intermedio no debe dejar una
  contraseña huérfana.

### 2.3 Uso

Con Maven:

```bash
SEED_ADMIN_PASSWORD=una-clave-segura ./mvnw spring-boot:run
```

Con Docker Compose, las cuatro variables `SEED_ADMIN_*` se leen del entorno o de un archivo `.env`
en la raíz (plantilla en `.env.example`) y se pasan al contenedor de la aplicación:

```bash
cp .env.example .env    # escribe SEED_ADMIN_PASSWORD en .env
docker compose up --build
```

- **RD-BOOT-05. Compose exige la contraseña.** `docker-compose.yml` declara
  `${SEED_ADMIN_PASSWORD:?...}`: si falta, Compose se niega a arrancar con un mensaje que nombra la
  variable. La alternativa —arrancar y dejar solo una advertencia en el log— produce un sistema
  encendido pero inalcanzable, que es precisamente el fallo que este documento existe para evitar.
  Las otras tres variables sí tienen valor por defecto, porque no son secretas.

La contraseña solo se usa la primera vez: con un administrador ya creado el seeder no hace nada
(RD-BOOT-01), así que cambiarla después en `.env` no modifica la credencial existente.

En el log aparece:

```text
INFO a.infrastructure.seed.DatabaseSeeder : Initial administrator created with email admin@nexusmarket.local
```

O bien, si falta la contraseña:

```text
WARN a.infrastructure.seed.DatabaseSeeder : No administrator exists and no seed password is configured.
```

---

## 3. Dependencia con `StaffUser`

El seeder solo es posible porque existe `StaffUser` (ver [Domain Model.md](../domain/Domain%20Model.md)
§4.3.1). `User` es abstracta y sus únicas especializaciones de negocio son `Buyer` y `Seller`, así
que sin una clase concreta para los roles restantes el administrador no podría siquiera
construirse.

El seeder usa `StaffUser.of(...)`, que rechaza los roles `BUYER` y `SELLER`: sembrar un comprador o
un vendedor por esta vía eludiría sus reglas de alta.

---

## 4. Verificación

Comprobado contra MySQL y MongoDB reales:

| Caso | Resultado |
|---|---|
| Base vacía con contraseña configurada | Administrador creado, login correcto |
| Segundo arranque | No crea nada (idempotente) |
| Sin contraseña configurada | No crea nada, advertencia en el log |
| El administrador incorpora un vendedor | `201`, y la operación queda auditada en MongoDB |
