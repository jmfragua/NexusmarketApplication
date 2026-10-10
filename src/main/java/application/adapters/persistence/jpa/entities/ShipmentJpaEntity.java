package application.adapters.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistence shape of a shipment.
 *
 * <p>It carries no status column on purpose: the specification declares no state catalogue for the
 * shipment, and the logistic progress lives on the state of the order (RD-ALC-03).</p>
 */
@Entity
@Table(name = "shipments")
@Getter
@Setter
@NoArgsConstructor
public class ShipmentJpaEntity {

    @Id
    @Column(name = "id", length = 60)
    private String id;

    @Column(name = "order_id", nullable = false, length = 60)
    private String orderId;

    @Column(name = "warehouse_id", nullable = false, length = 60)
    private String warehouseId;

    @Column(name = "delivery_address", nullable = false, length = 400)
    private String deliveryAddress;
}
