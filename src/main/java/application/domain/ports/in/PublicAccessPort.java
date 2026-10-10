package application.domain.ports.in;

import application.domain.models.Buyer;
import application.domain.models.Product;
import application.domain.models.User;
import application.domain.valuesObjects.CartId;
import application.domain.valuesObjects.EmailAddress;
import application.domain.valuesObjects.ProductId;
import java.util.List;

/**
 * Entry contract of the operations that need no previous authentication.
 *
 * <p>It covers the public catalogue, which is the public face of the marketplace (RD-CAT-03), the
 * self registration of buyers and the identification of the user who is going to operate. Sellers
 * are absent on purpose: they cannot register themselves, the administrator incorporates them
 * (RD-ROL-05).</p>
 */
public interface PublicAccessPort {

    /**
     * Identifies the user behind the email address and guarantees they are allowed to operate
     * (RG-01).
     */
    User<?> login(EmailAddress email);

    /**
     * Incorporates a buyer to the platform together with their active cart, keeping the email
     * address and the identity document unique (RD-ID-03, RD-ID-04, RD-PED-04).
     */
    Buyer registerBuyer(Buyer buyer, CartId cartId);

    /**
     * @return the products visible in the public catalogue.
     */
    List<Product> consultPublicCatalog();

    /**
     * @return a published product of the public catalogue.
     */
    Product consultPublishedProduct(ProductId productId);
}
