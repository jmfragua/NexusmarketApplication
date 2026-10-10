package application.adapters.persistence.jpa;

import application.adapters.persistence.jpa.mappers.InventoryItemJpaMapper;
import application.adapters.persistence.jpa.repositories.SpringDataJpaInventoryMovementRepository;
import application.domain.models.InventoryMovement;
import application.domain.ports.out.InventoryMovementRepositoryPort;
import application.domain.valuesObjects.InventoryItemId;
import application.domain.valuesObjects.InventoryMovementId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Relational adapter of {@link InventoryMovementRepositoryPort}.
 *
 * <p>Movements are written as part of their stock record, which cascades them. This adapter exists
 * for the queries the domain needs over the traceability itself (RD-INV-04), so {@code save} only
 * reports back what the cascade already persisted.</p>
 */
@Repository
public class InventoryMovementJpaAdapter implements InventoryMovementRepositoryPort {

    private final SpringDataJpaInventoryMovementRepository inventoryMovementRepository;

    public InventoryMovementJpaAdapter(SpringDataJpaInventoryMovementRepository inventoryMovementRepository) {
        this.inventoryMovementRepository = Objects.requireNonNull(inventoryMovementRepository,
                "the inventory movement repository is mandatory");
    }

    @Override
    public InventoryMovement save(InventoryMovement inventoryMovement) {
        return inventoryMovement;
    }

    @Override
    public Optional<InventoryMovement> findById(InventoryMovementId inventoryMovementId) {
        return inventoryMovementRepository.findById(inventoryMovementId.getValue())
                .map(InventoryItemJpaMapper::toMovementDomain);
    }

    @Override
    public List<InventoryMovement> findByInventoryItem(InventoryItemId inventoryItemId) {
        return inventoryMovementRepository.findByInventoryItemId(inventoryItemId.getValue()).stream()
                .map(InventoryItemJpaMapper::toMovementDomain)
                .toList();
    }

    @Override
    public List<InventoryMovement> findAll() {
        return inventoryMovementRepository.findAll().stream()
                .map(InventoryItemJpaMapper::toMovementDomain)
                .toList();
    }
}
