package application.adapters.rest.dtos.responses;

/**
 * Outbound shape of any user, used where the specialization does not matter.
 */
public record UserResponse(String id,
                           String fullName,
                           String email,
                           String identityDocument,
                           String role,
                           String status) {
}
