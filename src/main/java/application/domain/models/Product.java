package application.domain.models;

import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.ProductStatus;
import application.domain.valuesObjects.ProductType;
import application.domain.valuesObjects.SellerId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.Getter;

/**
 * Physical or digital good offered in the catalogue.
 *
 * <p>The catalogue tells apart physical products, which need inventory and dispatch, from digital
 * products, delivered immediately once the payment is confirmed. The type is set when the product
 * is registered and never changes during its life (RD-CAT-02).</p>
 */
@Getter
public abstract class Product extends DomainEntity<ProductId> {

    private final SellerId sellerId;

    private final ProductType productType;

    private final List<ProductVariant> variants = new ArrayList<>();

    private ProductStatus status;

    protected Product(ProductId identifier, SellerId sellerId, ProductType productType) {
        super(identifier);
        this.sellerId = Objects.requireNonNull(sellerId, "owning seller is mandatory");
        this.productType = Objects.requireNonNull(productType, "product type is mandatory");
        this.status = ProductStatus.PUBLISHED;
    }

    /**
     * @return the differences declared for this product, as an unmodifiable view.
     */
    public List<ProductVariant> getVariants() {
        return Collections.unmodifiableList(variants);
    }

    public void publish() {
        changeStatus(ProductStatus.PUBLISHED);
    }

    public void suspend() {
        changeStatus(ProductStatus.SUSPENDED);
    }

    /**
     * Withdraws the product definitively. {@code DISCONTINUED} is terminal: there is no way back to
     * {@code PUBLISHED} (RD-CAT-04).
     */
    public void discontinue() {
        changeStatus(ProductStatus.DISCONTINUED);
    }

    public void addVariant(ProductVariant variant) {
        Objects.requireNonNull(variant, "variant is mandatory");
        if (!variant.getProductId().equals(getIdentifier())) {
            throw new IllegalArgumentException("the variant belongs to another product");
        }
        if (status.isTerminal()) {
            throw new IllegalStateException("a discontinued product does not admit new variants");
        }
        if (variants.contains(variant)) {
            return;
        }
        variants.add(variant);
    }

    public void removeVariant(ProductVariant variant) {
        if (status.isTerminal()) {
            throw new IllegalStateException("a discontinued product cannot be modified");
        }
        variants.remove(variant);
    }

    /**
     * Only a published product is visible in the public catalogue (RD-CAT-03), and variants are the
     * unit of commercial selection: a product without variants cannot be sold (RD-CAT-05).
     */
    public boolean isVisibleInCatalog() {
        return status.isVisibleInCatalog() && !variants.isEmpty();
    }

    /**
     * @return true only for physical products (RD-INV-05).
     */
    public abstract boolean requiresInventory();

    private void changeStatus(ProductStatus target) {
        if (this.status == target) {
            return;
        }
        if (!this.status.canTransitionTo(target)) {
            throw new IllegalStateException("product cannot move from " + this.status + " to " + target);
        }
        this.status = target;
    }
}
