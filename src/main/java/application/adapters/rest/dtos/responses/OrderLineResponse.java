package application.adapters.rest.dtos.responses;

import java.math.BigDecimal;

/**
 * Outbound shape of a line of an order.
 */
public record OrderLineResponse(String id,
                                String productId,
                                String productVariantId,
                                int quantity,
                                BigDecimal lineAmount,
                                String currency) {
}
