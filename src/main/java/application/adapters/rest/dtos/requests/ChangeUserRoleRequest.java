package application.adapters.rest.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Inbound shape of a role reassignment. The role stays unique per user (RG-02): this replaces the
 * previous one, it never accumulates.
 */
public record ChangeUserRoleRequest(
        @NotBlank @Pattern(regexp = "BUYER|SELLER|LOGISTICS_OPERATOR|ADMINISTRATOR|SUPERVISOR") String role) {
}
