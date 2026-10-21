# Arquitectura de Software — NexusMarket

## 1. Introducción

NexusMarket se construye sobre **Arquitectura Hexagonal** (Ports & Adapters) con un núcleo de
**Domain-Driven Design**. La decisión responde a una exigencia explícita del modelo de dominio: la
especificación funcional deja fuera de alcance las tecnologías, la arquitectura de software y el
almacenamiento de la información (RD-ALC-01, RD-ALC-02), de modo que el dominio debe poder existir y
probarse sin saber que existen Spring, MySQL o MongoDB.

**Stack:** Java 17, Spring Boot 4.1.1, Maven.

---

## 2. Capas y dependencias

```
                    ┌──────────────────────────────────────────┐
                    │              adapters/rest               │
                    │   Controladores · DTOs · Mappers REST    │
                    └────────────────────┬─────────────────────┘
                                         │ invoca
                                         ▼
                    ┌──────────────────────────────────────────┐
                    │           adapters/useCases              │
                    │   Una clase por rol; implementa los       │
                    │   puertos de entrada e inyecta servicios  │
                    └────────────────────┬─────────────────────┘
                                         │ implementa
                    ┌────────────────────▼─────────────────────┐
                    │           domain/ports/in                │
                    │   PublicAccess · Buyer · Seller ·        │
                    │   LogisticOperator · Administrator ·     │
                    │   Supervisor                             │
                    └────────────────────┬─────────────────────┘
                                         │
   ┌─────────────────────────────────────▼─────────────────────────────────────┐
   │                                 DOMINIO                                    │
   │                                                                            │
   │   domain/models          Entidades con identidad y comportamiento          │
   │   domain/valuesObjects   Objetos de valor inmutables y enumeraciones        │
   │   domain/services        52 servicios, uno por caso de uso                 │
   │   domain/exceptions      Una excepción por regla de negocio                │
   │                                                                            │
   │              Cero dependencias de framework. Java puro.                    │
   └─────────────────────────────────────┬─────────────────────────────────────┘
                                         │ depende de
                    ┌────────────────────▼─────────────────────┐
                    │           domain/ports/out               │
                    │   13 interfaces de repositorio            │
                    └────────────────────┬─────────────────────┘
                                         │ implementa
                    ┌────────────────────▼─────────────────────┐
                    │        adapters/persistence              │
                    │   jpa/ (relacional) · mongodb/ (NoSQL)   │
                    │   Entities/Documents · Mappers · Repos   │
                    └──────────────────────────────────────────┘
```

**Regla de dependencia.** Las flechas apuntan siempre **hacia el dominio**. El dominio no conoce a
nadie hacia afuera: declara interfaces (`ports/out`) y los adaptadores las implementan. Esto es lo
que se llama *inversión de dependencias*.

---

## 3. Estructura de paquetes

```
src/main/java/application/
├── NexusmarketApplication.java
├── domain/
│   ├── models/              23 archivos (DomainEntity + 22 entidades)
│   ├── valuesObjects/       37 objetos de valor y enumeraciones
│   ├── exceptions/          24 excepciones de dominio
│   ├── ports/
│   │   ├── in/               6 puertos de entrada (uno por rol)
│   │   └── out/             13 puertos de salida (uno por agregado)
│   └── services/            52 servicios de dominio
│       ├── authorization/   4      ├── order/        4
│       ├── user/            5      ├── invoice/      2
│       ├── seller/          2      ├── shipment/     3
│       ├── warehouse/       2      ├── returns/      2
│       ├── catalog/         7      ├── refund/       2
│       ├── inventory/       8      ├── reporting/    2
│       └── cart/            5      └── audit/        1
├── adapters/
│   ├── useCases/             6 implementaciones de puertos de entrada
│   ├── persistence/
│   │   ├── jpa/             47 (12 adaptadores + 14 entidades + 9 mappers + 11 repos)
│   │   └── mongodb/          4 (bitácora de auditoría)
│   └── rest/
│       ├── controllers/      7
│       ├── dtos/            36 (18 de entrada + 18 de salida)
│       ├── mappers/          6
│       └── exception/        1 GlobalExceptionHandler
└── infrastructure/
    ├── config/              14 configuraciones de beans de servicio
    ├── security/             9 (JWT, credenciales, filtro, ActorResolver)
    └── seed/                 1 (DatabaseSeeder: primer administrador)
```

