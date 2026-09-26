package application.domain.services.catalog;

import application.domain.models.Product;
import application.domain.models.ProductVariant;
import application.domain.models.Seller;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.services.authorization.ValidateSellerOwnershipService;
import application.domain.valuesObjects.UserRole;
import java.util.Objects;

/**
 * Withdraws a declared difference of a product.
 *
 * <p>A discontinued product cannot be modified (RD-CAT-04), and a product left without variants
 * stops being available for sale (RD-CAT-05).</p>
 */
public class RemoveProductVariantService {

    private static final String OPERATION = "remove product variant";

    private final ProductRepositoryPort productRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    private final ValidateSellerOwnershipService validateSellerOwnershipService;

    public RemoveProductVariantService(ProductRepositoryPort productRepositoryPort,
                                       ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                       ValidateSellerOwnershipService validateSellerOwnershipService) {
        this.productRepositoryPort = Objects.requireNonNull(productRepositoryPort,
                "the product repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
        this.validateSellerOwnershipService = Objects.requireNonNull(validateSellerOwnershipService,
                "the ownership validation is mandatory");
    }

    public Product removeVariant(Seller seller, Product product, ProductVariant variant) {
        validateRoleAuthorizationService.validate(seller, OPERATION, UserRole.SELLER);
        validateSellerOwnershipService.validateProduct(seller, product);
        product.removeVariant(variant);
        return productRepositoryPort.save(product);
    }
}
