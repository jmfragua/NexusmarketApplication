package application.domain.support;

import application.domain.models.Product;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.ProductStatus;
import application.domain.valuesObjects.SellerId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory double of {@link ProductRepositoryPort}.
 */
public class InMemoryProductRepository implements ProductRepositoryPort {

    private final Map<String, Product> products = new LinkedHashMap<>();

    @Override
    public Product save(Product product) {
        products.put(product.getIdentifier().getValue(), product);
        return product;
    }

    @Override
    public Optional<Product> findById(ProductId productId) {
        return Optional.ofNullable(products.get(productId.getValue()));
    }

    @Override
    public List<Product> findBySeller(SellerId sellerId) {
        return products.values().stream()
                .filter(product -> product.getSellerId().equals(sellerId))
                .toList();
    }

    /**
     * Filters by status only, exactly like the relational adapter: the "at least one variant" half
     * of visibility (RD-CAT-05) is applied by the domain service, not by the repository.
     */
    @Override
    public List<Product> findVisibleInCatalog() {
        return products.values().stream()
                .filter(product -> product.getStatus() == ProductStatus.PUBLISHED)
                .toList();
    }

    @Override
    public List<Product> findAll() {
        return new ArrayList<>(products.values());
    }
}
