package application.domain.valuesObjects;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * State of a product inside the catalogue.
 *
 * <p>Lifecycle: {@code PUBLISHED <-> SUSPENDED -> DISCONTINUED}. {@code DISCONTINUED} is terminal
 * and admits no way back to {@code PUBLISHED} (RD-CAT-04, RD-VO-13).</p>
 */
public enum ProductStatus implements Enumeration {

    PUBLISHED("Published. Visible in the public catalogue and available for sale."),
    SUSPENDED("Suspended. Temporarily withdrawn from sale."),
    DISCONTINUED("Discontinued. Definitively withdrawn.");

    private final String description;

    ProductStatus(String description) {
        this.description = description;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public String code() {
        return name();
    }

    /**
     * Declares the allowed transitions; every transition not declared here is forbidden
     * (RD-VO-12).
     */
    public boolean canTransitionTo(ProductStatus target) {
        return target != null && allowedTargets().contains(target);
    }

    /**
     * @return true for the terminal state, which admits no outgoing transition (RD-VO-13).
     */
    public boolean isTerminal() {
        return this == DISCONTINUED;
    }

    /**
     * Only a published product is visible in the public catalogue (RD-CAT-03).
     */
    public boolean isVisibleInCatalog() {
        return this == PUBLISHED;
    }

    private Set<ProductStatus> allowedTargets() {
        return switch (this) {
            case PUBLISHED -> EnumSet.of(SUSPENDED);
            case SUSPENDED -> EnumSet.of(PUBLISHED, DISCONTINUED);
            case DISCONTINUED -> Collections.emptySet();
        };
    }
}
