package application.domain.ports.out;

import application.domain.models.Order;
import application.domain.valuesObjects.BuyerId;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.OrderStatus;
import java.util.List;
import java.util.Optional;

/**
 * Output port of the orders. An order belongs to a single buyer and only that buyer acts upon it
 * (RD-PED-05).
 */
public interface OrderRepositoryPort {

    Order save(Order order);

    Optional<Order> findById(OrderId orderId);

    List<Order> findByBuyer(BuyerId buyerId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findAll();
}
