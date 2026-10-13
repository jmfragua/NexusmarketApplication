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

Todas las capas están implementadas: 283 archivos de producción, cubiertos por 143 pruebas.

La documentación de diseño completa, con trazabilidad a las reglas de negocio, está en
[`SDD/`](SDD/):

- [Domain Model](<SDD/domain/Domain Model.md>) — entidades, ciclos de vida y reglas de negocio.
- [Domain Value Objects](<SDD/domain/Domain Value Objects.md>) — value objects y enumeraciones.
- [Domain Services](<SDD/domain/Domain Services.md>) — los 49 servicios de dominio.
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
export SEED_ADMIN_PASSWORD=una-contraseña-segura
docker compose up
