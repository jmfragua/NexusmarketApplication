package application.adapters.rest.mappers;

import application.adapters.rest.dtos.responses.CartItemResponse;
import application.adapters.rest.dtos.responses.CartResponse;
import application.domain.models.Cart;
import application.domain.models.CartItem;

/**
 * Translates carts between their domain shape and the REST shapes.
 */
public final class CartRestMapper {

    private CartRestMapper() {
    }

    public static CartResponse toResponse(Cart cart) {
        return new CartResponse(
                cart.getIdentifier().getValue(),
                cart.getBuyerId().getValue(),
                cart.getItems().stream().map(CartRestMapper::toResponse).toList());
    }

    public static CartItemResponse toResponse(CartItem item) {
        return new CartItemResponse(
                item.getIdentifier().getValue(),
                item.getProductId().getValue(),
                item.getProductVariantId().getValue(),
                item.getQuantity().getValue());
    }
}
