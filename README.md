# NexusMarket

Backend de un marketplace multi-vendedor, desarrollado en **Java 17** con **Spring Boot 4.1.1**,
siguiendo arquitectura hexagonal (Ports & Adapters) con un núcleo de dominio DDD. Proyecto
académico para la asignatura *Construcción de Software 2*.

## Arquitectura

El proyecto separa el dominio de negocio de los detalles técnicos mediante puertos y adaptadores:

- **Dominio** (`application.domain`) — entidades, value objects, excepciones, puertos y servicios.
  No importa nada de Spring, JPA, MongoDB ni ningún otro framework.
- **Casos de uso** (`application.adapters.useCases`) — implementan los puertos de entrada
  delegando en los servicios de dominio.
- **Persistencia** (`application.adapters.persistence`) — adaptadores JPA (MySQL) para los 12
  agregados de negocio, y un adaptador MongoDB para el log de auditoría.
- **REST** (`application.adapters.rest`) — controladores, DTOs y mappers.
- **Seguridad** (`application.infrastructure.security`) — autenticación JWT.

Todas las capas están implementadas: 294 archivos de producción, cubiertos por 217 pruebas.

La documentación de diseño completa, con trazabilidad a las reglas de negocio, está en
[`SDD/`](SDD/):

- [Domain Model](<SDD/domain/Domain Model.md>) — entidades, ciclos de vida y reglas de negocio.
- [Domain Value Objects](<SDD/domain/Domain Value Objects.md>) — value objects y enumeraciones.
- [Domain Services](<SDD/domain/Domain Services.md>) — los 52 servicios de dominio.
- [Input-ports](<SDD/domain/Input-ports.md>) / [Output-ports](<SDD/domain/Output-ports.md>) —
  puertos de entrada (por rol) y de salida (por agregado).
- [Software Architecture](<SDD/Software Architecture/Software Architecture.md>) — capas y
  decisiones de arquitectura.
- [`SDD/Adapters/`](SDD/Adapters/) y [`SDD/Infrastructure/`](SDD/Infrastructure/) — casos de uso,
  persistencia, REST, seguridad y el bootstrap del administrador inicial.

## Tecnologías

- Java 17, Maven
- Spring Boot 4.1.1 (Web, Data JPA, Data MongoDB, Security, Validation, Actuator)
- MySQL 8.4 — persistencia de los 12 agregados de negocio
- MongoDB 7 — log de auditoría (`OperationLog`)
- JWT (`io.jsonwebtoken`) — autenticación stateless
- springdoc-openapi — documentación interactiva de la API (Swagger UI)
- Lombok
- JUnit 5, H2 (en memoria, solo para pruebas)

## Requisitos

- Java 17
- Maven (o usar el wrapper `./mvnw` incluido)
- Docker y Docker Compose (para levantar MySQL y MongoDB sin instalarlos localmente)

## Puesta en marcha

### Con Docker (recomendado)

```bash
cp .env.example .env        # y escribe SEED_ADMIN_PASSWORD (y JWT_SECRET) en .env
docker compose up --build
```

`SEED_ADMIN_PASSWORD` es obligatoria: sin ella no se crea ningún administrador inicial y, dado que
solo los compradores pueden autorregistrarse, no habría forma de crear vendedores ni bodegas. Por eso
Compose se niega a arrancar si falta, con el mensaje
`Define SEED_ADMIN_PASSWORD (in the environment or in .env)`. También puedes pasarla sin `.env`:

```bash
SEED_ADMIN_PASSWORD='una-contraseña-larga-y-única' docker compose up --build
```

El administrador inicial se crea **solo la primera vez** (con la base vacía). Ingresa con
`SEED_ADMIN_EMAIL` (por omisión `admin@nexusmarket.local`) y esa contraseña. Detalles del proceso
en [Bootstrap.md](<SDD/Infrastructure/Bootstrap.md>).

Todas las variables, con su valor por defecto, están documentadas en [`.env.example`](.env.example):
administrador inicial (`SEED_ADMIN_*`), seguridad (`JWT_SECRET`, `JWT_EXPIRATION_MINUTES`,
`CORS_ALLOWED_ORIGINS`), bases de datos (`MYSQL_*`, `MONGO_*`, `JPA_DDL_AUTO`) y puertos del host
(`HOST_SERVER_PORT`, `HOST_MYSQL_PORT`, `HOST_MONGO_PORT`). En cualquier despliegue real define un
`JWT_SECRET` propio de al menos 32 caracteres: el valor por defecto es solo para desarrollo.

La aplicación queda disponible en `http://localhost:8080`. `.env` está en `.gitignore`: nunca lo
subas al repositorio.

### Localmente, apuntando a los contenedores de bases de datos

```bash
docker compose up mysql-db mongo-db      # con .env creado: Compose valida el archivo completo
SEED_ADMIN_PASSWORD=una-contraseña-segura ./mvnw spring-boot:run
```

## Documentación de la API

Con la aplicación corriendo:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Especificación OpenAPI: `http://localhost:8080/v3/api-docs`

## Pruebas

Corren contra H2 en memoria bajo el perfil `test`, así que no necesitan ninguna base de datos
levantada:

```bash
./mvnw test                                      # toda la suite
./mvnw test -Dtest=ClassName                     # una clase
./mvnw test -Dtest=ClassName#methodName          # un método
```

## Estructura del dominio

```
application.domain.models          23 archivos: DomainEntity<ID> + 22 entidades
application.domain.valuesObjects   37 value objects y enumeraciones
application.domain.services        52 servicios en 14 áreas (authorization, user, seller,
                                    warehouse, catalog, inventory, cart, order, invoice,
                                    shipment, returns, refund, reporting, audit)
application.domain.ports.in        6 puertos de entrada, uno por rol
application.domain.ports.out       13 puertos de salida, uno por agregado
application.domain.exceptions      25 excepciones de dominio
```

## Autor

José Mario Fragua Rengifo
