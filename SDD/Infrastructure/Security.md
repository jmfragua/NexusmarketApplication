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
| `SecurityConfig` | Cadena de filtros sin estado, rutas públicas, reglas de rol por prefijo, `PasswordEncoder` |
| `SecurityErrorResponder` | Responde los rechazos de la cadena con el cuerpo `{status, rule, message}`: 401 y 403 |
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
petición continúa como anónima y `SecurityErrorResponder` la rechaza con `401` (§6.1).

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
    .cors(cors -> cors.configurationSource(corsConfigurationSource))
    .sessionManagement(STATELESS)
    .exceptionHandling(errors -> errors
        .authenticationEntryPoint(securityErrorResponder)   // 401
        .accessDeniedHandler(securityErrorResponder))       // 403
    .authorizeHttpRequests(requests -> requests
        .requestMatchers("/api/v1/auth/**").permitAll()
        .requestMatchers(GET, "/api/v1/catalog/**").permitAll()
        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
        .requestMatchers("/api/v1/buyer/**").hasRole("BUYER")
        .requestMatchers("/api/v1/seller/**").hasRole("SELLER")
        .requestMatchers("/api/v1/logistics/**").hasRole("LOGISTICS_OPERATOR")
        .requestMatchers("/api/v1/admin/**").hasRole("ADMINISTRATOR")
        .requestMatchers("/api/v1/supervisor/**").hasRole("SUPERVISOR")
        .anyRequest().authenticated())
    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

**Sin estado** porque la identidad viaja íntegra en el token; no hay sesión de servidor que
mantener.

**El catálogo abierto** es la única lectura fuera de RG-01, y es deliberada: la especificación
describe un catálogo público, y un visitante debe poder navegarlo antes de registrarse.

**La documentación abierta** (`/swagger-ui.html`, `/v3/api-docs`) describe el contrato, no expone
datos. Si se cerrara, nadie podría consultar cómo autenticarse para poder abrirla.

**Reglas de rol por prefijo: defensa en profundidad.** Cada prefijo del API pertenece a un único
rol, y la cadena lo cierra a los demás antes de llegar al controlador. No sustituye a la validación
del dominio —`ValidateRoleAuthorizationService` y las de pertenencia siguen ejecutándose en cada
operación (RD-SRV-05)—: si una regla de URL se configurara mal, el dominio seguiría rechazando. La
autoridad la aporta `DomainUserPrincipal.authority()` como `ROLE_<rol>`.

---

## 6.1 Respuestas de error de la cadena: 401 y 403 con cuerpo

Sin manejadores propios, Spring Security responde a una petición sin autenticación válida con un
`403` **sin cuerpo**. Eso contradice el contrato del API y deja al cliente sin forma de distinguir
una sesión expirada de un rol insuficiente: un token vencido nunca se reconocería como tal.

`SecurityErrorResponder` implementa `AuthenticationEntryPoint` y `AccessDeniedHandler` y responde
con el mismo cuerpo que `GlobalExceptionHandler` usa para las reglas del dominio:

| Situación | Estado | `rule` |
|---|---|---|
| Sin token, token mal formado, firmado con otro secreto o expirado | `401` | `AuthenticationRequired` |
| Token válido de un usuario que ya no existe o está bloqueado (§4) | `401` | `AuthenticationRequired` |
| Usuario autenticado cuyo rol no corresponde al prefijo de la ruta | `403` | `AccessDenied` |
| Usuario autenticado que viola una regla de rol o pertenencia del dominio | `403` | la excepción de dominio, p. ej. `ProductNotOwnedException` |

```json
{ "status": 401, "rule": "AuthenticationRequired", "message": "authentication is required: ..." }
```

- **RD-SEC-01.** Toda respuesta de error del API, también las que produce la cadena de seguridad,
  lleva el cuerpo `{status, rule, message}`.
- **RD-SEC-02.** `401` significa "no sé quién eres" y `403` "sé quién eres y no puedes". Un usuario
  bloqueado recibe `401`, no `403`: su token deja de autenticarlo (RG-01).

Las rutas públicas no cambian: el login fallido sigue siendo el rechazo del dominio
(`403 UnauthorizedOperationException`), porque ocurre en una ruta abierta y lo decide el caso de
uso, no la cadena.

---

## 6.2 CORS: orígenes configurados, nunca comodín

Un cliente web servido desde otro dominio no puede llamar al API sin CORS. La configuración vive
en `SecurityConfig.corsConfigurationSource`, aplicada solo a `/api/**`:

| Ajuste | Valor |
|---|---|
| Orígenes | `nexusmarket.security.cors.allowed-origins` (por omisión `http://localhost:5173`) |
| Métodos | `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS` |
| Cabeceras | `Authorization`, `Content-Type` |

**Por qué nunca `*`:** este API se autentica con un *bearer token* que el cliente guarda y envía en
cada petición. Abrir el origen a cualquier dominio permitiría a una página arbitraria gastar la
sesión de un usuario autenticado. El comodín sería además incompatible con enviar credenciales. Los
orígenes se declaran por variable de entorno `CORS_ALLOWED_ORIGINS` en despliegue.

---

## 7. Orden de las operaciones en el registro

En `/auth/register`, `/admin/sellers` y `/admin/staff`, la credencial se guarda **después** de que el
dominio acepte al usuario, siempre con el mismo `CredentialService` (BCrypt):

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

`JwtAuthenticationFilterTest` cubre la decisión de §4, que es la que no se puede verificar leyendo
el proveedor:

| Caso | Resultado esperado |
|---|---|
| Token válido de usuario activo | Autentica, con el subtipo concreto (`Buyer`/`Seller`) en el principal |
| **Token válido de usuario bloqueado después de emitirlo** | **Rechazado**: el filtro relee el estado real |
| Token válido de un usuario que ya no existe | Rechazado |
| Cabecera ausente, vacía o sin el prefijo `Bearer ` | Pasa sin autenticar, para que la cadena devuelva 401/403 |

El segundo caso es el motivo de la decisión de §4 y la razón por la que el filtro no se conforma con
el *claim* del token.

`SecurityErrorResponsesTest` cubre §6.1 por HTTP, con la cadena real:

| Caso | Resultado esperado |
|---|---|
| Sin token | `401 AuthenticationRequired` con cuerpo JSON |
| Token mal formado | `401` |
| Token expirado | `401` |
| Token válido de un usuario bloqueado después de emitirlo | `401` |
| Comprador en una ruta de administrador / administrador en una ruta de comprador | `403 AccessDenied` |
| Catálogo y `/v3/api-docs` sin token | `200` |
| Login fallido | `403 UnauthorizedOperationException` (sin cambios) |

El ciclo completo por HTTP lo cubre `PurchaseFlowEndToEndTest`, que arranca el contexto real y
recorre plataforma vacía → pedido pagado, incluido el rechazo (`401`) de un token manipulado.
