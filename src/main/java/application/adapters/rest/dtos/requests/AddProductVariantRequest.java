package application.adapters.rest.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

/**
 * Inbound shape of a new product variant: a set of characteristics such as colour or size
 * (RD-CAT-05).
 */
public record AddProductVariantRequest(@NotBlank String variantId,
                                       @NotEmpty Map<String, String> attributes) {
}
