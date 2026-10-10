package application.adapters.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistence shape of an invoice. The order is unique: an order is invoiced once (RD-FAC-01).
 */
@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
public class InvoiceJpaEntity {

    @Id
    @Column(name = "id", length = 60)
    private String id;

    @Column(name = "order_id", nullable = false, unique = true, length = 60)
    private String orderId;

    @Column(name = "buyer_id", nullable = false, length = 60)
    private String buyerId;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;
}
