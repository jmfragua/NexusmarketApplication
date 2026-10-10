package application.adapters.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistence shape of a stock movement: the traceability of the inventory (RD-INV-04).
 */
@Entity
@Table(name = "inventory_movements")
@Getter
@Setter
@NoArgsConstructor
public class InventoryMovementJpaEntity {

    @Id
    @Column(name = "id", length = 60)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItemJpaEntity inventoryItem;

    @Column(name = "movement_type", nullable = false, length = 30)
    private String movementType;

    @Column(name = "quantity", nullable = false)
    private int quantity;
}
