package application.adapters.persistence.jpa;

import application.adapters.persistence.jpa.mappers.InvoiceJpaMapper;
import application.adapters.persistence.jpa.repositories.SpringDataJpaInvoiceRepository;
import application.domain.models.Invoice;
import application.domain.ports.out.InvoiceRepositoryPort;
import application.domain.valuesObjects.BuyerId;
import application.domain.valuesObjects.InvoiceId;
import application.domain.valuesObjects.OrderId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Relational adapter of {@link InvoiceRepositoryPort}.
 */
@Repository
public class InvoiceJpaAdapter implements InvoiceRepositoryPort {

    private final SpringDataJpaInvoiceRepository invoiceRepository;

    public InvoiceJpaAdapter(SpringDataJpaInvoiceRepository invoiceRepository) {
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository, "the invoice repository is mandatory");
    }

    @Override
    public Invoice save(Invoice invoice) {
        invoiceRepository.save(InvoiceJpaMapper.toEntity(invoice));
        return invoice;
    }

    @Override
    public Optional<Invoice> findById(InvoiceId invoiceId) {
        return invoiceRepository.findById(invoiceId.getValue()).map(InvoiceJpaMapper::toDomain);
    }

    @Override
    public Optional<Invoice> findByOrder(OrderId orderId) {
        return invoiceRepository.findByOrderId(orderId.getValue()).map(InvoiceJpaMapper::toDomain);
    }

    @Override
    public List<Invoice> findByBuyer(BuyerId buyerId) {
        return invoiceRepository.findByBuyerId(buyerId.getValue()).stream()
                .map(InvoiceJpaMapper::toDomain)
                .toList();
    }

    @Override
    public List<Invoice> findAll() {
        return invoiceRepository.findAll().stream()
                .map(InvoiceJpaMapper::toDomain)
                .toList();
    }
}
