package application.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import application.domain.models.Buyer;
import application.domain.support.DomainFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Exercises the token provider: what it issues must be readable back, and what it did not issue
 * must be rejected.
 */
class JwtProviderTest {

    private static final String SECRET = "a-test-secret-long-enough-for-hs256-signing";

    private final JwtProvider provider = new JwtProvider(new JwtProperties(SECRET, 60));

    @Test
    @DisplayName("a token it issued carries back the email of the user")
    void roundTripsTheEmail() {
        Buyer buyer = DomainFixtures.buyer();

        String token = provider.generateToken(buyer);

        assertEquals(buyer.getEmail().getValue(), provider.readEmail(token).orElseThrow());
    }

    @Test
    @DisplayName("a token signed with another secret is rejected")
    void rejectsForeignSignature() {
        JwtProvider other = new JwtProvider(new JwtProperties("a-different-secret-also-long-enough-ok", 60));
        String foreignToken = other.generateToken(DomainFixtures.buyer());

        assertTrue(provider.readEmail(foreignToken).isEmpty(),
                "a token this provider did not sign must never be trusted");
    }

    @Test
    @DisplayName("an expired token is rejected")
    void rejectsExpiredToken() {
        JwtProvider expiring = new JwtProvider(new JwtProperties(SECRET, -1));
        String expired = expiring.generateToken(DomainFixtures.buyer());

        assertTrue(provider.readEmail(expired).isEmpty());
    }

    @Test
    @DisplayName("garbage, blank and null inputs are rejected without throwing")
    void rejectsMalformedInput() {
        assertTrue(provider.readEmail("not-a-token").isEmpty());
        assertTrue(provider.readEmail("").isEmpty());
        assertTrue(provider.readEmail(null).isEmpty());
    }

    @Test
    @DisplayName("a tampered payload invalidates the signature")
    void rejectsTamperedToken() {
        String token = provider.generateToken(DomainFixtures.buyer());
        String tampered = token.substring(0, token.lastIndexOf('.')) + ".forged-signature";

        assertFalse(provider.readEmail(tampered).isPresent());
    }
}
