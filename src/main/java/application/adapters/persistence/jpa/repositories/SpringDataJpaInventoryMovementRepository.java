package application.adapters.persistence.jpa.repositories;

import application.adapters.persistence.jpa.entities.InventoryMovementJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataJpaInventoryMovementRepository extends JpaRepository<InventoryMovementJpaEntity, String> {

    List<InventoryMovementJpaEntity> findByInventoryItemId(String inventoryItemId);
}
