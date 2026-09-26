package application.domain.services.catalog;

import application.domain.models.Product;
import application.domain.models.Seller;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.services.authorization.ValidateSellerOwnershipService;
import application.domain.valuesObjects.UserRole;
import java.util.Objects;

/**
 * Suspends a product, temporarily withdrawing it from sale. A suspended product is no longer
 * visible in the public catalogue (RD-CAT-03) and can be published again later.
 */
public class SuspendProductService {

    private static final String OPERATION = "suspend product";

    private final ProductRepositoryPort productRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    private final ValidateSellerOwnershipService validateSellerOwnershipService;

    public SuspendProductService(ProductRepositoryPort productRepositoryPort,
                                 ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                 ValidateSellerOwnershipService validateSellerOwnershipService) {
        this.productRepositoryPort = Objects.requireNonNull(productRepositoryPort,
                "the product repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
        this.validateSellerOwnershipService = Objects.requireNonNull(validateSellerOwnershipService,
                "the ownership validation is mandatory");
    }

    public Product suspend(Seller seller, Product product) {
        validateRoleAuthorizationService.validate(seller, OPERATION, UserRole.SELLER);
        validateSellerOwnershipService.validateProduct(seller, product);
        seller.suspendProduct(product);
        return productRepositoryPort.save(product);
    }
}
