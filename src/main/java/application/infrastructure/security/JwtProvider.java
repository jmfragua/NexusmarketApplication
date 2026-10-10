package application.infrastructure.security;

import application.domain.models.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * Issues and reads the tokens that carry the identity of the authenticated user.
 *
 * <p>Lives outside the domain: the business specification leaves technical authentication
 * mechanisms explicitly out of scope (RD-ALC-01).</p>
 */
@Component
public class JwtProvider {

    private static final String EMAIL_CLAIM = "email";

    private static final String ROLE_CLAIM = "role";

    private final SecretKey signingKey;

    private final Duration expiration;

    public JwtProvider(JwtProperties properties) {
        Objects.requireNonNull(properties, "the jwt properties are mandatory");
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofMinutes(properties.expirationMinutes());
    }

    /**
     * @return a signed token carrying the identifier, email and role of the user.
     */
    public String generateToken(User<?> user) {
        Objects.requireNonNull(user, "the user is mandatory");
        Instant issuedAt = Instant.now();
        return Jwts.builder()
                .subject(user.getIdentifier().getValue())
                .claim(EMAIL_CLAIM, user.getEmail().getValue())
                .claim(ROLE_CLAIM, user.getRole().code())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plus(expiration)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Reads the email out of a token, verifying its signature and expiry.
     *
     * @return the email claim, empty when the token is absent, malformed, tampered with or expired.
     */
    public Optional<String> readEmail(String token) {
        return readClaims(token).map(claims -> claims.get(EMAIL_CLAIM, String.class));
    }

    private Optional<Claims> readClaims(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException invalidToken) {
            return Optional.empty();
        }
    }
}
