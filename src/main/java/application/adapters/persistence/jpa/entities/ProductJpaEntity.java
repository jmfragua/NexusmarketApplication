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
 * Persistence shape of a catalogue product.
 *
 * <p>Variants are a real {@code @OneToMany} because they are part of this aggregate: they have no
 * repository of their own and are never loaded without their product. The owning seller, in
 * contrast, is a plain identifier column — it belongs to another aggregate.</p>
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class ProductJpaEntity {

    @Id
    @Column(name = "id", length = 60)
    private String id;

    @Column(name = "seller_id", nullable = false, length = 60)
    private String sellerId;

    @Column(name = "product_type", nullable = false, length = 20)
    private String productType;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ProductVariantJpaEntity> variants = new ArrayList<>();
}
