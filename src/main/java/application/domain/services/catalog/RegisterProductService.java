package application.domain.services.catalog;

import application.domain.models.Product;
import application.domain.models.Seller;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.services.authorization.ValidateSellerOwnershipService;
import application.domain.valuesObjects.UserRole;
import java.util.Objects;

/**
 * Registers a product in the own catalogue of a seller.
 *
 * <p>The registration of products belongs to the seller (RD-ROL-06) and every product belongs to a
 * single seller, who is the only one managing it (RD-CAT-01). The type of the product is fixed at
 * registration and never changes during its life (RD-CAT-02).</p>
 */
public class RegisterProductService {

    private static final String OPERATION = "register product";

    private final ProductRepositoryPort productRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    private final ValidateSellerOwnershipService validateSellerOwnershipService;

    public RegisterProductService(ProductRepositoryPort productRepositoryPort,
                                  ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                  ValidateSellerOwnershipService validateSellerOwnershipService) {
        this.productRepositoryPort = Objects.requireNonNull(productRepositoryPort,
                "the product repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
        this.validateSellerOwnershipService = Objects.requireNonNull(validateSellerOwnershipService,
                "the ownership validation is mandatory");
    }

    /**
     * @param seller  owner of the catalogue where the product is registered.
     * @param product product already built as physical or digital.
     * @return the registered product.
     */
    public Product register(Seller seller, Product product) {
        validateRoleAuthorizationService.validate(seller, OPERATION, UserRole.SELLER);
        validateSellerOwnershipService.validateProduct(seller, product);
        seller.registerProduct(product);
        return productRepositoryPort.save(product);
    }
}
