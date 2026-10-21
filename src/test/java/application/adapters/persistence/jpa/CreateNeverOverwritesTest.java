package application.adapters.persistence.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import application.adapters.persistence.jpa.repositories.SpringDataJpaInventoryItemRepository;
import application.adapters.persistence.jpa.repositories.SpringDataJpaProductRepository;
import application.adapters.rest.exception.GlobalExceptionHandler;
import application.domain.models.DigitalProduct;
import application.domain.models.InventoryItem;
import application.domain.models.PhysicalProduct;
import application.domain.models.Product;
import application.domain.ports.out.InventoryItemRepositoryPort;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.valuesObjects.InventoryItemId;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.SellerId;
import application.domain.valuesObjects.WarehouseId;
import application.support.InMemoryAuditTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The database is the last line behind the identifier checks of the domain: when two requests race
 * past those checks with the same identifier, {@code create} inserts, the primary or unique key
 * refuses the second row, and the first one is left exactly as it was (RD-ID-06).
 *
 * <p>Not transactional on purpose: each step commits on its own, as two concurrent requests would,
 * and the rows are removed afterwards.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(InMemoryAuditTestConfig.class)
class CreateNeverOverwritesTest {

    @Autowired private ProductRepositoryPort products;
    @Autowired private InventoryItemRepositoryPort inventoryItems;
    @Autowired private SpringDataJpaProductRepository productRows;
    @Autowired private SpringDataJpaInventoryItemRepository inventoryRows;
    @Autowired private TransactionTemplate transactions;

    @AfterEach
    void cleanUp() {
        transactions.executeWithoutResult(status -> {
            inventoryRows.deleteAll(inventoryRows.findAllById(java.util.List.of("race-inv-1", "race-inv-2")));
            productRows.deleteAll(productRows.findAllById(java.util.List.of("race-p")));
        });
    }

    @Test
    @DisplayName("a second creation with a taken id is refused by the primary key and the first survives")
    void primaryKeyRefusesTheSecondCreation() {
        products.create(new PhysicalProduct(ProductId.of("race-p"), SellerId.of("race-seller-a")));

        assertThrows(DataIntegrityViolationException.class,
                () -> products.create(new DigitalProduct(ProductId.of("race-p"), SellerId.of("race-seller-b"))));

        Product original = products.findById(ProductId.of("race-p")).orElseThrow();
        assertEquals(SellerId.of("race-seller-a"), original.getSellerId());
        assertEquals(true, original.requiresInventory(), "still the physical product of seller A");
    }

    @Test
    @DisplayName("a second stock record for the same product and warehouse is refused by the unique key")
    void uniqueKeyRefusesTheSecondStockRecord() {
        inventoryItems.create(new InventoryItem(InventoryItemId.of("race-inv-1"), ProductId.of("race-p"),
                WarehouseId.of("race-wh")));

        assertThrows(DataIntegrityViolationException.class, () -> inventoryItems.create(new InventoryItem(
                InventoryItemId.of("race-inv-2"), ProductId.of("race-p"), WarehouseId.of("race-wh"))));

        assertEquals(1, inventoryItems.findByProduct(ProductId.of("race-p")).size());
    }

    @Test
    @DisplayName("the refusal reaches the client as 409 with the error body, never as 500")
    void integrityViolationIsAConflict() {
        var response = new GlobalExceptionHandler()
                .handleDataIntegrityViolation(new DataIntegrityViolationException("duplicate key"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("UniqueConstraintViolation", response.getBody().rule());
        assertEquals(409, response.getBody().status());
    }
}
