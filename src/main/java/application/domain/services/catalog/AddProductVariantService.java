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
 * Declares a new difference of a product: colour, size, model, and so on.
 *
 * <p>Variants are the unit of commercial selection: a product without variants cannot be sold
 * (RD-CAT-05).</p>
 */
public class AddProductVariantService {

    private static final String OPERATION = "add product variant";

    private final ProductRepositoryPort productRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    private final ValidateSellerOwnershipService validateSellerOwnershipService;

    public AddProductVariantService(ProductRepositoryPort productRepositoryPort,
                                    ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                    ValidateSellerOwnershipService validateSellerOwnershipService) {
        this.productRepositoryPort = Objects.requireNonNull(productRepositoryPort,
                "the product repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
        this.validateSellerOwnershipService = Objects.requireNonNull(validateSellerOwnershipService,
                "the ownership validation is mandatory");
    }

    public Product addVariant(Seller seller, Product product, ProductVariant variant) {
        validateRoleAuthorizationService.validate(seller, OPERATION, UserRole.SELLER);
        validateSellerOwnershipService.validateProduct(seller, product);
        product.addVariant(variant);
        return productRepositoryPort.save(product);
    }
}
