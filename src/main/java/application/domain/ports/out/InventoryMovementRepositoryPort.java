package application.domain.ports.out;

import application.domain.models.InventoryMovement;
import application.domain.valuesObjects.InventoryItemId;
import application.domain.valuesObjects.InventoryMovementId;
import java.util.List;
import java.util.Optional;

/**
 * Output port of the inventory traceability: no stock change exists without an associated movement
 * (RD-INV-04).
 */
public interface InventoryMovementRepositoryPort {

    InventoryMovement save(InventoryMovement inventoryMovement);

    Optional<InventoryMovement> findById(InventoryMovementId inventoryMovementId);

    List<InventoryMovement> findByInventoryItem(InventoryItemId inventoryItemId);

    List<InventoryMovement> findAll();
}
