package application.domain.ports.out;

import application.domain.models.Product;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.SellerId;
import java.util.List;
import java.util.Optional;

/**
 * Output port of the catalogue. Every product belongs to a single seller (RD-CAT-01) and only the
 * published ones are visible in the public catalogue (RD-CAT-03).
 */
public interface ProductRepositoryPort {

    Product save(Product product);

    Optional<Product> findById(ProductId productId);

    List<Product> findBySeller(SellerId sellerId);

    List<Product> findVisibleInCatalog();

    List<Product> findAll();
}
