package application.domain.valuesObjects;

import lombok.Getter;

/**
 * Concrete difference that characterizes a product variant: colour, size, model, and so on.
 * Variants are declared as a list of these differences.
 */
@Getter
public final class VariantAttribute extends ValueObject {

    private final String name;

    private final String value;

    private VariantAttribute(String name, String value) {
        this.name = requireNonBlank(name, "variant attribute name");
        this.value = requireNonBlank(value, "variant attribute value");
    }

    public static VariantAttribute of(String name, String value) {
        return new VariantAttribute(name, value);
    }

    /**
     * Two attributes describe the same characteristic when they share the name, whatever their
     * value. Supports the rule "a variant never declares the same characteristic twice".
     */
    public boolean hasSameName(VariantAttribute other) {
        return other != null && this.name.equalsIgnoreCase(other.name);
    }

    @Override
    protected Object[] equalityComponents() {
        return new Object[] { name, value };
    }

    @Override
    public String toString() {
        return name + ": " + value;
    }
}
