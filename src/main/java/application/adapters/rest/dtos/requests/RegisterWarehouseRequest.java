package application.adapters.rest.dtos.requests;

import jakarta.validation.constraints.NotBlank;

/**
 * Inbound shape of a marketplace warehouse registration.
 */
public record RegisterWarehouseRequest(@NotBlank String warehouseId) {
}
