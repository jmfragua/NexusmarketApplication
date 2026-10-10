package application.infrastructure.security;

import application.domain.models.User;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.valuesObjects.EmailAddress;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates each request from its bearer token.
 *
 * <p>The user is re-read from storage on every request instead of being trusted from the token
 * claims. A JWT stays structurally valid until it expires, so a user blocked after the token was
 * issued would keep operating — and the domain requires that only an active user can operate
 * (RG-01). The re-read also returns the concrete subtype ({@code Buyer}, {@code Seller}) the entry
 * ports expect, at the cost of one query per authenticated request.</p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    private final UserRepositoryPort userRepositoryPort;

    public JwtAuthenticationFilter(JwtProvider jwtProvider, UserRepositoryPort userRepositoryPort) {
        this.jwtProvider = Objects.requireNonNull(jwtProvider, "the jwt provider is mandatory");
        this.userRepositoryPort = Objects.requireNonNull(userRepositoryPort, "the user repository is mandatory");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        bearerTokenOf(request)
                .flatMap(jwtProvider::readEmail)
                .flatMap(this::findActiveUser)
                .ifPresent(this::authenticate);
        filterChain.doFilter(request, response);
    }

    private Optional<String> bearerTokenOf(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        return Optional.of(header.substring(BEARER_PREFIX.length()).trim());
    }

    /**
     * Rejects a structurally valid token whose user no longer exists or is no longer active: the
     * authentication is never established, so the request continues as anonymous and the
     * authorization rules deny it (RG-01).
     */
    private Optional<User<?>> findActiveUser(String email) {
        return userRepositoryPort.findByEmail(EmailAddress.of(email))
                .filter(User::isActive);
    }

    private void authenticate(User<?> user) {
        DomainUserPrincipal principal = new DomainUserPrincipal(user);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority(principal.authority())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
