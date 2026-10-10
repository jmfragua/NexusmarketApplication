package application.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import application.domain.models.Buyer;
import application.domain.models.User;
import application.domain.support.DomainFixtures;
import application.domain.support.InMemoryUserRepository;
import application.domain.valuesObjects.BuyerCommercialStatus;
import application.domain.valuesObjects.UserStatus;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Exercises the authentication filter, and above all the decision that shapes it: the user is read
 * from storage on every request instead of being trusted from the token claims.
 */
class JwtAuthenticationFilterTest {

    private static final String SECRET = "a-test-secret-long-enough-for-hs256-signing";

    private InMemoryUserRepository users;

    private JwtProvider jwtProvider;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        jwtProvider = new JwtProvider(new JwtProperties(SECRET, 60));
        filter = new JwtAuthenticationFilter(jwtProvider, users);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Authentication authenticateWith(String header) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (header != null) {
            request.addHeader("Authorization", header);
        }
        FilterChain chain = (req, res) -> { };
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    @DisplayName("a valid token authenticates the user carrying the real domain model")
    void validTokenAuthenticates() throws Exception {
        Buyer buyer = DomainFixtures.buyer();
        users.save(buyer);

        Authentication authentication = authenticateWith("Bearer " + jwtProvider.generateToken(buyer));

        assertNotNull(authentication);
        DomainUserPrincipal principal = (DomainUserPrincipal) authentication.getPrincipal();
        assertEquals(buyer.getIdentifier(), principal.user().getIdentifier());
        assertTrue(principal.user() instanceof Buyer,
                "re-reading the user restores the concrete subtype the entry ports demand");
    }

    /**
     * This is the whole point of re-reading the user. A JWT stays structurally valid until it
     * expires, so a user blocked <em>after</em> the token was issued would keep operating if the
     * filter trusted the claims — and the domain requires that only an active user can operate
     * (RG-01).
     */
    @Test
    @DisplayName("a structurally valid token of a user blocked afterwards authenticates nobody")
    void blockedUserIsRejectedDespiteValidToken() throws Exception {
        Buyer buyer = DomainFixtures.buyer();
        users.save(buyer);
        String token = jwtProvider.generateToken(buyer);

        buyer.block();
        users.save(buyer);

        assertNull(authenticateWith("Bearer " + token),
                "blocking must take effect immediately, not when the token expires");
    }

    @Test
    @DisplayName("reactivating the user restores access with the same token")
    void reactivatedUserIsAcceptedAgain() throws Exception {
        Buyer buyer = DomainFixtures.buyer();
        users.save(buyer);
        String token = jwtProvider.generateToken(buyer);

        buyer.block();
        users.save(buyer);
        assertNull(authenticateWith("Bearer " + token));

        buyer.activate();
        users.save(buyer);
        assertNotNull(authenticateWith("Bearer " + token));
    }

    @Test
    @DisplayName("a token of a user that no longer exists authenticates nobody")
    void unknownUserIsRejected() throws Exception {
        Buyer buyer = DomainFixtures.buyer();
        String token = jwtProvider.generateToken(buyer);

        assertNull(authenticateWith("Bearer " + token), "the user was never stored");
    }

    @Test
    @DisplayName("the authority carries the role the user has right now")
    void authorityReflectsCurrentRole() throws Exception {
        Buyer buyer = DomainFixtures.buyer("buyer-auth", UserStatus.ACTIVE, BuyerCommercialStatus.ENABLED);
        users.save(buyer);

        Authentication authentication = authenticateWith("Bearer " + jwtProvider.generateToken(buyer));

        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(granted -> granted.getAuthority().equals("ROLE_BUYER")));
    }

    @Test
    @DisplayName("requests without a usable bearer header stay anonymous instead of failing")
    void requestsWithoutUsableTokenStayAnonymous() throws Exception {
        User<?> buyer = DomainFixtures.buyer();
        users.save(buyer);

        assertNull(authenticateWith(null), "no header at all");
        assertNull(authenticateWith("Basic dXNlcjpwYXNz"), "a scheme this filter does not handle");
        assertNull(authenticateWith("Bearer not-a-real-token"), "garbage after the scheme");
        assertNull(authenticateWith("Bearer "), "an empty token");
    }

    @Test
    @DisplayName("a token signed with another secret authenticates nobody")
    void foreignSignatureIsRejected() throws Exception {
        Buyer buyer = DomainFixtures.buyer();
        users.save(buyer);
        JwtProvider other = new JwtProvider(new JwtProperties("a-different-secret-also-long-enough-ok", 60));

        assertNull(authenticateWith("Bearer " + other.generateToken(buyer)));
    }
}
