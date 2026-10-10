package application.infrastructure.security;

import application.domain.models.User;
import java.util.Objects;

/**
 * Carries the authenticated domain user through the Spring Security context, so REST controllers
 * receive a real {@code User} instead of a framework-specific principal.
 *
 * @param user the authenticated user, freshly resolved from storage on this request.
 */
public record DomainUserPrincipal(User<?> user) {

    public DomainUserPrincipal {
        Objects.requireNonNull(user, "the authenticated user is mandatory");
    }

    /**
     * @return the Spring Security authority derived from the domain role, prefixed as Spring
     *         expects for {@code hasRole} checks.
     */
    public String authority() {
        return "ROLE_" + user.getRole().code();
    }
}
