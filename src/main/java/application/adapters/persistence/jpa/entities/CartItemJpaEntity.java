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
 * Persistence shape of a line of the provisional selection.
 */
@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
public class CartItemJpaEntity {

    @Id
    @Column(name = "id", length = 60)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private CartJpaEntity cart;

    @Column(name = "product_id", nullable = false, length = 60)
    private String productId;

    @Column(name = "product_variant_id", nullable = false, length = 60)
    private String productVariantId;

    @Column(name = "quantity", nullable = false)
    private int quantity;
}
