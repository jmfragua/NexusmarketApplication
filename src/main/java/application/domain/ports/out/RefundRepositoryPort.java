package application.domain.ports.out;

import application.domain.models.Refund;
import application.domain.valuesObjects.ProductReturnId;
import application.domain.valuesObjects.RefundId;
import java.util.List;
import java.util.Optional;

/**
 * Output port of the refunds. Every refund derives from a return and is applied on the invoice of
 * the corresponding order (RD-POS-02).
 */
public interface RefundRepositoryPort {

    Refund save(Refund refund);

    /**
     * Stores a new refund. Unlike {@link #save}, it never replaces an existing one: when the
     * identifier is already taken the storage refuses it, so a creation can never act as an update
     * (RD-ID-06).
     */
    Refund create(Refund refund);

    Optional<Refund> findById(RefundId refundId);

    Optional<Refund> findByProductReturn(ProductReturnId productReturnId);

    List<Refund> findAll();
}
