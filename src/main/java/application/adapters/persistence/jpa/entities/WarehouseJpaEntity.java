package application.adapters.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistence shape of a warehouse. {@code warehouseType} tells a marketplace warehouse from a
 * seller one; {@code sellerId} is null for the former, which has no owner.
 */
@Entity
@Table(name = "warehouses")
@Getter
@Setter
@NoArgsConstructor
public class WarehouseJpaEntity {

    @Id
    @Column(name = "id", length = 60)
    private String id;

    @Column(name = "warehouse_type", nullable = false, length = 20)
    private String warehouseType;

    /** Seller warehouses only; null for marketplace warehouses. */
    @Column(name = "seller_id", length = 60)
    private String sellerId;
}
