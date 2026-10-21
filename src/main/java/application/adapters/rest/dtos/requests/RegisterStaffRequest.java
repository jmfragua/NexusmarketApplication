package application.adapters.rest.dtos.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Inbound shape of a staff incorporation. Only logistics operators and supervisors are incorporated
 * this way (RD-ROL-09); the domain rejects any other role again, so the pattern is a first filter,
 * not the rule.
 */
public record RegisterStaffRequest(@NotBlank String userId,
                                   @NotBlank String fullName,
                                   @NotBlank @Email String email,
                                   @NotBlank String identityDocument,
                                   @NotBlank String password,
                                   @NotBlank @Pattern(regexp = "LOGISTICS_OPERATOR|SUPERVISOR",
                                           message = "must be LOGISTICS_OPERATOR or SUPERVISOR") String role) {
}
