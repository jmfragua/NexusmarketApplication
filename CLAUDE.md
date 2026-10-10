# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

NexusMarket: a marketplace backend in Java 17 / Spring Boot 4.1.1, built with Maven, following
hexagonal architecture (Ports & Adapters) with a DDD core.

All layers are implemented: domain (entities, value objects, exceptions, ports, services),
persistence adapters (JPA for 12 aggregates, MongoDB for the audit log), REST adapters, use cases,
JWT security and the bootstrap seeder — 280 production files.

Run it with `docker compose up` (MySQL 8.4 + MongoDB 7 + the app), or point the app at those
containers locally. Bootstrapping needs `SEED_ADMIN_PASSWORD`: without it no administrator is
created and, since only buyers self-register, nothing else can be either — see
[SDD/Infrastructure/Bootstrap.md](SDD/Infrastructure/Bootstrap.md).

## Commands

```bash
./mvnw compile                                   # compile
./mvnw test                                      # run all tests
./mvnw test -Dtest=ClassName                     # run a single test class
./mvnw test -Dtest=ClassName#methodName          # run a single test method
./mvnw spring-boot:run                           # run the application
```

There is no linter configured. Lombok is used with annotation processing wired into both the
`default-compile` and `default-testCompile` executions of the compiler plugin — if a Lombok-derived
method (e.g. a `@Getter` accessor) isn't resolving, check that the IDE/build is running annotation
processing, not that the method is missing.

Tests run against H2 under the `test` profile (`src/test/resources/application-test.properties`),
so `./mvnw test` needs no database running. Mongo is never contacted — its driver connects lazily,
so the `Connection refused` lines in the output are expected noise, not failures.

## Domain model — read this before adding or changing anything under `domain/`

`SDD/` is the source of truth for every class under `src/main/java/application/domain/**`. Every
class has a 1:1 documented counterpart there — keep both sides in sync when adding, renaming, or
changing behavior. Documentation is written in Spanish; all code and its Javadoc are in English.

- [SDD/domain/Domain Model.md](SDD/domain/Domain%20Model.md) — entities, relationships, lifecycles,
  and numbered business rules (`RG-*` from the functional spec, `RD-*` design rules derived from it).
- [SDD/domain/Domain Value Objects.md](SDD/domain/Domain%20Value%20Objects.md) — value objects and
  enumerations, with their own `RD-VO-*` rule numbering.
- [SDD/domain/Domain Services.md](SDD/domain/Domain%20Services.md) — the 49 services, their design
  rules (`RD-SRV-*`) and the exception catalogue; one detail file per area under
  `SDD/domain/services/` (e.g. `inventory-services.md`), documenting each service's inputs,
  validations, processing and exceptions.
- [SDD/domain/Input-ports.md](SDD/domain/Input-ports.md) / [Output-ports.md](SDD/domain/Output-ports.md)
  — the 6 role-based input ports (`RD-PIN-*`) and 13 repository output ports (`RD-POUT-*`).
- [SDD/Software Architecture/Software Architecture.md](SDD/Software%20Architecture/Software%20Architecture.md)
  — layering, dependency rule and architecture decisions.
- `SDD/Adapters/` — use cases, persistence, REST and the HTTP contract;
  `SDD/Infrastructure/` — security and bootstrap.
- [enunciado-nexusmarket/especificacion_funcional_nexusmarket.md](enunciado-nexusmarket/especificacion_funcional_nexusmarket.md)
  — the original business spec (also kept as the source PDF in the same folder), which `SDD/domain/`
  was derived from.

Both documents state explicitly that the model excludes UI, mobile/web clients, technical auth
mechanisms, and any storage/architecture decisions — do not let those concerns leak into
`domain/models`, `domain/valuesObjects` or `domain/services`. When implementing new domain
behavior, find the rule number backing it in `SDD/` first; if a rule doesn't exist there, it
doesn't belong in the model.

### Package layout and conventions

```
application.domain.models          23 files: the abstract DomainEntity<ID> base + 22 entities
application.domain.valuesObjects   37 value objects and enumerations (ValueObject hierarchy)
application.domain.services        49 services, one per use case, in 14 area subpackages:
                                    authorization, user, seller, warehouse, catalog, inventory,
                                    cart, order, invoice, shipment, returns, refund, reporting,
                                    audit
application.domain.ports.in        6 interfaces, one per user role (PublicAccess, Buyer, Seller,
                                    LogisticOperator, Administrator, Supervisor)
application.domain.ports.out       13 interfaces, one per aggregate repository
application.domain.exceptions      20 exceptions, one per business rule, all extending
                                    DomainException
```

