package application.domain.support;

import application.domain.models.SellerWarehouse;
import application.domain.models.Warehouse;
import application.domain.ports.out.WarehouseRepositoryPort;
import application.domain.valuesObjects.SellerId;
import application.domain.valuesObjects.WarehouseId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory double of {@link WarehouseRepositoryPort}.
 */
public class InMemoryWarehouseRepository implements WarehouseRepositoryPort {

    private final Map<String, Warehouse> warehouses = new LinkedHashMap<>();

    @Override
    public Warehouse save(Warehouse warehouse) {
        warehouses.put(warehouse.getIdentifier().getValue(), warehouse);
        return warehouse;
    }

    @Override
    public Warehouse create(Warehouse warehouse) {
        if (warehouses.containsKey(warehouse.getIdentifier().getValue())) {
            throw new IllegalStateException("identifier already taken: " + warehouse.getIdentifier());
        }
        return save(warehouse);
    }

    @Override
    public Optional<Warehouse> findById(WarehouseId warehouseId) {
        return Optional.ofNullable(warehouses.get(warehouseId.getValue()));
    }

    @Override
    public List<SellerWarehouse> findBySeller(SellerId sellerId) {
        return warehouses.values().stream()
                .filter(SellerWarehouse.class::isInstance)
                .map(SellerWarehouse.class::cast)
                .filter(warehouse -> warehouse.belongsTo(sellerId))
                .toList();
    }

    @Override
    public List<Warehouse> findAll() {
        return new ArrayList<>(warehouses.values());
    }
}
