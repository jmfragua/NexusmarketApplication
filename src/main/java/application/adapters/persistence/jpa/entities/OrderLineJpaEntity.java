package application.adapters.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

/**
 * Persistence shape of a line of an order, with the commercial value committed at confirmation time.
 */
// Immutable: an order line never changes once the order is placed (RD-PED-02).
// A merge that reaches an existing row, for instance through an identifier reused by another
// request, can therefore never rewrite it or move it to another parent.
@Immutable
@Entity
@Table(name = "order_lines")
@Getter
@Setter
@NoArgsConstructor
public class OrderLineJpaEntity {

    @Id
    @Column(name = "id", length = 60)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderJpaEntity order;

    @Column(name = "product_id", nullable = false, length = 60)
    private String productId;

    @Column(name = "product_variant_id", nullable = false, length = 60)
    private String productVariantId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "line_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineAmount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;
}
