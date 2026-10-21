package application.adapters.persistence.jpa;

import application.adapters.persistence.jpa.mappers.ProductJpaMapper;
import application.adapters.persistence.jpa.repositories.SpringDataJpaProductRepository;
import application.domain.models.Product;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.ProductStatus;
import application.domain.valuesObjects.ProductVariantId;
import application.domain.valuesObjects.SellerId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Relational adapter of {@link ProductRepositoryPort}.
 */
@Repository
public class ProductJpaAdapter implements ProductRepositoryPort {

    private final SpringDataJpaProductRepository productRepository;

    private final JpaInserter jpaInserter;

    public ProductJpaAdapter(SpringDataJpaProductRepository productRepository,
                             JpaInserter jpaInserter) {
        this.productRepository = Objects.requireNonNull(productRepository, "the product repository is mandatory");
        this.jpaInserter = Objects.requireNonNull(jpaInserter, "the inserter is mandatory");
    }

    @Override
    public Product save(Product product) {
        productRepository.save(ProductJpaMapper.toEntity(product));
        return product;
    }

    @Override
    public Product create(Product product) {
        jpaInserter.insert(ProductJpaMapper.toEntity(product));
        return product;
    }

    @Override
    public Optional<Product> findById(ProductId productId) {
        return productRepository.findById(productId.getValue()).map(ProductJpaMapper::toDomain);
    }

    @Override
    public List<Product> findBySeller(SellerId sellerId) {
        return productRepository.findBySellerId(sellerId.getValue()).stream()
                .map(ProductJpaMapper::toDomain)
                .toList();
    }

    /**
     * Filters by catalogue status only. The second condition of visibility — holding at least one
     * variant (RD-CAT-05) — is applied by the domain service through
     * {@code Product.isVisibleInCatalog()}, so the rule stays in the domain rather than being
     * duplicated as a query.
     */
    @Override
    public List<Product> findVisibleInCatalog() {
        return productRepository.findByStatus(ProductStatus.PUBLISHED.code()).stream()
                .map(ProductJpaMapper::toDomain)
                .toList();
    }

    @Override
    public List<Product> findAll() {
        return productRepository.findAll().stream()
                .map(ProductJpaMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsVariant(ProductVariantId variantId) {
        return productRepository.existsByVariantsId(variantId.getValue());
    }
}
