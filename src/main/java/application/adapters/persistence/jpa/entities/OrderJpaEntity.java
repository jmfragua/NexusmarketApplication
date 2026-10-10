package application.adapters.persistence.jpa.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistence shape of an order.
 *
 * <p>{@code status} is stored verbatim and restored verbatim through {@code Order.reconstruct}
 * (RD-ALC-04): a reloaded order keeps the point of its lifecycle it had reached, it is not reborn
 * as a new one.</p>
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class OrderJpaEntity {

    @Id
    @Column(name = "id", length = 60)
    private String id;

    @Column(name = "buyer_id", nullable = false, length = 60)
    private String buyerId;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "delivery_address", nullable = false, length = 400)
    private String deliveryAddress;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderLineJpaEntity> lines = new ArrayList<>();
}
