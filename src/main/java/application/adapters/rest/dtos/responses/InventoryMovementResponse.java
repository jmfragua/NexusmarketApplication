package application.adapters.rest.dtos.responses;

/**
 * Outbound shape of a stock movement, the traceability of the inventory (RD-INV-04).
 */
public record InventoryMovementResponse(String id, String inventoryItemId, String movementType, int quantity) {
}
