package application.domain.ports.out;

import application.domain.models.Seller;
import application.domain.valuesObjects.SellerId;
import java.util.List;
import java.util.Optional;

/**
 * Output port of the sellers incorporated by the administrator (RD-ROL-05).
 */
public interface SellerRepositoryPort {

    Seller save(Seller seller);

    Optional<Seller> findById(SellerId sellerId);

    List<Seller> findAll();
}
