package application.adapters.persistence.jpa;

import application.adapters.persistence.jpa.mappers.RefundJpaMapper;
import application.adapters.persistence.jpa.repositories.SpringDataJpaRefundRepository;
import application.domain.models.Refund;
import application.domain.ports.out.RefundRepositoryPort;
import application.domain.valuesObjects.ProductReturnId;
import application.domain.valuesObjects.RefundId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Relational adapter of {@link RefundRepositoryPort}.
 */
@Repository
public class RefundJpaAdapter implements RefundRepositoryPort {

    private final SpringDataJpaRefundRepository refundRepository;

    public RefundJpaAdapter(SpringDataJpaRefundRepository refundRepository) {
        this.refundRepository = Objects.requireNonNull(refundRepository, "the refund repository is mandatory");
    }

    @Override
    public Refund save(Refund refund) {
        refundRepository.save(RefundJpaMapper.toEntity(refund));
        return refund;
    }

    @Override
    public Optional<Refund> findById(RefundId refundId) {
        return refundRepository.findById(refundId.getValue()).map(RefundJpaMapper::toDomain);
    }

    @Override
    public Optional<Refund> findByProductReturn(ProductReturnId productReturnId) {
        return refundRepository.findByProductReturnId(productReturnId.getValue())
                .map(RefundJpaMapper::toDomain);
    }

    @Override
    public List<Refund> findAll() {
        return refundRepository.findAll().stream()
                .map(RefundJpaMapper::toDomain)
                .toList();
    }
}
