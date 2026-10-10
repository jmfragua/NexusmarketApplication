package application.adapters.persistence.jpa.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistence shape of the provisional selection of a buyer. It commits no stock (RD-PED-04), so it
 * holds no reference whatsoever to inventory.
 */
@Entity
@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor
public class CartJpaEntity {

    @Id
    @Column(name = "id", length = 60)
    private String id;

    @Column(name = "buyer_id", nullable = false, length = 60)
    private String buyerId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CartItemJpaEntity> items = new ArrayList<>();
}
