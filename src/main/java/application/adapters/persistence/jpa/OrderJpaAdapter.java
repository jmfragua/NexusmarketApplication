package application.adapters.persistence.jpa;

import application.adapters.persistence.jpa.mappers.OrderJpaMapper;
import application.adapters.persistence.jpa.repositories.SpringDataJpaOrderRepository;
import application.domain.models.Order;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.valuesObjects.BuyerId;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.OrderStatus;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Relational adapter of {@link OrderRepositoryPort}.
 */
@Repository
public class OrderJpaAdapter implements OrderRepositoryPort {

    private final SpringDataJpaOrderRepository orderRepository;

    public OrderJpaAdapter(SpringDataJpaOrderRepository orderRepository) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "the order repository is mandatory");
    }

    @Override
    public Order save(Order order) {
        orderRepository.save(OrderJpaMapper.toEntity(order));
        return order;
    }

    @Override
    public Optional<Order> findById(OrderId orderId) {
        return orderRepository.findById(orderId.getValue()).map(OrderJpaMapper::toDomain);
    }

    @Override
    public List<Order> findByBuyer(BuyerId buyerId) {
        return orderRepository.findByBuyerId(buyerId.getValue()).stream()
                .map(OrderJpaMapper::toDomain)
                .toList();
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status.code()).stream()
                .map(OrderJpaMapper::toDomain)
                .toList();
    }

    @Override
    public List<Order> findAll() {
        return orderRepository.findAll().stream()
                .map(OrderJpaMapper::toDomain)
                .toList();
    }
}
