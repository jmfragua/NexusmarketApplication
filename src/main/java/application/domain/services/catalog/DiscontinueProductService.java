package application.domain.services.catalog;

import application.domain.models.Product;
import application.domain.models.Seller;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.services.authorization.ValidateSellerOwnershipService;
import application.domain.valuesObjects.UserRole;
import java.util.Objects;

/**
 * Withdraws a product definitively from the catalogue.
 *
 * <p>{@code DISCONTINUED} is a terminal state: it admits no way back to {@code PUBLISHED}
 * (RD-CAT-04, RD-VO-13).</p>
 */
public class DiscontinueProductService {

    private static final String OPERATION = "discontinue product";

    private final ProductRepositoryPort productRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    private final ValidateSellerOwnershipService validateSellerOwnershipService;

    public DiscontinueProductService(ProductRepositoryPort productRepositoryPort,
                                     ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                     ValidateSellerOwnershipService validateSellerOwnershipService) {
        this.productRepositoryPort = Objects.requireNonNull(productRepositoryPort,
                "the product repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
        this.validateSellerOwnershipService = Objects.requireNonNull(validateSellerOwnershipService,
                "the ownership validation is mandatory");
    }

    public Product discontinue(Seller seller, Product product) {
        validateRoleAuthorizationService.validate(seller, OPERATION, UserRole.SELLER);
        validateSellerOwnershipService.validateProduct(seller, product);
        seller.discontinueProduct(product);
        return productRepositoryPort.save(product);
    }
}
