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

    /**
     * Stores a new seller. Unlike {@link #save}, it never replaces an existing one: when the
     * identifier is already taken the storage refuses it, so a creation can never act as an update
     * (RD-ID-06).
     */
    Seller create(Seller seller);

    Optional<Seller> findById(SellerId sellerId);

    List<Seller> findAll();
}
