package application.adapters.rest.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Inbound shape of a priced order line.
 *
 * <p>The amount travels in the request because the specification models no price on the product:
 * only the line amount and the order total exist, so the domain never invents one (RD-ALC-03).</p>
 */
public record OrderLineRequest(@NotBlank String orderLineId,
                               @NotBlank String productId,
                               @NotBlank String productVariantId,
                               @Positive int quantity,
                               @NotNull @PositiveOrZero BigDecimal lineAmount,
                               @NotBlank String currency) {
}
