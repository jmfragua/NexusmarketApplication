package application.adapters.persistence.jpa.mappers;

import application.adapters.persistence.jpa.entities.OrderJpaEntity;
import application.adapters.persistence.jpa.entities.OrderLineJpaEntity;
import application.domain.models.Order;
import application.domain.models.OrderLine;
import application.domain.valuesObjects.Address;
import application.domain.valuesObjects.BuyerId;
import application.domain.valuesObjects.Money;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.OrderLineId;
import application.domain.valuesObjects.OrderStatus;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.ProductVariantId;
import application.domain.valuesObjects.Quantity;
import java.util.List;

/**
 * Translates an order between its domain shape and its persistence shape.
 */
public final class OrderJpaMapper {

    private OrderJpaMapper() {
    }

    public static OrderJpaEntity toEntity(Order order) {
        OrderJpaEntity entity = new OrderJpaEntity();
        entity.setId(order.getIdentifier().getValue());
        entity.setBuyerId(order.getBuyerId().getValue());
        entity.setStatus(order.getStatus().code());
        entity.setDeliveryAddress(order.getDeliveryAddress().getValue());
        entity.setTotalAmount(order.getTotalAmount().getAmount());
        entity.setCurrency(order.getTotalAmount().getCurrency());
        order.getLines().forEach(line -> entity.getLines().add(toLineEntity(line, entity)));
        return entity;
    }

    /**
     * Restores the order through {@link Order#reconstruct}, which keeps the persisted status instead
     * of forcing the one a brand new order is born with (RD-ALC-04).
     */
    public static Order toDomain(OrderJpaEntity entity) {
        List<OrderLine> lines = entity.getLines().stream()
                .map(OrderJpaMapper::toLineDomain)
                .toList();
        return Order.reconstruct(
                OrderId.of(entity.getId()),
                BuyerId.of(entity.getBuyerId()),
                Address.of(entity.getDeliveryAddress()),
                lines,
                OrderStatus.valueOf(entity.getStatus()));
    }

    private static OrderLineJpaEntity toLineEntity(OrderLine line, OrderJpaEntity order) {
        OrderLineJpaEntity entity = new OrderLineJpaEntity();
        entity.setId(line.getIdentifier().getValue());
        entity.setOrder(order);
        entity.setProductId(line.getProductId().getValue());
        entity.setProductVariantId(line.getProductVariantId().getValue());
        entity.setQuantity(line.getQuantity().getValue());
        entity.setLineAmount(line.getLineAmount().getAmount());
        entity.setCurrency(line.getLineAmount().getCurrency());
        return entity;
    }

    private static OrderLine toLineDomain(OrderLineJpaEntity entity) {
        return new OrderLine(
                OrderLineId.of(entity.getId()),
                OrderId.of(entity.getOrder().getId()),
                ProductId.of(entity.getProductId()),
                ProductVariantId.of(entity.getProductVariantId()),
                Quantity.of(entity.getQuantity()),
                Money.of(entity.getLineAmount(), entity.getCurrency()));
    }
}
