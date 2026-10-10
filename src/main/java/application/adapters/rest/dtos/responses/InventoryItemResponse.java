package application.adapters.rest.dtos.responses;

/**
 * Outbound shape of a stock record of the distributed inventory (RD-INV-01).
 */
public record InventoryItemResponse(String id,
                                    String productId,
                                    String warehouseId,
                                    int availableQuantity,
                                    int reservedQuantity,
                                    String condition) {
}
