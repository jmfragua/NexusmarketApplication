package application.domain.services.catalog;

import application.domain.exceptions.EntityNotFoundException;
import application.domain.exceptions.ProductNotAvailableException;
import application.domain.models.Product;
import application.domain.models.Seller;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.UserRole;
import java.util.List;
import java.util.Objects;

/**
 * Consults the catalogue.
 *
 * <p>The public catalogue only exposes published products holding at least one variant (RD-CAT-03,
 * RD-CAT-05); a seller reaches their own products whatever their state (RD-CAT-01).</p>
 */
public class ConsultCatalogService {

    private static final String OPERATION = "consult own catalogue";

    private final ProductRepositoryPort productRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public ConsultCatalogService(ProductRepositoryPort productRepositoryPort,
                                 ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.productRepositoryPort = Objects.requireNonNull(productRepositoryPort,
                "the product repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
    }

    /**
     * @return the products visible in the public catalogue. No authenticated user is required: the
     *         catalogue is the public face of the marketplace.
     */
    public List<Product> consultPublicCatalog() {
        return productRepositoryPort.findVisibleInCatalog().stream()
                .filter(Product::isVisibleInCatalog)
                .toList();
    }

    /**
     * @throws ProductNotAvailableException when the product is not visible in the public catalogue.
     */
    public Product consultPublished(ProductId productId) {
        Product product = productRepositoryPort.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("product", productId));
        if (!product.isVisibleInCatalog()) {
            throw new ProductNotAvailableException(productId);
        }
        return product;
    }

    /**
     * @return every product of the seller, whatever its catalogue state.
     */
    public List<Product> consultOwnCatalog(Seller seller) {
        validateRoleAuthorizationService.validate(seller, OPERATION, UserRole.SELLER);
        return productRepositoryPort.findBySeller(seller.getIdentifier());
    }
}
