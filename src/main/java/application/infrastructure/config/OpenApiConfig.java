package application.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Publishes the API contract as navigable OpenAPI documentation.
 *
 * <p>The written contract lives in {@code SDD/Adapters/Api-rest-endpoints.md}; this makes the same
 * contract explorable and callable from a browser at {@code /swagger-ui.html}, which is what lets
 * someone try the API without writing a single {@code curl}.</p>
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI nexusmarketOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("NexusMarket API")
                        .version("v1")
                        .description("""
                                Marketplace con arquitectura hexagonal. Los puertos de entrada están \
                                organizados por rol, de modo que cada grupo de endpoints corresponde a \
                                un participante del negocio: comprador, vendedor, operador logístico, \
                                administrador y supervisor.

                                Para operar: registra un comprador en /api/v1/auth/register o \
                                autentícate en /api/v1/auth/login, y usa el token devuelto con el \
                                botón Authorize. El catálogo público no requiere token."""))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token devuelto por /api/v1/auth/login o /api/v1/auth/register")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
