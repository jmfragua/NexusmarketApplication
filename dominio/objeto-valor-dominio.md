# Objetos de Valor del Dominio — NexusMarket

## 1. Introducción

Un **objeto de valor** (*value object*) es un concepto del dominio que se define exclusivamente por
sus atributos y **no posee identidad propia**. Dos objetos de valor con los mismos atributos son
indistinguibles e intercambiables, a diferencia de las entidades descritas en `modelo-dominio.md`,
que se comparan por identificador.

En NexusMarket los objetos de valor cumplen tres propósitos:

- **Encapsular restricciones de la especificación.** La unicidad del correo electrónico, la
  obligatoriedad de un nombre no vacío o la prohibición de cantidades negativas dejan de ser
  validaciones dispersas y pasan a ser condiciones que el propio tipo garantiza al construirse.
- **Eliminar tipos primitivos ambiguos.** Un `String` puede contener cualquier cosa; un
  `EmailAddress` solo puede contener un correo válido. Un número suelto puede ser negativo; una
  `Quantity` no.
- **Representar los catálogos definidos del negocio.** Los estados de usuario, de producto, de
  pedido y los tipos de movimiento de inventario son conjuntos cerrados de valores; se modelan como
  enumeraciones para impedir estados no contemplados por la especificación.

Todos los objetos de valor de este documento son **inmutables**: una modificación produce una
instancia nueva, nunca altera la existente.

---

## 2. Jerarquía de objetos de valor

```
ValueObject  (abstracta — inmutable, comparación por valor)
│
├── Identifier
│   ├── UserId
│   ├── BuyerId
│   ├── SellerId
│   ├── WarehouseId
│   ├── ProductId
│   ├── ProductVariantId
│   ├── InventoryItemId
│   ├── InventoryMovementId
│   ├── CartId
│   ├── CartItemId
│   ├── OrderId
│   ├── OrderLineId
│   ├── InvoiceId
│   ├── ShipmentId
│   ├── ProductReturnId
│   └── RefundId
│
├── FullName
├── EmailAddress
├── IdentityDocument
├── Address
├── Money
├── Quantity
├── VariantAttribute
│
└── Enumeration  (abstracta — catálogos definidos del negocio)
    ├── UserRole
    ├── UserStatus
    ├── BuyerCommercialStatus
    ├── WarehouseType
    ├── ProductType
    ├── ProductStatus
    ├── StockCondition
    ├── InventoryMovementType
    └── OrderStatus
```

---

## 3. Objetos de valor compuestos

### 3.1 ValueObject

**Descripción.** Clase base abstracta de todo objeto de valor. Define el contrato común: inmutable
tras su construcción, sin identificador propio y comparable por la totalidad de sus atributos.

**Hereda de.** — (raíz de la jerarquía de objetos de valor)

| Atributo | Tipo | Descripción |
|---|---|---|
| — | — | No define atributos propios; establece el contrato de igualdad por valor e inmutabilidad. |

**Reglas.**

- Se valida por completo en la construcción: una instancia inválida no puede existir.
- No expone operaciones de modificación; cualquier cambio devuelve una instancia nueva.

---

### 3.2 Identifier

**Descripción.** Identificador único de una entidad dentro de su tipo. La especificación establece
que el identificador de usuario es obligatorio y único; el mismo concepto se aplica al resto de
entidades del modelo.

**Hereda de.** `ValueObject`

| Atributo | Tipo | Descripción |
|---|---|---|
| `value` | `String` | Valor del identificador. Obligatorio, no vacío. |

**Valores permitidos.**

| Condición | Descripción |
|---|---|
| No nulo | El identificador siempre debe estar presente. |
| No vacío | No se admiten cadenas en blanco. |
| Único por tipo de entidad | Dos entidades del mismo tipo no comparten identificador. |

**Especializaciones.** `UserId`, `BuyerId`, `SellerId`, `WarehouseId`, `ProductId`,
`ProductVariantId`, `InventoryItemId`, `InventoryMovementId`, `CartId`, `CartItemId`, `OrderId`,
`OrderLineId`, `InvoiceId`, `ShipmentId`, `ProductReturnId`, `RefundId`.

