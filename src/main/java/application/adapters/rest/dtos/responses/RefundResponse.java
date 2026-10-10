package application.adapters.rest.dtos.responses;

import java.math.BigDecimal;

/**
 * Outbound shape of a refund.
 */
public record RefundResponse(String id,
                             String productReturnId,
                             String invoiceId,
                             BigDecimal amount,
                             String currency) {
}
