package application.domain.ports.out;

import application.domain.models.Invoice;
import application.domain.valuesObjects.BuyerId;
import application.domain.valuesObjects.InvoiceId;
import application.domain.valuesObjects.OrderId;
import java.util.List;
import java.util.Optional;

/**
 * Output port of the invoicing. An order is invoiced once, out of its paid state (RD-FAC-01).
 */
public interface InvoiceRepositoryPort {

    Invoice save(Invoice invoice);

    /**
     * Stores a new invoice. Unlike {@link #save}, it never replaces an existing one: when the
     * identifier is already taken the storage refuses it, so a creation can never act as an update
     * (RD-ID-06).
     */
    Invoice create(Invoice invoice);

    Optional<Invoice> findById(InvoiceId invoiceId);

    Optional<Invoice> findByOrder(OrderId orderId);

    List<Invoice> findByBuyer(BuyerId buyerId);

    List<Invoice> findAll();
}
