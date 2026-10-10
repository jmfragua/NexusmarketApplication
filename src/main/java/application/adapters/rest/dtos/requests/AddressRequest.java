package application.adapters.rest.dtos.requests;

import jakarta.validation.constraints.NotBlank;

/**
 * Inbound shape of a delivery address, used both to add an additional one and to promote one to
 * primary.
 */
public record AddressRequest(@NotBlank String address) {
}
