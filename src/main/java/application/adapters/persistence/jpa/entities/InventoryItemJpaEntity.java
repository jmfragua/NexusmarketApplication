package application.adapters.persistence.jpa.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistence shape of a stock record.
 *
 * <p>The unique constraint over (product, warehouse) is the storage-level counterpart of RD-ID-05:
 * a product never holds two stock records in the same warehouse. Movements are part of this
 * aggregate and travel with it.</p>
 */
@Entity
@Table(name = "inventory_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_product_warehouse",
                columnNames = {"product_id", "warehouse_id"}))
@Getter
@Setter
@NoArgsConstructor
public class InventoryItemJpaEntity {

    @Id
    @Column(name = "id", length = 60)
    private String id;

    @Column(name = "product_id", nullable = false, length = 60)
    private String productId;

    @Column(name = "warehouse_id", nullable = false, length = 60)
    private String warehouseId;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Column(name = "stock_condition", nullable = false, length = 20)
    private String condition;

    @OneToMany(mappedBy = "inventoryItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<InventoryMovementJpaEntity> movements = new ArrayList<>();
}
