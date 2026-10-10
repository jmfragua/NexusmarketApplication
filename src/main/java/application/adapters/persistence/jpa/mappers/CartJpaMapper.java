package application.adapters.persistence.jpa.mappers;

import application.adapters.persistence.jpa.entities.CartItemJpaEntity;
import application.adapters.persistence.jpa.entities.CartJpaEntity;
import application.domain.models.Cart;
import application.domain.models.CartItem;
import application.domain.valuesObjects.BuyerId;
import application.domain.valuesObjects.CartId;
import application.domain.valuesObjects.CartItemId;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.ProductVariantId;
import application.domain.valuesObjects.Quantity;

/**
 * Translates a cart between its domain shape and its persistence shape.
 *
 * <p>The cart needs no reconstruction factory: replaying {@code addItem} once per stored row is
 * faithful, because each row already represents a distinct product variant — the accumulation rule
 * of {@code addItem} merged them at write time, not at read time.</p>
 */
public final class CartJpaMapper {

    private CartJpaMapper() {
    }

    public static CartJpaEntity toEntity(Cart cart) {
        CartJpaEntity entity = new CartJpaEntity();
        entity.setId(cart.getIdentifier().getValue());
        entity.setBuyerId(cart.getBuyerId().getValue());
        cart.getItems().forEach(item -> entity.getItems().add(toItemEntity(item, entity)));
        return entity;
    }

    public static Cart toDomain(CartJpaEntity entity) {
        Cart cart = new Cart(CartId.of(entity.getId()), BuyerId.of(entity.getBuyerId()));
        entity.getItems().forEach(item -> cart.addItem(
                CartItemId.of(item.getId()),
                ProductId.of(item.getProductId()),
                ProductVariantId.of(item.getProductVariantId()),
                Quantity.of(item.getQuantity())));
        return cart;
    }

    private static CartItemJpaEntity toItemEntity(CartItem item, CartJpaEntity cart) {
        CartItemJpaEntity entity = new CartItemJpaEntity();
        entity.setId(item.getIdentifier().getValue());
        entity.setCart(cart);
        entity.setProductId(item.getProductId().getValue());
        entity.setProductVariantId(item.getProductVariantId().getValue());
        entity.setQuantity(item.getQuantity().getValue());
        return entity;
    }
}
