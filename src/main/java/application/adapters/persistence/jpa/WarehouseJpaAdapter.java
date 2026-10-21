package application.adapters.persistence.jpa;

import application.adapters.persistence.jpa.mappers.WarehouseJpaMapper;
import application.adapters.persistence.jpa.repositories.SpringDataJpaWarehouseRepository;
import application.domain.models.SellerWarehouse;
import application.domain.models.Warehouse;
import application.domain.ports.out.WarehouseRepositoryPort;
import application.domain.valuesObjects.SellerId;
import application.domain.valuesObjects.WarehouseId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Relational adapter of {@link WarehouseRepositoryPort}.
 */
@Repository
public class WarehouseJpaAdapter implements WarehouseRepositoryPort {

    private final SpringDataJpaWarehouseRepository warehouseRepository;

    private final JpaInserter jpaInserter;

    public WarehouseJpaAdapter(SpringDataJpaWarehouseRepository warehouseRepository,
                               JpaInserter jpaInserter) {
        this.warehouseRepository = Objects.requireNonNull(warehouseRepository,
                "the warehouse repository is mandatory");
        this.jpaInserter = Objects.requireNonNull(jpaInserter, "the inserter is mandatory");
    }

    @Override
    public Warehouse save(Warehouse warehouse) {
        warehouseRepository.save(WarehouseJpaMapper.toEntity(warehouse));
        return warehouse;
    }

    @Override
    public Warehouse create(Warehouse warehouse) {
        jpaInserter.insert(WarehouseJpaMapper.toEntity(warehouse));
        return warehouse;
    }

    @Override
    public Optional<Warehouse> findById(WarehouseId warehouseId) {
        return warehouseRepository.findById(warehouseId.getValue()).map(WarehouseJpaMapper::toDomain);
    }

    @Override
    public List<SellerWarehouse> findBySeller(SellerId sellerId) {
        return warehouseRepository.findBySellerId(sellerId.getValue()).stream()
                .map(WarehouseJpaMapper::toSellerWarehouseDomain)
                .toList();
    }

    @Override
    public List<Warehouse> findAll() {
        return warehouseRepository.findAll().stream()
                .map(WarehouseJpaMapper::toDomain)
                .toList();
    }
}
