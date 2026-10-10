package application.adapters.rest.dtos.responses;

import java.util.List;

/**
 * Outbound shape of a catalogue product.
 *
 * <p>{@code visibleInCatalog} is exposed as a computed field because visibility is not the status
 * alone: it also demands at least one variant (RD-CAT-03, RD-CAT-05), and a client should not have
 * to re-derive that rule.</p>
 */
public record ProductResponse(String id,
                              String sellerId,
                              String productType,
                              String status,
                              boolean visibleInCatalog,
                              List<ProductVariantResponse> variants) {
}
