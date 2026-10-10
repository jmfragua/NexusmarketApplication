package application.adapters.persistence.jpa.repositories;

import application.adapters.persistence.jpa.entities.ProductReturnJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataJpaProductReturnRepository extends JpaRepository<ProductReturnJpaEntity, String> {

    List<ProductReturnJpaEntity> findByOrderId(String orderId);
}
