package application.adapters.rest.dtos.requests;

import jakarta.validation.constraints.NotBlank;

/**
 * Inbound shape of a new stock record: one product in one warehouse (RD-INV-01). The identifier is
 * supplied by the caller, like every new entity (RD-POUT-04).
 */
public record OpenStockRecordRequest(@NotBlank String inventoryItemId,
                                     @NotBlank String productId,
                                     @NotBlank String warehouseId) {
}