Entities, compared by identity:

```
DomainEntity<ID extends Identifier>  (abstract)
├── User (abstract) → Buyer, Seller, StaffUser
├── Warehouse (abstract) → MarketplaceWarehouse, SellerWarehouse
├── Product (abstract) → PhysicalProduct, DigitalProduct
├── ProductVariant
├── InventoryItem → InventoryMovement
├── Cart → CartItem
├── Order → OrderLine
├── Invoice, Shipment, ProductReturn, Refund
└── OperationLog   (audit extension — not from the functional spec, see SDD §4.22)
```

**`StaffUser` (RD-ROL-07).** `User` is abstract, so the three roles with no specialization of their
own — logistics operator, administrator, supervisor — need a concrete class to exist at all. It adds
no attribute and its factory refuses `BUYER` and `SELLER`, which have their own creation rules.

**Reconstruction factories (RD-ALC-04).** `Order`, `Seller`, `Product`/`PhysicalProduct`/
`DigitalProduct`, `InventoryItem` and `OperationLog` expose a `reconstruct(...)` static factory used
*only* by persistence adapters to restore a stored aggregate. Their regular constructors enforce
creation-time rules (an order is born `PENDING_PAYMENT`, a product `PUBLISHED`, a seller needs an
administrator and a first warehouse) that would falsify the state of an entity being reloaded, or
fabricate inventory movements that never happened. Never call `reconstruct(...)` from a domain
service — it bypasses those rules by design.

Value objects, compared by value, all immutable:

```
ValueObject  (abstract)
├── Identifier → one specialization per entity type (UserId, OrderId, ProductId, ...)
├── FullName, EmailAddress, IdentityDocument, Address, Money, Quantity, VariantAttribute
└── Enumeration  (interface, implemented by Java enums)
    ├── UserRole, UserStatus, BuyerCommercialStatus, WarehouseType
    ├── ProductType, ProductStatus, StockCondition, InventoryMovementType, OrderStatus
```

Key conventions enforced throughout, and expected in any new code:

- **`DomainEntity<ID>`** takes its identifier type as a generic parameter so identifiers of different
  entities are never interchangeable. `equals`/`hashCode` are `final`, based solely on `identifier`.
- **`ValueObject`** provides `final equals`/`hashCode` built from `equalityComponents()`, which every
  subclass must implement. Validation happens entirely in constructors — an invalid instance must
  never be constructible (RD-VO-07). "Mutating" a value object always returns a new instance.
- **`Enumeration`** is an interface (Java forbids `enum X extends Y`), giving every catalogue enum a
  `code()`/`description()` contract plus, where the domain doc defines a lifecycle, transition guard
  methods (e.g. `OrderStatus.canTransitionTo(target)`, `isTerminal()`, `isModifiable()`).
- Sequential lifecycles (`Order`, `Product`, `InventoryItem`, `UserStatus`, `BuyerCommercialStatus`)
  must reject skipped or backward transitions the way `Order.changeStatus` /
  `OrderStatus.canTransitionTo` do — check the relevant `RD-*` rule and lifecycle diagram in
  `SDD/domain/Domain Model.md` §5 before adding a new transition.
- Entities expose ownership/authorization query methods (`belongsTo`, `ownsProduct`, `ownsWarehouse`,
  `canOperateOn`) rather than exposing raw ID fields for callers to compare — this is how RG-03 (no
  participant may act outside its role/ownership) is meant to be enforced from the domain itself.
- Collections exposed by entities (e.g. `Order.getLines()`) return unmodifiable views; mutation goes
  through explicit methods that maintain invariants (e.g. no negative stock, no modifying a finished
  order).
- **Services orchestrate, entities decide.** A service never reimplements a rule the model already
  enforces; it validates the actor's role and the resource ownership, checks preconditions
  (`isModifiable()`, `canTransitionTo()`, `canReserve()`) so it can throw a specific
  `domain/exceptions/` type instead of leaking a generic one, then calls the model and persists.
- **Nothing from a framework may be imported under `domain/`.** Verify with:
  `grep -rE "import (org\.springframework|jakarta\.persistence|org\.bson|com\.fasterxml)" src/main/java/application/domain/`
