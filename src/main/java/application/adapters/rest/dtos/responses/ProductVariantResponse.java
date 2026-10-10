package application.adapters.rest.dtos.responses;

import java.util.Map;

/**
 * Outbound shape of a product variant.
 */
public record ProductVariantResponse(String id, Map<String, String> attributes) {
}
