package application.domain.models;

import application.domain.valuesObjects.EmailAddress;
import application.domain.valuesObjects.FullName;
import application.domain.valuesObjects.IdentityDocument;
import application.domain.valuesObjects.SellerId;
import application.domain.valuesObjects.UserRole;
import application.domain.valuesObjects.UserStatus;
import java.util.Objects;

/**
 * User responsible for selling products: registers them and manages them.
 *
 * <p>Sellers cannot register themselves: a seller is created by the administrator together with
 * their first warehouse (RD-ROL-05). The domain expresses that rule through
 * {@link #register(SellerId, FullName, EmailAddress, IdentityDocument, UserStatus)}, which is the
 * only way to build a seller and always demands the first warehouse.</p>
 */
public class Seller extends User<SellerId> {

    private Seller(SellerId identifier,
                   FullName fullName,
                   EmailAddress email,
                   IdentityDocument identityDocument,
                   UserStatus status) {
        super(identifier, fullName, email, identityDocument, UserRole.SELLER, status);
    }

    /**
     * Incorporates a seller. Reserved to the administrator role and always performed together with
     * the first warehouse of the seller (RD-ROL-05).
     *
     * @param administrator  administrator performing the incorporation.
     * @param firstWarehouse first warehouse of the seller, registered along with them.
     * @return the seller, already owner of the supplied warehouse.
     */
    public static Seller register(SellerId identifier,
                                  FullName fullName,
                                  EmailAddress email,
                                  IdentityDocument identityDocument,
                                  UserStatus status,
                                  SellerWarehouse firstWarehouse,
                                  User<?> administrator) {
        Objects.requireNonNull(administrator, "an administrator is required to register a seller");
        Objects.requireNonNull(firstWarehouse, "a seller is registered along with their first warehouse");
        if (administrator.getRole() != UserRole.ADMINISTRATOR) {
            throw new IllegalStateException("only an administrator can register a seller");
        }
        if (!administrator.isActive()) {
            throw new IllegalStateException("the administrator must be active to register a seller");
        }
        if (!firstWarehouse.belongsTo(identifier)) {
            throw new IllegalArgumentException("the first warehouse belongs to another seller");
        }
        return new Seller(identifier, fullName, email, identityDocument, status);
    }

    /**
     * Registers a product in the own catalogue of this seller.
     *
     * @throws IllegalStateException when the product was registered under another seller.
     */
    public void registerProduct(Product product) {
        Objects.requireNonNull(product, "product is mandatory");
        if (!ownsProduct(product)) {
            throw new IllegalStateException("the product does not belong to seller " + getIdentifier());
        }
    }

    public void publishProduct(Product product) {
        requireOwnership(product);
        product.publish();
    }

    public void suspendProduct(Product product) {
        requireOwnership(product);
        product.suspend();
    }

    public void discontinueProduct(Product product) {
        requireOwnership(product);
        product.discontinue();
    }

    /**
     * Supports RG-03: a seller only manages their own warehouses.
     */
    public boolean ownsWarehouse(Warehouse warehouse) {
        return warehouse instanceof SellerWarehouse sellerWarehouse && sellerWarehouse.belongsTo(this);
    }

    /**
     * Supports RG-03: every product belongs to a single seller and only that seller manages it
     * (RD-CAT-01).
     */
    public boolean ownsProduct(Product product) {
        return product != null && product.getSellerId().equals(getIdentifier());
    }

    @Override
    protected boolean ownsResource(DomainEntity<?> resource) {
        if (resource instanceof Product product) {
            return ownsProduct(product);
        }
        if (resource instanceof Warehouse warehouse) {
            return ownsWarehouse(warehouse);
        }
        return false;
    }

    private void requireOwnership(Product product) {
        if (!ownsProduct(product)) {
            throw new IllegalStateException("the product does not belong to seller " + getIdentifier());
        }
    }
}
