package application.domain.ports.out;

import application.domain.models.ProductReturn;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.ProductReturnId;
import java.util.List;
import java.util.Optional;

/**
 * Output port of the after sales returns. Every return originates in a line of an existing order
 * (RD-POS-01).
 */
public interface ProductReturnRepositoryPort {

    ProductReturn save(ProductReturn productReturn);

    /**
     * Stores a new return. Unlike {@link #save}, it never replaces an existing one: when the
     * identifier is already taken the storage refuses it, so a creation can never act as an update
     * (RD-ID-06).
     */
    ProductReturn create(ProductReturn productReturn);

    Optional<ProductReturn> findById(ProductReturnId productReturnId);

    List<ProductReturn> findByOrder(OrderId orderId);

    List<ProductReturn> findAll();
}
