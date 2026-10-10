package application.adapters.persistence.jpa.repositories;

import application.adapters.persistence.jpa.entities.OrderJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataJpaOrderRepository extends JpaRepository<OrderJpaEntity, String> {

    List<OrderJpaEntity> findByBuyerId(String buyerId);

    List<OrderJpaEntity> findByStatus(String status);
}
