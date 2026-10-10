package application.adapters.rest.dtos.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Inbound shape of a seller incorporation.
 *
 * <p>The first warehouse is mandatory in the request because a seller is never created without one
 * (RD-ROL-05). The password is set here too: the administrator incorporates the seller, who could
 * not have registered themselves.</p>
 */
public record RegisterSellerRequest(@NotBlank String sellerId,
                                    @NotBlank String fullName,
                                    @NotBlank @Email String email,
                                    @NotBlank String identityDocument,
                                    @NotBlank String password,
                                    @NotBlank String firstWarehouseId) {
}
