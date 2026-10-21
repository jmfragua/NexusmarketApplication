package application.adapters.persistence.jpa;

import application.adapters.persistence.jpa.mappers.ProductReturnJpaMapper;
import application.adapters.persistence.jpa.repositories.SpringDataJpaProductReturnRepository;
import application.domain.models.ProductReturn;
import application.domain.ports.out.ProductReturnRepositoryPort;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.ProductReturnId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Relational adapter of {@link ProductReturnRepositoryPort}.
 */
@Repository
public class ProductReturnJpaAdapter implements ProductReturnRepositoryPort {

    private final SpringDataJpaProductReturnRepository productReturnRepository;

    private final JpaInserter jpaInserter;

    public ProductReturnJpaAdapter(SpringDataJpaProductReturnRepository productReturnRepository,
                                   JpaInserter jpaInserter) {
        this.productReturnRepository = Objects.requireNonNull(productReturnRepository,
                "the product return repository is mandatory");
        this.jpaInserter = Objects.requireNonNull(jpaInserter, "the inserter is mandatory");
    }

    @Override
    public ProductReturn save(ProductReturn productReturn) {
        productReturnRepository.save(ProductReturnJpaMapper.toEntity(productReturn));
        return productReturn;
    }

    @Override
    public ProductReturn create(ProductReturn productReturn) {
        jpaInserter.insert(ProductReturnJpaMapper.toEntity(productReturn));
        return productReturn;
    }

    @Override
    public Optional<ProductReturn> findById(ProductReturnId productReturnId) {
        return productReturnRepository.findById(productReturnId.getValue())
                .map(ProductReturnJpaMapper::toDomain);
    }

    @Override
    public List<ProductReturn> findByOrder(OrderId orderId) {
        return productReturnRepository.findByOrderId(orderId.getValue()).stream()
                .map(ProductReturnJpaMapper::toDomain)
                .toList();
    }

    @Override
    public List<ProductReturn> findAll() {
        return productReturnRepository.findAll().stream()
                .map(ProductReturnJpaMapper::toDomain)
                .toList();
    }
}
