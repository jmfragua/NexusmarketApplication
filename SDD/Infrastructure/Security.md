# Seguridad — NexusMarket

**Paquete:** `application.infrastructure.security`

## 1. Introducción

La seguridad traduce una petición HTTP autenticada en un `User` de dominio activo, para que las
reglas RG-01 (toda operación la ejecuta un usuario autenticado) y RG-03 (nadie opera fuera de su
rol) puedan cumplirse en el borde de entrada.

## 1.1 Por qué vive en `infrastructure` y no en `adapters`

En el vocabulario de este código, **adapter** significa "implementa un puerto del dominio". La
seguridad no implementa ninguno: la especificación funcional excluye explícitamente los mecanismos
de autenticación técnica del alcance del dominio (RD-ALC-01), así que no existe —ni debe existir—
un `AuthenticationPort`.

Llamarla adapter sería deshonesto. Es cableado técnico transversal, y por eso vive junto a
`infrastructure/config`, que tampoco implementa puerto alguno.

---

## 2. El dominio no conoce las contraseñas

`User` no declara ningún atributo de credencial, y el Javadoc de `LoginService` lo dice
explícitamente: verificar el secreto corresponde al adaptador de seguridad.

Por eso las credenciales viven **enteramente fuera del dominio**:

```text
infrastructure/security/persistence/UserCredentialJpaEntity   email + hash BCrypt
infrastructure/security/persistence/UserCredentialRepository
infrastructure/security/CredentialService                     register / matches
```

**Por qué no es un puerto de salida.** Crear un `PasswordServicePort` bajo `domain/ports/out/`
violaría RD-POUT-01 (un puerto por agregado): una credencial no es un agregado del negocio, es un
detalle del mecanismo de acceso.

El reparto de responsabilidades en el login queda así:

| Responsabilidad | Dónde |
|---|---|
| ¿Quién es este usuario y puede operar? | `LoginService` (dominio, RG-01) |
| ¿La contraseña coincide? | `CredentialService` (infraestructura, RD-ALC-01) |
| ¿Qué token emito? | `JwtProvider` (infraestructura) |

---

## 3. Componentes

| Clase | Responsabilidad |
|---|---|
| `JwtProperties` | Enlaza `nexusmarket.security.jwt.*` (secreto, expiración) |
| `JwtProvider` | Emite y lee tokens firmados (HS256, jjwt 0.12.6) |
| `CredentialService` | Registra y verifica contraseñas con BCrypt |
| `DomainUserPrincipal` | Transporta el `User` de dominio por el contexto de Spring Security |
| `JwtAuthenticationFilter` | Autentica cada petición desde su bearer token |
| `SecurityConfig` | Cadena de filtros sin estado, rutas públicas, `PasswordEncoder` |
| `ActorResolver` | Reduce `User<?>` al subtipo concreto que exige el puerto |

---

## 4. Decisión clave: el actor se relee en cada petición

`JwtAuthenticationFilter` **no confía en los claims del token** para construir el usuario. Lee el
correo del token y vuelve a consultar el usuario en el repositorio:

```java
private Optional<User<?>> findActiveUser(String email) {
    return userRepositoryPort.findByEmail(EmailAddress.of(email))
            .filter(User::isActive);
}
```

**Por qué.** Un JWT sigue siendo estructuralmente válido hasta que expira. Si un administrador
bloquea a un usuario, ese usuario continuaría operando con su token vigente —y el dominio exige que
**solo un usuario activo pueda operar** (RG-01). Confiar en el claim rompería la regla en silencio
durante el tiempo restante de vida del token.

**Beneficio adicional.** La relectura devuelve el **subtipo concreto real** (`Buyer`, `Seller`),
porque el mapper de persistencia lo reconstruye desde el discriminador almacenado. Sin eso, haría
falta lógica aparte para resolver el subtipo que los puertos exigen.

**Coste.** Una consulta por petición autenticada. Aceptable en este alcance; la optimización
natural sería una caché de vida corta por correo, nunca confiar en el claim.

Si el usuario no existe o está bloqueado, **la autenticación simplemente no se establece**: la
petición continúa como anónima y las reglas de autorización la rechazan con `401`.

---

## 5. `ActorResolver`: estrechar el tipo, no convertirlo

Varios métodos de los puertos reciben `Buyer` o `Seller` en lugar de `User<?>`, porque la operación
solo tiene sentido para ese participante. El principal ya trae el subtipo real, así que esto es un
estrechamiento verificado:

```java
public static Buyer requireBuyer(User<?> actor) {
    if (actor instanceof Buyer buyer) {
        return buyer;
    }
    throw new UnauthorizedOperationException(
            "the authenticated user is not a buyer and cannot execute buyer operations");
}
```

Falla con la excepción de dominio correspondiente, que `GlobalExceptionHandler` traduce a `403`.

---

## 6. Configuración de la cadena de filtros

```java
http.csrf(disable)
    .sessionManagement(STATELESS)
    .authorizeHttpRequests(requests -> requests
        .requestMatchers("/api/v1/auth/**").permitAll()
        .requestMatchers(GET, "/api/v1/catalog/**").permitAll()
        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
        .anyRequest().authenticated())
    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

**Sin estado** porque la identidad viaja íntegra en el token; no hay sesión de servidor que
mantener.

**El catálogo abierto** es la única lectura fuera de RG-01, y es deliberada: la especificación
describe un catálogo público, y un visitante debe poder navegarlo antes de registrarse.

---

## 7. Orden de las operaciones en el registro

Tanto en `/auth/register` como en `/admin/sellers`, la credencial se guarda **después** de que el
dominio acepte al usuario:

```java
Buyer registered = publicAccessPort.registerBuyer(buyer);   // puede fallar por correo duplicado
credentialService.register(registered.getEmail(), request.password());
```

Invertir el orden dejaría contraseñas huérfanas de usuarios que nunca llegaron a existir. Es el
mismo criterio que RD-SRV-07 aplica a la bitácora de auditoría.

---

## 8. Configuración

| Propiedad | Defecto | Nota |
|---|---|---|
| `nexusmarket.security.jwt.secret` | valor de desarrollo | **Debe sustituirse en cualquier entorno real.** Mínimo 32 caracteres para HS256 |
| `nexusmarket.security.jwt.expiration-minutes` | `60` | — |

El valor por defecto del secreto es explícitamente de desarrollo y está nombrado como tal en el
archivo de propiedades. En despliegue se inyecta por variable de entorno `JWT_SECRET`.

---

## 9. Verificación

`JwtProviderTest` cubre lo que el proveedor debe aceptar y, sobre todo, lo que debe rechazar:

| Caso | Resultado esperado |
|---|---|
| Token propio | Devuelve el correo |
| Token firmado con otro secreto | Rechazado |
| Token expirado | Rechazado |
| Cadena basura, vacía o nula | Rechazado sin lanzar excepción |
| Firma manipulada | Rechazado |

**Pendiente:** no existe todavía un test del `JwtAuthenticationFilter` que cubra el caso central de
la decisión de §4 — un token válido de un usuario que fue bloqueado después de emitirlo.