---

## 4. Decisiones de arquitectura

### 4.1 Puertos de entrada organizados por rol, no por entidad

La especificación establece que cada usuario tiene un único rol (RG-02) y que ningún participante
administra información fuera de su rol (RG-03), con una matriz explícita de responsabilidades
(RD-ROL-06). Organizar los puertos por rol hace que esa matriz sea **estructural**: un comprador no
puede invocar una operación de vendedor porque esa operación ni siquiera existe en su interfaz.

### 4.2 Un servicio por caso de uso

En lugar de un `OrderService` con diez métodos, hay `ConfirmPaymentService`, `DispatchOrderService`,
`CompleteDeliveryService` y `ConsultOrderService`. Cada clase tiene una sola razón para cambiar, y
la trazabilidad con la documentación es 1:1.

### 4.3 Las reglas viven en el modelo; los servicios las orquestan

`Order` decide si una transición es válida; `InventoryItem` impide un saldo negativo; `Quantity`
hace imposible construir un número negativo. Los servicios no reimplementan esas reglas: deciden
**cuándo** se invocan y **quién** puede hacerlo, y verifican las precondiciones antes para lanzar
excepciones de dominio específicas (RD-SRV-06).

### 4.4 Los objetos de valor sustituyen a los primitivos

No hay `String status` ni `int quantity` en el dominio. Hay `OrderStatus`, `Quantity`, `Money`,
`EmailAddress`. Un objeto de valor inválido no puede llegar a existir, porque se valida
íntegramente en construcción (RD-VO-07).

### 4.5 Los identificadores los aporta quien invoca

Los servicios reciben el `Identifier` de las entidades nuevas como parámetro, igual que hacen los
constructores del modelo (RD-ID-01). La generación de identificadores es una decisión de
infraestructura y no contamina los puertos de salida.

---

## 5. Estado de implementación

| Capa | Archivos | Estado | Documento |
|---|---:|---|---|
| `domain/models` | 23 | Implementada | [Domain Model.md](../domain/Domain%20Model.md) |
| `domain/valuesObjects` | 37 | Implementada | [Domain Value Objects.md](../domain/Domain%20Value%20Objects.md) |
| `domain/exceptions` | 24 | Implementada | [Domain Services.md](../domain/Domain%20Services.md) §5 |
| `domain/ports/in` | 6 | Implementada | [Input-ports.md](../domain/Input-ports.md) |
| `domain/ports/out` | 13 | Implementada | [Output-ports.md](../domain/Output-ports.md) |
| `domain/services` | 52 | Implementada | [Domain Services.md](../domain/Domain%20Services.md) |
| `adapters/useCases` | 6 | Implementada | [Use-cases-adapters.md](../Adapters/Use-cases-adapters.md) |
| `adapters/persistence` | 51 | Implementada | [Persistence-adapters.md](../Adapters/Persistence-adapters.md) |
| `adapters/rest` | 53 | Implementada | [Rest-adapters.md](../Adapters/Rest-adapters.md) · [Api-rest-endpoints.md](../Adapters/Api-rest-endpoints.md) |
| `infrastructure/config` | 15 | Implementada | §5.1 |
| `infrastructure/security` | 10 | Implementada | [Security.md](../Infrastructure/Security.md) |
| `infrastructure/seed` | 1 | Implementada | [Bootstrap.md](../Infrastructure/Bootstrap.md) |
| Pruebas | 32 archivos / 189 tests | Implementadas | §6 |

**Total:** 292 archivos de producción (incluida la clase `NexusmarketApplication`).

### 5.1 Exposición de los servicios de dominio como beans

Los 52 servicios de dominio son clases Java planas, sin anotación alguna de framework: es
precisamente lo que mantiene el dominio libre de Spring (RD-SRV-02). Su cableado vive en 14 clases
`@Configuration` bajo `infrastructure/config`, una por área de servicio, con un `@Bean` por
servicio.

