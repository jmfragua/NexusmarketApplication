package application.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the JWT settings declared under {@code nexusmarket.security.jwt} in the configuration.
 *
 * @param secret            signing key. Must be at least 32 characters for HS256.
 * @param expirationMinutes lifetime of an issued token.
 */
@ConfigurationProperties(prefix = "nexusmarket.security.jwt")
public record JwtProperties(String secret, long expirationMinutes) {
}
