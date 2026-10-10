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
 * Persistence shape of a refund. The return is unique: the relation return → refund is 1 to 1
 * (RD-POS-02).
 */
@Entity
@Table(name = "refunds")
@Getter
@Setter
@NoArgsConstructor
public class RefundJpaEntity {

    @Id
    @Column(name = "id", length = 60)
    private String id;

    @Column(name = "product_return_id", nullable = false, unique = true, length = 60)
    private String productReturnId;

    @Column(name = "invoice_id", nullable = false, length = 60)
    private String invoiceId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;
}