Spring resuelve el grafo por tipo, así que no hace falta ordenar las configuraciones ni usar
`@DependsOn`, aunque `AuditServicesConfig` sea consumida por cuatro de las otras.

---

## 6. Verificación

### 6.1 Desacoplamiento del dominio

El criterio objetivo de que la arquitectura se respeta es que este comando no devuelva ninguna
línea:

```bash
grep -rE "import (org\.springframework|jakarta\.persistence|org\.bson|com\.fasterxml|io\.jsonwebtoken)" \
  src/main/java/application/domain/
```

Si el dominio importara cualquiera de esos paquetes, la regla de dependencia estaría rota.

### 6.2 Ensamblaje del grafo de dependencias

Que el proyecto compile no prueba que Spring pueda resolver el grafo. `NexusmarketApplicationTests`
lo verifica explícitamente:

| Verificación | Qué asegura |
|---|---|
| El contexto arranca | El cableado completo es resoluble |
| 49 beans de servicio de dominio | Ninguna configuración olvidó un servicio |
| 13 puertos de salida resueltos | Cada puerto tiene exactamente un adaptador |
| 6 puertos de entrada resueltos | Cada rol tiene su caso de uso |

### 6.3 Cobertura de pruebas

| Suite | Tests | Qué cubre |
|---|---:|---|
| `InventoryServicesTest` | 17 | RD-INV-02/03/04: saldos negativos, existencias dañadas, movimientos |
| `OrderServicesTest` | 12 | RD-PED-01/02/05: ciclo secuencial, estado terminal, pertenencia |
| `CartServicesTest` | 11 | RD-PED-04, RD-CAT-03/05: el carrito no compromete nada |
| `CatalogServicesTest` | 12 | RD-CAT-01/04/05: propiedad, estado terminal, visibilidad |
| `AuditAndAfterSalesServicesTest` | 14 | RD-POS-01/02, RD-AUD-01: devoluciones, reembolsos, bitácora |
| `PersistenceReconstructionTest` | 6 | RD-ALC-04: los cuatro caminos de reconstrucción contra H2 |
| `JwtProviderTest` | 5 | Emisión y rechazo de tokens |

Las pruebas de dominio usan dobles en memoria escritos a mano
(`src/test/java/application/domain/support/`) en lugar de un framework de *mocking*: los fakes
almacenan de verdad lo que reciben, de modo que las aserciones hablan de resultados observables
—el saldo que cambió, la bitácora que se escribió— y no de qué método se llamó.

### 6.4 Verificación extremo a extremo

Ejercida manualmente contra MySQL 8.4 y MongoDB 7 en contenedor, con el flujo completo: administrador
sembrado → incorpora vendedor → vendedor registra producto y variante → el catálogo público pasa a
visible → comprador se registra (con carrito) → añade al carrito → confirma pedido → paga. Las
transiciones auditadas aparecieron en MongoDB, y las reglas rechazaron correctamente lo que debían
(descontinuar sin suspender → 409, rol cruzado → 403, producto suspendido al carrito → 409).

Esa prueba descubrió **cuatro defectos que las 77 pruebas automatizadas no detectaban**, porque
todas construían sus propios agregados y nunca preguntaban de dónde salía el primero:

| Defecto | Corrección |
|---|---|
| El comprador no tenía carrito y nada lo creaba: el flujo de compra era inalcanzable | `RegisterBuyerService` crea el carrito activo en el alta |
| No existía forma de crear el primer administrador | `DatabaseSeeder` ([Bootstrap.md](../Infrastructure/Bootstrap.md)) |
| Tres de los cinco roles no eran instanciables (`User` es abstracta) | `StaffUser` (RD-ROL-07) |
| La bitácora se escribía en la base `test` por defecto | `spring.mongodb.uri`, no `spring.data.mongodb.uri` |

### 6.5 Pendiente

- **Automatizar el flujo extremo a extremo.** Hoy está verificado manualmente; debería ser un test
  de integración para que no vuelva a romperse en silencio.
- **Test de `JwtAuthenticationFilter`** para el caso central de su diseño: un token válido de un
  usuario bloqueado después de emitirlo.
- **Áreas de servicio con cobertura parcial:** `user`, `seller`, `warehouse`, `invoice`, `shipment`,
  `reporting`.
