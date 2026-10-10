package application.adapters.rest.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Inbound shape of a product registration. The type is fixed at this moment and never changes
 * during the life of the product (RD-CAT-02).
 */
public record RegisterProductRequest(@NotBlank String productId,
                                     @NotBlank @Pattern(regexp = "PHYSICAL|DIGITAL") String productType) {
}
