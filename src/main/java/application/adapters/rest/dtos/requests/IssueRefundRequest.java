package application.adapters.rest.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Inbound shape of a refund issuance. The amount is supplied because the specification defines no
 * formula for it, only the ceiling: it never exceeds the invoiced total (RD-POS-02).
 */
public record IssueRefundRequest(@NotBlank String refundId,
                                 @NotBlank String productReturnId,
                                 @NotBlank String invoiceId,
                                 @NotNull @PositiveOrZero BigDecimal amount,
                                 @NotBlank String currency) {
}
