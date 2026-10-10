package application.adapters.persistence.jpa.mappers;

import application.adapters.persistence.jpa.entities.RefundJpaEntity;
import application.domain.models.Refund;
import application.domain.valuesObjects.InvoiceId;
import application.domain.valuesObjects.Money;
import application.domain.valuesObjects.ProductReturnId;
import application.domain.valuesObjects.RefundId;

/**
 * Translates a refund between its domain shape and its persistence shape.
 *
 * <p>Restored through its public constructor rather than {@code issueFor}: that factory needs the
 * live return and invoice to validate a <em>new</em> refund (RD-POS-02), which a reload does not
 * have at hand and does not need — the stored refund already satisfied those rules.</p>
 */
public final class RefundJpaMapper {

    private RefundJpaMapper() {
    }

    public static RefundJpaEntity toEntity(Refund refund) {
        RefundJpaEntity entity = new RefundJpaEntity();
        entity.setId(refund.getIdentifier().getValue());
        entity.setProductReturnId(refund.getProductReturnId().getValue());
        entity.setInvoiceId(refund.getInvoiceId().getValue());
        entity.setAmount(refund.getAmount().getAmount());
        entity.setCurrency(refund.getAmount().getCurrency());
        return entity;
    }

    public static Refund toDomain(RefundJpaEntity entity) {
        return new Refund(
                RefundId.of(entity.getId()),
                ProductReturnId.of(entity.getProductReturnId()),
                InvoiceId.of(entity.getInvoiceId()),
                Money.of(entity.getAmount(), entity.getCurrency()));
    }
}
