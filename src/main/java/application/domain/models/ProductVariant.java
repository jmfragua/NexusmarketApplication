package application.domain.models;

import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.ProductVariantId;
import application.domain.valuesObjects.VariantAttribute;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * Concrete difference of a product: colour, size, model, and so on. It is the unit on which the
 * commercial selection is made (RD-CAT-05).
 */
@Getter
public class ProductVariant extends DomainEntity<ProductVariantId> {

    private final ProductId productId;

    private final List<VariantAttribute> attributes = new ArrayList<>();

    public ProductVariant(ProductVariantId identifier, ProductId productId, Collection<VariantAttribute> attributes) {
        super(identifier);
        this.productId = Objects.requireNonNull(productId, "owning product is mandatory");
        Objects.requireNonNull(attributes, "the attributes of the variant are mandatory");
        if (attributes.isEmpty()) {
            throw new IllegalArgumentException("a variant declares at least one characteristic");
        }
        attributes.forEach(this::addAttribute);
    }

    /**
     * @return the characteristics defining the variant, as an unmodifiable view.
     */
    public List<VariantAttribute> getAttributes() {
        return Collections.unmodifiableList(attributes);
    }

    /**
     * @return a readable composition of the characteristics of the variant.
     */
    public String describe() {
        return attributes.stream()
                .map(VariantAttribute::toString)
                .collect(Collectors.joining(", "));
    }

    /**
     * @return true when the variant declares every requested characteristic.
     */
    public boolean matches(Collection<VariantAttribute> requested) {
        return requested != null && !requested.isEmpty() && attributes.containsAll(requested);
    }

    /**
     * @throws IllegalArgumentException when the characteristic is already declared: a variant never
     *                                  declares the same characteristic twice.
     */
    private void addAttribute(VariantAttribute attribute) {
        Objects.requireNonNull(attribute, "variant attribute is mandatory");
        boolean duplicated = attributes.stream().anyMatch(declared -> declared.hasSameName(attribute));
        if (duplicated) {
            throw new IllegalArgumentException("the variant already declares the characteristic " + attribute.getName());
        }
        attributes.add(attribute);
    }
}
