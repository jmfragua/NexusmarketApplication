package application.domain.ports.out;

import application.domain.models.SellerWarehouse;
import application.domain.models.Warehouse;
import application.domain.valuesObjects.SellerId;
import application.domain.valuesObjects.WarehouseId;
import java.util.List;
import java.util.Optional;

/**
 * Output port of the warehouses where the distributed inventory is held (RD-INV-01).
 */
public interface WarehouseRepositoryPort {

    Warehouse save(Warehouse warehouse);

    /**
     * Stores a new warehouse. Unlike {@link #save}, it never replaces an existing one: when the
     * identifier is already taken the storage refuses it, so a creation can never act as an update
     * (RD-ID-06).
     */
    Warehouse create(Warehouse warehouse);

    Optional<Warehouse> findById(WarehouseId warehouseId);

    List<SellerWarehouse> findBySeller(SellerId sellerId);

    List<Warehouse> findAll();
}
