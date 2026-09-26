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
   │   domain/services        48 servicios, uno por caso de uso                 │
   │   domain/exceptions      Una excepción por regla de negocio                │
   │                                                                            │
   │              Cero dependencias de framework. Java puro.                    │
   └─────────────────────────────────────┬─────────────────────────────────────┘
                                         │ depende de
                    ┌────────────────────▼─────────────────────┐
                    │           domain/ports/out               │
                    │   12 interfaces de repositorio            │
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
└── domain/
    ├── models/              22 entidades
    ├── valuesObjects/       35 objetos de valor y enumeraciones
    ├── exceptions/          20 excepciones de dominio
    ├── ports/
    │   ├── in/               6 puertos de entrada (uno por rol)
    │   └── out/             12 puertos de salida (uno por agregado)
    └── services/            48 servicios de dominio
        ├── authorization/   4
        ├── user/            5
        ├── seller/          2
        ├── warehouse/       2
        ├── catalog/         7
        ├── inventory/       8
        ├── cart/            5
        ├── order/           4
        ├── invoice/         2
        ├── shipment/        3
        ├── returns/         2
        ├── refund/          2
        └── reporting/       2
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

| Capa | Estado |
|---|---|
| `domain/models` | Implementada |
| `domain/valuesObjects` | Implementada |
| `domain/exceptions` | Implementada |
| `domain/ports/in` | Implementada |
| `domain/ports/out` | Implementada |
| `domain/services` | Implementada |
| `adapters/useCases` | Pendiente |
| `adapters/rest` | Pendiente |
| `adapters/persistence` | Pendiente |
| `infrastructure/security` | Pendiente |
| Pruebas unitarias | Pendientes |

---

## 6. Verificación del desacoplamiento

El criterio objetivo de que la arquitectura se respeta es que este comando no devuelva ninguna
línea:

```bash
grep -rE "import (org\.springframework|jakarta\.persistence|org\.bson|com\.fasterxml)" \
  src/main/java/application/domain/
```

Si el dominio importara cualquiera de esos paquetes, la regla de dependencia estaría rota.
