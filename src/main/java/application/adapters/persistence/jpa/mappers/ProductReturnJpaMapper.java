package application.adapters.persistence.jpa.mappers;

import application.adapters.persistence.jpa.entities.ProductReturnJpaEntity;
import application.domain.models.ProductReturn;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.OrderLineId;
import application.domain.valuesObjects.ProductReturnId;
import application.domain.valuesObjects.Quantity;

/**
 * Translates an after sales return between its domain shape and its persistence shape.
 *
 * <p>Restored through its public constructor rather than {@code registerReturn}: the latter demands
 * a delivered order and a matching line to validate a <em>new</em> return (RD-POS-01), conditions a
 * stored return already met when it was requested.</p>
 */
public final class ProductReturnJpaMapper {

    private ProductReturnJpaMapper() {
    }

    public static ProductReturnJpaEntity toEntity(ProductReturn productReturn) {
        ProductReturnJpaEntity entity = new ProductReturnJpaEntity();
        entity.setId(productReturn.getIdentifier().getValue());
        entity.setOrderId(productReturn.getOrderId().getValue());
        entity.setOrderLineId(productReturn.getOrderLineId().getValue());
        entity.setQuantity(productReturn.getQuantity().getValue());
        return entity;
    }

    public static ProductReturn toDomain(ProductReturnJpaEntity entity) {
        return new ProductReturn(
                ProductReturnId.of(entity.getId()),
                OrderId.of(entity.getOrderId()),
                OrderLineId.of(entity.getOrderLineId()),
                Quantity.of(entity.getQuantity()));
    }
}
