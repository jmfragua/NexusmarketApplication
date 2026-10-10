package application.domain.services.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import application.domain.exceptions.ProductNotAvailableException;
import application.domain.exceptions.ProductNotOwnedException;
import application.domain.exceptions.UserNotActiveException;
import application.domain.models.PhysicalProduct;
import application.domain.models.Product;
import application.domain.models.Seller;
import application.domain.support.DomainFixtures;
import application.domain.support.ServiceHarness;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Exercises the catalogue services: ownership of the product (RD-CAT-01), the lifecycle of its
 * status (RD-CAT-04) and the double condition of visibility (RD-CAT-03 plus RD-CAT-05).
 */
class CatalogServicesTest {

    private ServiceHarness harness;

    private Seller seller;

    @BeforeEach
    void setUp() {
        harness = new ServiceHarness();
        seller = DomainFixtures.seller();
    }

    @Nested
    @DisplayName("Register product")
    class Register {

        @Test
        @DisplayName("stores a product of the seller, born published")
        void storesOwnProduct() {
            PhysicalProduct product = DomainFixtures.sellableProduct(seller);

            Product result = harness.registerProduct().register(seller, product);

            assertEquals(ProductStatus.PUBLISHED, result.getStatus());
            assertTrue(harness.products.findById(product.getIdentifier()).isPresent());
        }

        @Test
        @DisplayName("rejects a product registered under another seller (RD-CAT-01)")
        void rejectsForeignProduct() {
            PhysicalProduct foreign = DomainFixtures.sellableProduct(DomainFixtures.seller("seller-2"));

            assertThrows(ProductNotOwnedException.class,
                    () -> harness.registerProduct().register(seller, foreign));
        }

        /**
         * A buyer cannot even reach this service: {@code registerProduct(Seller, Product)} takes the
         * specialization, so the type system rules it out before any validation runs. What the
         * service does have to guard is a seller who is no longer allowed to operate (RG-01).
         */
        @Test
        @DisplayName("rejects a blocked seller before touching the catalogue")
        void rejectsBlockedSeller() {
            Seller blocked = DomainFixtures.seller("seller-blocked");
            blocked.block();
            PhysicalProduct product = DomainFixtures.sellableProduct(blocked);

            assertThrows(UserNotActiveException.class,
                    () -> harness.registerProduct().register(blocked, product));
            assertTrue(harness.products.findAll().isEmpty(), "nothing is stored when the actor is rejected");
        }
    }

    @Nested
    @DisplayName("Catalogue lifecycle")
    class Lifecycle {

        @Test
        @DisplayName("suspending withdraws the product from the public catalogue")
        void suspendWithdrawsFromCatalogue() {
            PhysicalProduct product = DomainFixtures.sellableProduct(seller);

            harness.suspendProduct().suspend(seller, product);

            assertEquals(ProductStatus.SUSPENDED, product.getStatus());
            assertFalse(product.isVisibleInCatalog());
        }

        @Test
        @DisplayName("a suspended product can be published again")
        void suspendedReturnsToPublished() {
            PhysicalProduct product = DomainFixtures.sellableProduct(seller);
            harness.suspendProduct().suspend(seller, product);

            harness.publishProduct().publish(seller, product);

            assertEquals(ProductStatus.PUBLISHED, product.getStatus());
        }

        @Test
        @DisplayName("a published product cannot be discontinued directly: it must be suspended first")
        void cannotSkipSuspension() {
            PhysicalProduct product = DomainFixtures.sellableProduct(seller);

            assertThrows(IllegalStateException.class,
                    () -> harness.discontinueProduct().discontinue(seller, product));
        }

        @Test
        @DisplayName("discontinued is terminal: no way back to published (RD-CAT-04)")
        void discontinuedIsTerminal() {
            PhysicalProduct product = DomainFixtures.sellableProduct(seller);
            harness.suspendProduct().suspend(seller, product);
            harness.discontinueProduct().discontinue(seller, product);

            assertEquals(ProductStatus.DISCONTINUED, product.getStatus());
            assertThrows(IllegalStateException.class, () -> harness.publishProduct().publish(seller, product));
        }

        @Test
        @DisplayName("a discontinued product admits no new variants")
        void discontinuedAdmitsNoVariants() {
            PhysicalProduct product = DomainFixtures.sellableProduct(seller);
            harness.suspendProduct().suspend(seller, product);
            harness.discontinueProduct().discontinue(seller, product);

            assertThrows(IllegalStateException.class, () -> harness.addProductVariant()
                    .addVariant(seller, product, DomainFixtures.variant(product.getIdentifier())));
        }
    }

    @Nested
    @DisplayName("Visibility in the public catalogue")
    class Visibility {

        @Test
        @DisplayName("a published product without variants is not visible (RD-CAT-05)")
        void publishedWithoutVariantsIsNotVisible() {
            PhysicalProduct bare = DomainFixtures.productWithoutVariants(seller);
            harness.products.save(bare);

            assertEquals(ProductStatus.PUBLISHED, bare.getStatus());
            assertFalse(bare.isVisibleInCatalog());
            assertTrue(harness.consultCatalog().consultPublicCatalog().isEmpty(),
                    "the service filters what the repository could not");
        }

        @Test
        @DisplayName("a published product with variants is visible")
        void publishedWithVariantsIsVisible() {
            harness.products.save(DomainFixtures.sellableProduct(seller));

            assertEquals(1, harness.consultCatalog().consultPublicCatalog().size());
        }

        @Test
        @DisplayName("consulting a non visible product by identifier is rejected")
        void rejectsNonVisibleProduct() {
            harness.products.save(DomainFixtures.productWithoutVariants(seller));

            assertThrows(ProductNotAvailableException.class,
                    () -> harness.consultCatalog().consultPublished(ProductId.of("product-2")));
        }

        @Test
        @DisplayName("the seller sees their own products whatever their status")
        void sellerSeesOwnProductsInAnyStatus() {
            PhysicalProduct product = DomainFixtures.sellableProduct(seller);
            harness.suspendProduct().suspend(seller, product);
            harness.products.save(product);

            assertEquals(1, harness.consultCatalog().consultOwnCatalog(seller).size());
        }
    }
}
