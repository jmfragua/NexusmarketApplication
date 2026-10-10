package application.adapters.persistence.jpa.repositories;

import application.adapters.persistence.jpa.entities.InvoiceJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataJpaInvoiceRepository extends JpaRepository<InvoiceJpaEntity, String> {

    Optional<InvoiceJpaEntity> findByOrderId(String orderId);

    List<InvoiceJpaEntity> findByBuyerId(String buyerId);
}
