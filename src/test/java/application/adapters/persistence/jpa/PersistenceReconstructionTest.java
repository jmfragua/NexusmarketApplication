package application.adapters.persistence.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import application.domain.models.InventoryItem;
import application.domain.models.Order;
import application.domain.models.PhysicalProduct;
import application.domain.models.Product;
import application.domain.models.Seller;
import application.domain.ports.out.InventoryItemRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.ports.out.SellerRepositoryPort;
import application.domain.support.DomainFixtures;
import application.domain.valuesObjects.InventoryItemId;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.ProductStatus;
import application.domain.valuesObjects.OrderStatus;
import application.domain.valuesObjects.SellerId;
import application.domain.valuesObjects.StockCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies the round trip of the four aggregates that need a reconstruction factory (RD-ALC-04).
 *
 * <p>These are the cases where a naive mapper silently corrupts data: reloading an order would reset
 * it to {@code PENDING_PAYMENT}, a discontinued product would come back published, and replaying the
 * commands on a stock record would fabricate movements that never happened. Each test therefore
 * stores a state that the creation-time rules would refuse to produce, and asserts it survives.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PersistenceReconstructionTest {

    @Autowired
    private OrderRepositoryPort orderRepositoryPort;

    @Autowired
    private ProductRepositoryPort productRepositoryPort;

    @Autowired
    private SellerRepositoryPort sellerRepositoryPort;

    @Autowired
    private InventoryItemRepositoryPort inventoryItemRepositoryPort;

    @Test
    @DisplayName("a dispatched order comes back dispatched, not reset to pending payment")
    void orderKeepsItsStatus() {
        Order dispatched = DomainFixtures.orderIn(DomainFixtures.buyer(), OrderStatus.DISPATCHED);
        orderRepositoryPort.save(dispatched);

        Order reloaded = orderRepositoryPort.findById(OrderId.of("order-1")).orElseThrow();

        assertEquals(OrderStatus.DISPATCHED, reloaded.getStatus());
        assertEquals(1, reloaded.getLines().size());
        assertEquals(dispatched.getTotalAmount(), reloaded.getTotalAmount());
    }

    @Test
    @DisplayName("a delivered order comes back immutable")
    void deliveredOrderComesBackImmutable() {
        orderRepositoryPort.save(DomainFixtures.orderIn(DomainFixtures.buyer(), OrderStatus.DELIVERED));

        Order reloaded = orderRepositoryPort.findById(OrderId.of("order-1")).orElseThrow();

        assertEquals(OrderStatus.DELIVERED, reloaded.getStatus());
        assertTrue(!reloaded.isModifiable(), "the terminal state survives the round trip (RD-PED-02)");
    }

    @Test
    @DisplayName("a discontinued product comes back discontinued, with its variants")
    void productKeepsStatusAndVariants() {
        Seller seller = DomainFixtures.seller();
        PhysicalProduct product = PhysicalProduct.reconstruct(
                ProductId.of("product-9"),
                seller.getIdentifier(),
                ProductStatus.DISCONTINUED,
                java.util.List.of(DomainFixtures.variant(ProductId.of("product-9"))));
        productRepositoryPort.save(product);

        Product reloaded = productRepositoryPort.findById(ProductId.of("product-9")).orElseThrow();

        assertEquals(ProductStatus.DISCONTINUED, reloaded.getStatus());
        assertEquals(1, reloaded.getVariants().size());
        assertEquals("color", reloaded.getVariants().get(0).getAttributes().get(0).getName());
    }

    @Test
    @DisplayName("a seller is reloaded without demanding an administrator and a first warehouse")
    void sellerIsReloadedWithoutIncorporationRules() {
        sellerRepositoryPort.save(DomainFixtures.seller("seller-7"));

        Seller reloaded = sellerRepositoryPort.findById(SellerId.of("seller-7")).orElseThrow();

        assertEquals("seller-7", reloaded.getIdentifier().getValue());
        assertEquals(application.domain.valuesObjects.UserRole.SELLER, reloaded.getRole());
    }

    @Test
    @DisplayName("a stock record keeps its balances and fabricates no movements")
    void stockKeepsBalancesWithoutFabricatingHistory() {
        InventoryItem stock = DomainFixtures.stockWith(7, 3, StockCondition.DAMAGED);
        inventoryItemRepositoryPort.save(stock);

        InventoryItem reloaded = inventoryItemRepositoryPort
                .findById(InventoryItemId.of("stock-1")).orElseThrow();

        assertEquals(7, reloaded.getAvailableQuantity().getValue());
        assertEquals(3, reloaded.getReservedQuantity().getValue());
        assertEquals(StockCondition.DAMAGED, reloaded.getCondition());
        assertTrue(reloaded.getMovements().isEmpty(),
                "replaying commands would have invented movements that never happened (RD-INV-04)");
    }

    @Test
    @DisplayName("the unique pair product-warehouse resolves the stock record (RD-ID-05)")
    void resolvesStockByProductAndWarehouse() {
        inventoryItemRepositoryPort.save(DomainFixtures.stockWith(5, 0, StockCondition.AVAILABLE));

        assertTrue(inventoryItemRepositoryPort.findByProductAndWarehouse(
                ProductId.of("product-1"),
                application.domain.valuesObjects.WarehouseId.of("warehouse-1")).isPresent());
    }
}
