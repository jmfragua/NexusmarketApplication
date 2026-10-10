package application.adapters.rest.dtos.requests;

import jakarta.validation.constraints.NotBlank;

/**
 * Inbound shape of an invoice issuance over a paid order (RD-FAC-01).
 */
public record IssueInvoiceRequest(@NotBlank String invoiceId, @NotBlank String orderId) {
}
