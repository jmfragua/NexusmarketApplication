package application.adapters.persistence.jpa.repositories;

import application.adapters.persistence.jpa.entities.ShipmentJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataJpaShipmentRepository extends JpaRepository<ShipmentJpaEntity, String> {

    List<ShipmentJpaEntity> findByOrderId(String orderId);

    List<ShipmentJpaEntity> findByWarehouseId(String warehouseId);
}