Cada especialización es un tipo distinto: un `OrderId` no puede usarse donde se espera un
`ProductId`, aunque ambos envuelvan una cadena.

---

### 3.3 FullName

**Descripción.** Nombre oficial del usuario. Atributo obligatorio del Dominio 1 con restricción de
"no vacío".

**Hereda de.** `ValueObject`

| Atributo | Tipo | Descripción |
|---|---|---|
| `value` | `String` | Nombre completo oficial del usuario. |

**Valores permitidos.**

| Condición | Descripción |
|---|---|
| Obligatorio | Todo usuario debe tener nombre completo. |
| No vacío | No se admite una cadena en blanco ni compuesta solo de espacios. |

---

### 3.4 EmailAddress

**Descripción.** Medio principal de acceso y comunicación del usuario. Atributo obligatorio del
Dominio 1 con restricción de unicidad.

**Hereda de.** `ValueObject`

| Atributo | Tipo | Descripción |
|---|---|---|
| `value` | `String` | Dirección de correo electrónico del usuario. |

**Valores permitidos.**

| Condición | Descripción |
|---|---|
| Obligatorio | Todo usuario debe tener correo electrónico. |
| Único | El correo electrónico debe ser único en toda la plataforma (Validación Crítica). |
| Formato válido | Debe corresponder a una dirección de correo bien formada. |

---

### 3.5 IdentityDocument

**Descripción.** Documento de identidad del usuario. La especificación exige que sea único en la
plataforma, junto con el correo electrónico.

**Hereda de.** `ValueObject`

| Atributo | Tipo | Descripción |
|---|---|---|
| `value` | `String` | Número o código del documento de identidad. |

**Valores permitidos.**

| Condición | Descripción |
|---|---|
| No vacío | No se admite documento en blanco. |
| Único | El documento de identidad debe ser único en toda la plataforma (Validación Crítica). |

---

### 3.6 Address

**Descripción.** Ubicación de entrega del comprador (Dominio 2). Se utiliza tanto como dirección
principal —obligatoria— como en la lista de direcciones adicionales —opcional—, y viaja al pedido y
al envío como dirección de entrega.

**Hereda de.** `ValueObject`

| Atributo | Tipo | Descripción |
|---|---|---|
| `value` | `String` | Descripción de la ubicación de entrega. |

**Valores permitidos.**

| Condición | Descripción |
|---|---|
| No vacía | Una dirección registrada no puede estar en blanco. |
| Obligatoria como principal | Todo comprador debe tener una dirección principal. |
| Opcional como adicional | Las direcciones adicionales no son obligatorias. |

**Nota.** La especificación describe la dirección como "ubicación habitual para entregas" sin
desglosarla en componentes (calle, ciudad, país). Se modela como un valor único y no se añaden
campos no contemplados.

---

### 3.7 Money

**Descripción.** Valor comercial asociado a una venta. Se utiliza en el importe de las líneas del
pedido, el total del pedido, el total facturado y el monto reembolsado.

**Hereda de.** `ValueObject`

| Atributo | Tipo | Descripción |
|---|---|---|
| `amount` | `Decimal` | Importe monetario. |
| `currency` | `String` | Moneda en la que se expresa el importe. |

**Valores permitidos.**

| Condición | Descripción |
|---|---|
| No negativo | Un valor comercial no puede ser negativo. |
| Moneda obligatoria | Todo importe está expresado en una moneda. |
| Operaciones homogéneas | Solo se suman o comparan importes de la misma moneda. |

**Operaciones.**

- `add(money): Money`
- `subtract(money): Money`
- `multiply(quantity): Money`
- `isGreaterThan(money): boolean`

---

### 3.8 Quantity

**Descripción.** Cantidad de unidades manejada por el dominio: existencias disponibles, existencias
reservadas, cantidad de un movimiento de inventario, cantidad seleccionada en el carrito o
solicitada en una línea de pedido.

**Hereda de.** `ValueObject`

| Atributo | Tipo | Descripción |
|---|---|---|
| `value` | `Integer` | Número de unidades. |

**Valores permitidos.**

