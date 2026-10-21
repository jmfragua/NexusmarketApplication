package application.adapters.persistence.jpa.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

/**
 * Persistence shape of a product variant.
 *
 * <p>Its attributes are stored as a name/value map, which mirrors the domain rule that a variant
 * never declares the same characteristic twice: the map key enforces it in storage too.</p>
 */
// Immutable: a variant is never edited once added: it is removed and replaced (RD-CAT-05).
// A merge that reaches an existing row, for instance through an identifier reused by another
// request, can therefore never rewrite it or move it to another parent.
@Immutable
@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
public class ProductVariantJpaEntity {

    @Id
    @Column(name = "id", length = 60)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductJpaEntity product;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_variant_attributes", joinColumns = @JoinColumn(name = "variant_id"))
    @MapKeyColumn(name = "attribute_name", length = 100)
    @Column(name = "attribute_value", length = 200)
    private Map<String, String> attributes = new LinkedHashMap<>();
}
