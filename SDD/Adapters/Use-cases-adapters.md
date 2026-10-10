# Adaptadores de Casos de Uso — NexusMarket

**Paquete:** `application.adapters.useCases`

## 1. Introducción

Los **casos de uso** son las clases concretas que implementan los puertos de entrada
(`domain/ports/in/`). Son la costura entre el framework y el dominio: Spring los descubre como
beans (`@Service`), y hacia adentro delegan en los servicios de dominio, que son Java puro.

Hay exactamente **seis**, uno por puerto de entrada, es decir uno por rol del sistema.

---

## 2. Principio de diseño

- **RD-UC-01.** Un caso de uso **no contiene regla de negocio alguna**. Cada método es una
  delegación de una línea al servicio de dominio que posee la regla. Si un método de un caso de uso
  crece más allá de eso, es señal de que falta un servicio de dominio.
- **RD-UC-02.** Los casos de uso inyectan **servicios de dominio** para todo lo que decide algo.
  Inyectan además puertos de salida para **resolver entidades por identificador** (RD-PIN-06) y para
  las tres operaciones de `BuyerPort` que no tienen servicio detrás (§4). Esa es la frontera: un
  puerto de salida en un caso de uso resuelve o guarda; nunca sustituye a un servicio que posee una
  regla. El caso de uso es el lugar legítimo para esto, porque está del lado del dominio en la
  jerarquía de capas; un controlador no lo está (RD-REST-02).
- **RD-UC-03.** Son el único lugar anotado con `@Service` en toda la ruta que va del controlador al
  modelo: los servicios de dominio se exponen como beans desde `infrastructure/config`, nunca con
  anotaciones propias.

---

## 3. Catálogo de casos de uso

| Clase | Implementa | Servicios de dominio | Puertos de salida (resolución) |
|---|---|---|---|
| `PublicAccessUseCaseImpl` | `PublicAccessPort` | 3 | — |
| `BuyerUseCaseImpl` | `BuyerPort` | 11 | 2 |
| `SellerUseCaseImpl` | `SellerPort` | 14 | 2 |
| `LogisticOperatorUseCaseImpl` | `LogisticOperatorPort` | 12 | 5 |
| `AdministratorUseCaseImpl` | `AdministratorPort` | 11 | 3 |
| `SupervisorUseCaseImpl` | `SupervisorPort` | 4 | — |

Los puertos de salida de la última columna cubren exactamente la resolución por identificador que
exige RD-PIN-06. `LogisticOperatorUseCaseImpl` necesita cinco porque las sobrecargas de envío
resuelven pedido, envío, producto, registro de stock y devolución, y además **deciden**: abrir el
envío si no existe y recopilar los productos del pedido para determinar si requiere logística
(RD-LOG-01). Esa decisión estuvo un tiempo en el controlador, que es lo que RD-REST-02 prohíbe.

### 3.1 Ejemplo del patrón

```java
@Service
public class SellerUseCaseImpl implements SellerPort {

    private final PublishProductService publishProductService;
    // ... resto de servicios inyectados por constructor

    @Override
    public Product publishProduct(Seller seller, Product product) {
        return publishProductService.publish(seller, product);
    }
}
```

Nótese que el nombre del método del servicio **no coincide** con el del puerto
(`publishProduct` → `publish`). Los servicios se nombran por su operación dentro de su área; los
puertos, por cómo la ve el rol. El caso de uso es quien traduce entre ambos vocabularios.

---

## 4. Excepción documentada: los tres métodos sin servicio

`BuyerPort` declara tres operaciones que **no tienen un servicio de dominio detrás**:

| Operación | Por qué no tiene servicio |
|---|---|
| `addAdditionalAddress` | Mutación delgada de `Buyer`; no existe regla `RD-SRV` propia |
| `changePrimaryAddress` | Ídem; la regla de no perder la dirección anterior vive en el modelo |
| `consultMyCart` | Lectura directa del carrito del comprador autenticado |

`BuyerUseCaseImpl` las orquesta contra `UserRepositoryPort` y `CartRepositoryPort` directamente,
pero **sin saltarse la validación de rol**:

```java
@Override
public Buyer addAdditionalAddress(Buyer buyer, Address address) {
    validateRoleAuthorizationService.validate(buyer, ADDRESS_OPERATION, UserRole.BUYER);
    buyer.addAdditionalAddress(address);
    return userRepositoryPort.save(buyer);
}
```

La alternativa —crear tres servicios de dominio que solo delegaran en el modelo— habría añadido
ceremonia sin añadir ninguna regla. Queda documentado aquí para que la ausencia sea deliberada y no
parezca un olvido.

---

## 5. Trazabilidad caso de uso ↔ puerto de entrada

| Caso de uso | Puerto | Documento del puerto |
|---|---|---|
| `PublicAccessUseCaseImpl` | `PublicAccessPort` | [Input-ports.md](../domain/Input-ports.md) §3.1 |
| `BuyerUseCaseImpl` | `BuyerPort` | [Input-ports.md](../domain/Input-ports.md) §3.2 |
| `SellerUseCaseImpl` | `SellerPort` | [Input-ports.md](../domain/Input-ports.md) §3.3 |
| `LogisticOperatorUseCaseImpl` | `LogisticOperatorPort` | [Input-ports.md](../domain/Input-ports.md) §3.4 |
| `AdministratorUseCaseImpl` | `AdministratorPort` | [Input-ports.md](../domain/Input-ports.md) §3.5 |
| `SupervisorUseCaseImpl` | `SupervisorPort` | [Input-ports.md](../domain/Input-ports.md) §3.6 |
