package application.adapters.persistence.jpa;

import application.adapters.persistence.jpa.mappers.InventoryItemJpaMapper;
import application.adapters.persistence.jpa.repositories.SpringDataJpaInventoryItemRepository;
import application.domain.models.InventoryItem;
import application.domain.ports.out.InventoryItemRepositoryPort;
import application.domain.valuesObjects.InventoryItemId;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.WarehouseId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Relational adapter of {@link InventoryItemRepositoryPort}.
 */
@Repository
public class InventoryItemJpaAdapter implements InventoryItemRepositoryPort {

    private final SpringDataJpaInventoryItemRepository inventoryItemRepository;

    public InventoryItemJpaAdapter(SpringDataJpaInventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = Objects.requireNonNull(inventoryItemRepository,
                "the inventory item repository is mandatory");
    }

    @Override
    public InventoryItem save(InventoryItem inventoryItem) {
        inventoryItemRepository.save(InventoryItemJpaMapper.toEntity(inventoryItem));
        return inventoryItem;
    }

    @Override
    public Optional<InventoryItem> findById(InventoryItemId inventoryItemId) {
        return inventoryItemRepository.findById(inventoryItemId.getValue())
                .map(InventoryItemJpaMapper::toDomain);
    }

    /**
     * Resolves the stock record by the unique pair (product, warehouse) of the distributed
     * inventory (RD-ID-05).
     */
    @Override
    public Optional<InventoryItem> findByProductAndWarehouse(ProductId productId, WarehouseId warehouseId) {
        return inventoryItemRepository
                .findByProductIdAndWarehouseId(productId.getValue(), warehouseId.getValue())
                .map(InventoryItemJpaMapper::toDomain);
    }

    @Override
    public List<InventoryItem> findByWarehouse(WarehouseId warehouseId) {
        return inventoryItemRepository.findByWarehouseId(warehouseId.getValue()).stream()
                .map(InventoryItemJpaMapper::toDomain)
                .toList();
    }

    @Override
    public List<InventoryItem> findByProduct(ProductId productId) {
        return inventoryItemRepository.findByProductId(productId.getValue()).stream()
                .map(InventoryItemJpaMapper::toDomain)
                .toList();
    }

    @Override
    public List<InventoryItem> findAll() {
        return inventoryItemRepository.findAll().stream()
                .map(InventoryItemJpaMapper::toDomain)
                .toList();
    }
}
