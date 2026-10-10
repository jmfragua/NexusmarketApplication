package application.adapters.rest.dtos.responses;

/**
 * Outbound shape of a warehouse. {@code sellerId} is null for marketplace warehouses, which have no
 * owner.
 */
public record WarehouseResponse(String id, String warehouseType, String sellerId) {
}
