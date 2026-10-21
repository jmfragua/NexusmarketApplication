package application.domain.support;

import application.domain.models.Seller;
import application.domain.ports.out.SellerRepositoryPort;
import application.domain.valuesObjects.SellerId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory double of {@link SellerRepositoryPort}.
 */
public class InMemorySellerRepository implements SellerRepositoryPort {

    private final Map<String, Seller> sellers = new LinkedHashMap<>();

    @Override
    public Seller save(Seller seller) {
        sellers.put(seller.getIdentifier().getValue(), seller);
        return seller;
    }

    @Override
    public Seller create(Seller seller) {
        if (sellers.containsKey(seller.getIdentifier().getValue())) {
            throw new IllegalStateException("identifier already taken: " + seller.getIdentifier());
        }
        return save(seller);
    }

    @Override
    public Optional<Seller> findById(SellerId sellerId) {
        return Optional.ofNullable(sellers.get(sellerId.getValue()));
    }

    @Override
    public List<Seller> findAll() {
        return new ArrayList<>(sellers.values());
    }
}
