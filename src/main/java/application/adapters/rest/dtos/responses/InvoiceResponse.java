package application.adapters.rest.dtos.responses;

import java.math.BigDecimal;

/**
 * Outbound shape of an invoice.
 */
public record InvoiceResponse(String id,
                              String orderId,
                              String buyerId,
                              BigDecimal totalAmount,
                              String currency) {
}
