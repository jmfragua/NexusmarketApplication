package application.adapters.persistence.jpa.mappers;

import application.adapters.persistence.jpa.entities.InvoiceJpaEntity;
import application.domain.models.Invoice;
import application.domain.valuesObjects.BuyerId;
import application.domain.valuesObjects.InvoiceId;
import application.domain.valuesObjects.Money;
import application.domain.valuesObjects.OrderId;

/**
 * Translates an invoice between its domain shape and its persistence shape. Its public constructor
 * restores it faithfully: an invoice holds no lifecycle to falsify.
 */
public final class InvoiceJpaMapper {

    private InvoiceJpaMapper() {
    }

    public static InvoiceJpaEntity toEntity(Invoice invoice) {
        InvoiceJpaEntity entity = new InvoiceJpaEntity();
        entity.setId(invoice.getIdentifier().getValue());
        entity.setOrderId(invoice.getOrderId().getValue());
        entity.setBuyerId(invoice.getBuyerId().getValue());
        entity.setTotalAmount(invoice.getTotalAmount().getAmount());
        entity.setCurrency(invoice.getTotalAmount().getCurrency());
        return entity;
    }

    public static Invoice toDomain(InvoiceJpaEntity entity) {
        return new Invoice(
                InvoiceId.of(entity.getId()),
                OrderId.of(entity.getOrderId()),
                BuyerId.of(entity.getBuyerId()),
                Money.of(entity.getTotalAmount(), entity.getCurrency()));
    }
}