| Condición | Descripción |
|---|---|
| Mayor o igual a cero | **No se permitirán existencias negativas bajo ninguna circunstancia** (Dominio 6). |
| Mayor que cero en operaciones | Un movimiento, una línea de carrito o de pedido no puede ser de cero unidades. |
| Entera | Las unidades no admiten fracciones. |

**Operaciones.**

- `add(quantity): Quantity`
- `subtract(quantity): Quantity` — falla si el resultado sería negativo.
- `isGreaterThanOrEqual(quantity): boolean` — soporte de la validación de reserva.

---

### 3.9 VariantAttribute

**Descripción.** Diferencia concreta que caracteriza a una variante de producto: color, talla,
modelo, etc. (Dominio 5). Las variantes se declaran como una lista de estas diferencias.

**Hereda de.** `ValueObject`

| Atributo | Tipo | Descripción |
|---|---|---|
| `name` | `String` | Nombre de la característica (color, talla, modelo, etc.). |
| `value` | `String` | Valor concreto de la característica. |

**Valores permitidos.**

| Condición | Descripción |
|---|---|
| Nombre no vacío | La característica debe estar identificada. |
| Valor no vacío | La característica debe tener un valor concreto. |
| Nombre no repetido | Una variante no declara dos veces la misma característica. |

---

## 4. Enumeraciones primitivas

Las enumeraciones representan los **catálogos definidos** del negocio: conjuntos cerrados de
valores fuera de los cuales no existe estado válido en el dominio.

### 4.1 UserRole

**Descripción.** Define las responsabilidades y permisos del usuario (Dominio 1). Cada participante
desempeña un único rol y solo puede interactuar con la información correspondiente a sus funciones.

**Hereda de.** `Enumeration`

| Valor | Descripción |
|---|---|
| `BUYER` | Comprador. Persona que adquiere productos publicados. |
| `SELLER` | Vendedor. Responsable de registrar y administrar sus productos. |
| `LOGISTICS_OPERATOR` | Operador Logístico. Encargado de la operación física de bodegas y despachos. |
| `ADMINISTRATOR` | Administrador. Responsable de la administración de vendedores y bodegas. |
| `SUPERVISOR` | Supervisor. Perfil de consulta y seguimiento operativo. |

**Reglas.** Obligatorio y único por usuario (RG-02). Ningún participante podrá administrar
información fuera de su rol (RG-03).

---

### 4.2 UserStatus

**Descripción.** Condición operativa del usuario (Dominio 1). Corresponde a un catálogo definido
por el negocio.

**Hereda de.** `Enumeration`

| Valor | Descripción |
|---|---|
| `ACTIVE` | Activo. El usuario puede operar en la plataforma. |
| `BLOCKED` | Bloqueado. El usuario no puede ejecutar operaciones. |

**Ciclo de vida.**

```
    ┌──────────┐    block()     ┌───────────┐
    │  ACTIVE  │───────────────▶│  BLOCKED  │
    └──────────┘◀───────────────└───────────┘
                   activate()
```

**Nota.** La especificación enuncia el catálogo como "Activo, Bloqueado, etc.", por lo que se trata
de un catálogo definido por el negocio y ampliable; aquí se recogen únicamente los valores
nombrados de forma explícita.

---

### 4.3 BuyerCommercialStatus

**Descripción.** Condición del comprador para realizar compras (Dominio 2). Atributo obligatorio del
comprador.

**Hereda de.** `Enumeration`

| Valor | Descripción |
|---|---|
| `ENABLED` | Habilitado. El comprador puede realizar compras. |
| `DISABLED` | Inhabilitado. El comprador no puede realizar compras. |

**Ciclo de vida.**

```
    ┌───────────┐    disable()    ┌────────────┐
    │  ENABLED  │────────────────▶│  DISABLED  │
    └───────────┘◀────────────────└────────────┘
                     enable()

    Solo un comprador ENABLED puede confirmar un pedido.
```

**Nota.** La especificación define el atributo como "condición del comprador para realizar
compras", sin enumerar sus valores; se modelan los dos estados implicados por esa definición.

---

### 4.4 WarehouseType

