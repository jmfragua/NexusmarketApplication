package application.adapters.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistence shape of an after sales return, always originated in a line of an existing order
 * (RD-POS-01).
 */
@Entity
@Table(name = "product_returns")
@Getter
@Setter
@NoArgsConstructor
public class ProductReturnJpaEntity {

    @Id
    @Column(name = "id", length = 60)
    private String id;

    @Column(name = "order_id", nullable = false, length = 60)
    private String orderId;

    @Column(name = "order_line_id", nullable = false, length = 60)
    private String orderLineId;

    @Column(name = "quantity", nullable = false)
    private int quantity;
}
