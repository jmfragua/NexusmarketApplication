package application.adapters.rest.mappers;

import application.adapters.rest.dtos.requests.OrderLineRequest;
import application.adapters.rest.dtos.responses.OrderLineResponse;
import application.adapters.rest.dtos.responses.OrderResponse;
import application.domain.models.Order;
import application.domain.models.OrderLine;
import application.domain.valuesObjects.Money;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.OrderLineId;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.ProductVariantId;
import application.domain.valuesObjects.Quantity;

/**
 * Translates orders between their domain shape and the REST shapes.
 */
public final class OrderRestMapper {

    private OrderRestMapper() {
    }

    public static OrderLine toLineDomain(OrderLineRequest request, OrderId orderId) {
        return new OrderLine(
                OrderLineId.of(request.orderLineId()),
                orderId,
                ProductId.of(request.productId()),
                ProductVariantId.of(request.productVariantId()),
                Quantity.of(request.quantity()),
                Money.of(request.lineAmount(), request.currency()));
    }

    public static OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getIdentifier().getValue(),
                order.getBuyerId().getValue(),
                order.getStatus().code(),
                order.isModifiable(),
                order.getDeliveryAddress().getValue(),
                order.getTotalAmount().getAmount(),
                order.getTotalAmount().getCurrency(),
                order.getLines().stream().map(OrderRestMapper::toResponse).toList());
    }

    public static OrderLineResponse toResponse(OrderLine line) {
        return new OrderLineResponse(
                line.getIdentifier().getValue(),
                line.getProductId().getValue(),
                line.getProductVariantId().getValue(),
                line.getQuantity().getValue(),
                line.getLineAmount().getAmount(),
                line.getLineAmount().getCurrency());
    }
}