**Descripción.** Clasificación de las bodegas (Dominio 4). Distingue los espacios físicos de
almacenamiento según su titularidad.

**Hereda de.** `Enumeration`

| Valor | Descripción |
|---|---|
| `MARKETPLACE` | Bodega del Marketplace, operada por la plataforma. |
| `SELLER` | Bodega de un Vendedor, asociada a su titular. |

**Reglas.** El tipo determina la especialización de la entidad `Warehouse` y no cambia durante su
vida.

---

### 4.5 ProductType

**Descripción.** Naturaleza del producto en el catálogo (Dominio 5). Determina el comportamiento
operativo del producto en todo el dominio.

**Hereda de.** `Enumeration`

| Valor | Descripción |
|---|---|
| `PHYSICAL` | Producto físico. Requiere inventario y despacho. |
| `DIGITAL` | Producto digital. Entrega inmediata tras el pago. |

**Reglas.** El tipo se define al registrar el producto y no cambia durante su vida.

---

### 4.6 ProductStatus

**Descripción.** Estado del producto dentro del catálogo (Dominio 5).

**Hereda de.** `Enumeration`

| Valor | Descripción |
|---|---|
| `PUBLISHED` | Publicado. Visible en el catálogo público y comercializable. |
| `SUSPENDED` | Suspendido. Temporalmente retirado de la comercialización. |
| `DISCONTINUED` | Descontinuado. Retirado de forma definitiva. |

**Ciclo de vida.**

```
        ┌───────────────┐
        │   PUBLISHED   │◀────────────┐
        └───────┬───────┘             │
                │ suspend()           │ publish()
                ▼                     │
        ┌───────────────┐             │
        │   SUSPENDED   │─────────────┘
        └───────┬───────┘
                │ discontinue()
                ▼
        ┌────────────────┐
        │  DISCONTINUED  │  ── TERMINAL ──
        └────────────────┘
```

---

### 4.7 StockCondition

**Descripción.** Condición de las existencias en bodega. La especificación identifica de forma
explícita el marcado como "Dañado" como impedimento para reservar inventario.

**Hereda de.** `Enumeration`

| Valor | Descripción |
|---|---|
| `AVAILABLE` | Disponible. Las existencias pueden reservarse y venderse. |
| `DAMAGED` | Dañado. Las existencias quedan excluidas de toda reserva. |

**Ciclo de vida.**

```
    ┌─────────────┐    markAsDamaged()    ┌────────────┐
    │  AVAILABLE  │──────────────────────▶│   DAMAGED  │
    └─────────────┘                       └────────────┘

    No se puede reservar inventario inexistente o marcado como DAMAGED.
```

---

### 4.8 InventoryMovementType

**Descripción.** Tipo de movimiento que afecta a las existencias (Dominio 6). Conjunto cerrado de
cinco movimientos definidos por el negocio.

**Hereda de.** `Enumeration`

| Valor | Descripción |
|---|---|
| `INBOUND` | Ingreso. Registro de existencias en la bodega. |
| `RESERVATION` | Reserva. Compromiso de existencias para un pedido. |
| `SALE_OUTBOUND` | Salida por venta. Salida física de existencias por un pedido despachado. |
| `ADJUSTMENT` | Ajuste. Corrección de las existencias registradas. |
| `RETURN` | Devolución. Reintegro de existencias procedente de una devolución. |

**Reglas.** Ningún cambio de existencias puede producirse sin un movimiento de uno de estos cinco
tipos. Ninguno de ellos puede dejar el saldo en negativo.

---

### 4.9 OrderStatus

**Descripción.** Estado del pedido dentro de su ciclo de vida (Dominio 7). Es el catálogo central
del sistema: representa el avance del compromiso comercial formal.

**Hereda de.** `Enumeration`

| Valor | Descripción |
|---|---|
| `CART` | Carrito. Selección provisional de productos. |
| `PENDING_PAYMENT` | Pendiente de Pago. Espera de confirmación financiera. |
| `PAID` | Pagado. Inicio de procesos de alistamiento. |
| `DISPATCHED` | Despachado. Salida física de la bodega. |
| `DELIVERED` | Entregado / Finalizado. Conclusión satisfactoria de la entrega. |

