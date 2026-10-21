package application.domain.ports.out;

import application.domain.models.Order;
import application.domain.valuesObjects.BuyerId;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.OrderLineId;
import application.domain.valuesObjects.OrderStatus;
import java.util.List;
import java.util.Optional;

/**
 * Output port of the orders. An order belongs to a single buyer and only that buyer acts upon it
 * (RD-PED-05).
 */
public interface OrderRepositoryPort {

    Order save(Order order);

    /**
     * Stores a new order together with its lines. Unlike {@link #save}, it never replaces an existing
     * one: when the identifier of the order or of a line is already taken the storage refuses it, so
     * a creation can never act as an update (RD-ID-06).
     */
    Order create(Order order);

    Optional<Order> findById(OrderId orderId);

    List<Order> findByBuyer(BuyerId buyerId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findAll();

    /**
     * @return whether any order already holds a line with this identifier.
     */
    boolean existsOrderLine(OrderLineId orderLineId);
}
