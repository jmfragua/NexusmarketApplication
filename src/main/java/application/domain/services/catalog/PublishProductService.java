package application.domain.services.catalog;

import application.domain.models.Product;
import application.domain.models.Seller;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.services.authorization.ValidateSellerOwnershipService;
import application.domain.valuesObjects.UserRole;
import java.util.Objects;

/**
 * Publishes a product, which makes it visible in the public catalogue (RD-CAT-03).
 *
 * <p>Only a suspended product returns to {@code PUBLISHED}; a discontinued one never does, because
 * that state is terminal (RD-CAT-04).</p>
 */
public class PublishProductService {

    private static final String OPERATION = "publish product";

    private final ProductRepositoryPort productRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    private final ValidateSellerOwnershipService validateSellerOwnershipService;

    public PublishProductService(ProductRepositoryPort productRepositoryPort,
                                 ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                 ValidateSellerOwnershipService validateSellerOwnershipService) {
        this.productRepositoryPort = Objects.requireNonNull(productRepositoryPort,
                "the product repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
        this.validateSellerOwnershipService = Objects.requireNonNull(validateSellerOwnershipService,
                "the ownership validation is mandatory");
    }

    public Product publish(Seller seller, Product product) {
        validateRoleAuthorizationService.validate(seller, OPERATION, UserRole.SELLER);
        validateSellerOwnershipService.validateProduct(seller, product);
        seller.publishProduct(product);
        return productRepositoryPort.save(product);
    }
}