**Ciclo de vida.**

```
   ┌────────┐      ┌──────────────────┐      ┌────────┐
   │  CART  │─────▶│ PENDING_PAYMENT  │─────▶│  PAID  │
   └────────┘      └──────────────────┘      └───┬────┘
                                                 │
                    ┌────────────────────────────┘
                    ▼
            ┌──────────────┐      ┌───────────────┐
            │  DISPATCHED  │─────▶│   DELIVERED   │  ── TERMINAL ──
            └──────────────┘      └───────────────┘     inmodificable
```

**Reglas.** El ciclo es secuencial: no se admiten saltos ni retrocesos. `DELIVERED` es terminal; un
pedido finalizado no podrá ser modificado bajo ninguna circunstancia.

---

## 5. Reglas de diseño de objetos de valor

### 5.1 Identidad y comparación

- **RD-VO-01.** Un objeto de valor no tiene identificador propio y se compara por la totalidad de
  sus atributos. Dos instancias con los mismos valores son intercambiables.
- **RD-VO-02.** `Identifier` y sus especializaciones son objetos de valor, aunque **representen** la
  identidad de una entidad: el identificador en sí se compara por valor.
- **RD-VO-03.** Cada tipo de entidad tiene su propia especialización de `Identifier`, de modo que
  identificadores de entidades distintas no sean intercambiables entre sí.

### 5.2 Inmutabilidad

- **RD-VO-04.** Todo objeto de valor es inmutable: sus atributos se fijan en la construcción y no
  se modifican después.
- **RD-VO-05.** Toda operación que "modifica" un objeto de valor devuelve una instancia nueva
  (`Quantity.add`, `Money.subtract`), sin alterar la original.
- **RD-VO-06.** Los objetos de valor pueden compartirse libremente entre entidades sin riesgo de
  efectos colaterales.

### 5.3 Validación

- **RD-VO-07.** La validación ocurre íntegramente en la construcción: **una instancia inválida no
  puede llegar a existir**. No se admiten objetos de valor "a medio validar".
- **RD-VO-08.** Las restricciones de la especificación viven en el objeto de valor, no en la capa
  que lo invoca: `EmailAddress` valida el formato, `Quantity` impide el valor negativo, `FullName`
  impide la cadena vacía.
- **RD-VO-09.** Las restricciones que dependen del conjunto de datos —la unicidad del correo
  electrónico y del documento de identidad en toda la plataforma— no pueden verificarse dentro del
  objeto de valor; este garantiza la validez del formato, y la unicidad se comprueba al incorporar
  el usuario al dominio.

### 5.4 Enumeraciones

- **RD-VO-10.** Todo atributo cuyo valor provenga de un catálogo definido por el negocio se modela
  como enumeración, nunca como cadena libre.
- **RD-VO-11.** Las enumeraciones son conjuntos cerrados: un valor fuera del catálogo es un estado
  inválido del dominio y debe rechazarse.
- **RD-VO-12.** Las enumeraciones con ciclo de vida (`UserStatus`, `BuyerCommercialStatus`,
  `ProductStatus`, `StockCondition`, `OrderStatus`) declaran explícitamente sus transiciones
  permitidas; toda transición no declarada está prohibida.
- **RD-VO-13.** Los estados terminales (`DISCONTINUED` en `ProductStatus`, `DELIVERED` en
  `OrderStatus`) no admiten transición de salida alguna.
- **RD-VO-14.** Las enumeraciones que clasifican de forma permanente (`WarehouseType`,
  `ProductType`) se fijan al crear la entidad y no cambian durante su vida.

### 5.5 Uso en el modelo

- **RD-VO-15.** Los atributos del dominio no se representan con tipos primitivos cuando existe un
  objeto de valor que expresa su significado y sus restricciones.
- **RD-VO-16.** Los objetos de valor no contienen referencias a entidades: expresan valores, no
  relaciones.
- **RD-VO-17.** Solo se modelan los objetos de valor derivados de la especificación funcional; no
  se introducen conceptos ausentes del documento de negocio.
