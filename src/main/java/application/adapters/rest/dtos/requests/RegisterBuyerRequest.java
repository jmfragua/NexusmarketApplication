package application.adapters.rest.dtos.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Inbound shape of a buyer self registration.
 *
 * <p>Bean validation only guards the shape of the request. The real rules — a well formed email, a
 * non blank name, a unique document — belong to the value objects and the domain service, and are
 * enforced again there (RD-VO-07, RD-ID-03).</p>
 */
public record RegisterBuyerRequest(@NotBlank String buyerId,
                                   @NotBlank String fullName,
                                   @NotBlank @Email String email,
                                   @NotBlank String identityDocument,
                                   @NotBlank String password,
                                   @NotBlank String primaryAddress) {
}
