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

    Optional<ProductReturn> findById(ProductReturnId productReturnId);

    List<ProductReturn> findByOrder(OrderId orderId);

    List<ProductReturn> findAll();
}
