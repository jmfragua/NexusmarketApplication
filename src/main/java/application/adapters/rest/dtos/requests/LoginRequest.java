package application.adapters.rest.dtos.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Inbound shape of an authentication attempt.
 */
public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
}
