package application.adapters.persistence.jpa.repositories;

import application.adapters.persistence.jpa.entities.RefundJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataJpaRefundRepository extends JpaRepository<RefundJpaEntity, String> {

    Optional<RefundJpaEntity> findByProductReturnId(String productReturnId);
}
