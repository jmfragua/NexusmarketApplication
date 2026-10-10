package application.adapters.rest.dtos.responses;

/**
 * Outbound shape of a successful authentication: the bearer token plus the identity it carries.
 */
public record AuthResponse(String token, String userId, String email, String role) {
}
