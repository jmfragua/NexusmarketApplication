package application.adapters.persistence.jpa;

import application.adapters.persistence.jpa.mappers.ShipmentJpaMapper;
import application.adapters.persistence.jpa.repositories.SpringDataJpaShipmentRepository;
import application.domain.models.Shipment;
import application.domain.ports.out.ShipmentRepositoryPort;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.ShipmentId;
import application.domain.valuesObjects.WarehouseId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Relational adapter of {@link ShipmentRepositoryPort}.
 */
@Repository
public class ShipmentJpaAdapter implements ShipmentRepositoryPort {

    private final SpringDataJpaShipmentRepository shipmentRepository;

    private final JpaInserter jpaInserter;

    public ShipmentJpaAdapter(SpringDataJpaShipmentRepository shipmentRepository,
                              JpaInserter jpaInserter) {
        this.shipmentRepository = Objects.requireNonNull(shipmentRepository, "the shipment repository is mandatory");
        this.jpaInserter = Objects.requireNonNull(jpaInserter, "the inserter is mandatory");
    }

    @Override
    public Shipment save(Shipment shipment) {
        shipmentRepository.save(ShipmentJpaMapper.toEntity(shipment));
        return shipment;
    }

    @Override
    public Shipment create(Shipment shipment) {
        jpaInserter.insert(ShipmentJpaMapper.toEntity(shipment));
        return shipment;
    }

    @Override
    public Optional<Shipment> findById(ShipmentId shipmentId) {
        return shipmentRepository.findById(shipmentId.getValue()).map(ShipmentJpaMapper::toDomain);
    }

    @Override
    public List<Shipment> findByOrder(OrderId orderId) {
        return shipmentRepository.findByOrderId(orderId.getValue()).stream()
                .map(ShipmentJpaMapper::toDomain)
                .toList();
    }

    @Override
    public List<Shipment> findByWarehouse(WarehouseId warehouseId) {
        return shipmentRepository.findByWarehouseId(warehouseId.getValue()).stream()
                .map(ShipmentJpaMapper::toDomain)
                .toList();
    }

    @Override
    public List<Shipment> findAll() {
        return shipmentRepository.findAll().stream()
                .map(ShipmentJpaMapper::toDomain)
                .toList();
    }
}
