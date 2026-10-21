package application.adapters.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import application.domain.ports.out.InventoryItemRepositoryPort;
import application.domain.models.InventoryItem;
import application.domain.valuesObjects.InventoryItemId;
import application.domain.valuesObjects.StockCondition;
import application.support.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A seller operates only on the stock of their own products in their own warehouses (RD-CAT-01,
 * RG-03). Seller A owns record {@code inv-a}; seller B owns {@code inv-b}.
 */
class SellerStockOwnershipApiTest extends ApiTestSupport {

    @Autowired
    private InventoryItemRepositoryPort inventoryItems;

    private String admin;

    private String sellerA;

    private String sellerB;

    @BeforeEach
    void setUp() throws Exception {
        admin = adminToken();
        sellerA = sellerToken(admin, "a");
        sellerB = sellerToken(admin, "b");
        openRecordWithStock(sellerA, "a");
        openRecordWithStock(sellerB, "b");
    }

    private void openRecordWithStock(String token, String key) throws Exception {
        mockMvc.perform(authorized(post("/api/v1/seller/products"), token)
                .content("{\"productId\":\"p-%s\",\"productType\":\"PHYSICAL\"}".formatted(key)))
                .andExpect(status().isCreated());
        mockMvc.perform(authorized(post("/api/v1/seller/stock/items"), token)
                .content("{\"inventoryItemId\":\"inv-%1$s\",\"productId\":\"p-%1$s\",\"warehouseId\":\"wh-%1$s\"}".formatted(key)))
                .andExpect(status().isCreated());
        mockMvc.perform(authorized(post("/api/v1/seller/stock/receipts"), token)
                .content("{\"inventoryItemId\":\"inv-%1$s\",\"movementId\":\"mov-%1$s-0\",\"quantity\":10}".formatted(key)))
                .andExpect(status().isCreated());
    }

    private void assertRecordOfAIntact() {
        InventoryItem record = inventoryItems.findById(InventoryItemId.of("inv-a")).orElseThrow();
        assertEquals(10, record.getAvailableQuantity().getValue());
        assertEquals(StockCondition.AVAILABLE, record.getCondition());
    }

    @Nested
    @DisplayName("seller B on the stock of seller A: 403 and nothing changes")
    class ForeignStock {

        @Test
        @DisplayName("receipt")
        void receipt() throws Exception {
            mockMvc.perform(authorized(post("/api/v1/seller/stock/receipts"), sellerB)
                            .content("{\"inventoryItemId\":\"inv-a\",\"movementId\":\"mov-x\",\"quantity\":5}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.rule").value("ProductNotOwnedException"));
            assertRecordOfAIntact();
        }

        @Test
        @DisplayName("adjustment")
        void adjustment() throws Exception {
            mockMvc.perform(authorized(post("/api/v1/seller/stock/adjustments"), sellerB)
                            .content("{\"inventoryItemId\":\"inv-a\",\"movementId\":\"mov-x\",\"quantity\":1}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.rule").value("ProductNotOwnedException"));
            assertRecordOfAIntact();
        }

        @Test
        @DisplayName("damage")
        void damage() throws Exception {
            mockMvc.perform(authorized(post("/api/v1/seller/stock/inv-a/damage"), sellerB))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.rule").value("ProductNotOwnedException"));
            assertRecordOfAIntact();
        }

        @Test
        @DisplayName("stock of a product of seller A")
        void stockOfForeignProduct() throws Exception {
            mockMvc.perform(authorized(get("/api/v1/seller/products/p-a/stock"), sellerB))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.rule").value("ProductNotOwnedException"));
        }
    }

    @Test
    @DisplayName("the owner keeps operating on their own stock")
    void ownerOperates() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/seller/stock/receipts"), sellerA)
                        .content("{\"inventoryItemId\":\"inv-a\",\"movementId\":\"mov-a-1\",\"quantity\":5}"))
                .andExpect(status().isCreated());
        mockMvc.perform(authorized(post("/api/v1/seller/stock/adjustments"), sellerA)
                        .content("{\"inventoryItemId\":\"inv-a\",\"movementId\":\"mov-a-2\",\"quantity\":12}"))
                .andExpect(status().isCreated());
        mockMvc.perform(authorized(get("/api/v1/seller/products/p-a/stock"), sellerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].availableQuantity").value(12));
        mockMvc.perform(authorized(post("/api/v1/seller/stock/inv-a/damage"), sellerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.condition").value("DAMAGED"));
    }

    @Test
    @DisplayName("404 for a stock record or a product that does not exist")
    void missingRecord() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/seller/stock/receipts"), sellerA)
                        .content("{\"inventoryItemId\":\"inv-none\",\"movementId\":\"mov-x\",\"quantity\":5}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(authorized(post("/api/v1/seller/stock/inv-none/damage"), sellerA))
                .andExpect(status().isNotFound());
        mockMvc.perform(authorized(get("/api/v1/seller/products/p-none/stock"), sellerA))
                .andExpect(status().isNotFound());
    }

    @Nested
    @DisplayName("logistics and supervision keep operating on every warehouse, and only they do")
    class OperationRoles {

        @Test
        @DisplayName("a logistics operator receives on the stock of any seller")
        void logisticsUnchanged() throws Exception {
            String operator = staff("ops-own", "LOGISTICS_OPERATOR");
            mockMvc.perform(authorized(post("/api/v1/logistics/stock/receipts"), operator)
                            .content("{\"inventoryItemId\":\"inv-a\",\"movementId\":\"mov-l\",\"quantity\":3}"))
                    .andExpect(status().isCreated());
            mockMvc.perform(authorized(get("/api/v1/logistics/warehouses/wh-b/stock"), operator))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value("inv-b"));
        }

        @Test
        @DisplayName("a supervisor reads the stock of any warehouse")
        void supervisorUnchanged() throws Exception {
            String supervisor = staff("sup-own", "SUPERVISOR");
            mockMvc.perform(authorized(get("/api/v1/supervisor/reports/warehouses/wh-a/stock"), supervisor))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value("inv-a"));
        }

        @Test
        @DisplayName("sellers and buyers cannot use the logistics or supervision stock routes")
        void otherRolesRejected() throws Exception {
            String buyer = buyerToken("stock-curious");
            mockMvc.perform(authorized(post("/api/v1/logistics/stock/receipts"), sellerB)
                            .content("{\"inventoryItemId\":\"inv-a\",\"movementId\":\"mov-x\",\"quantity\":3}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(authorized(get("/api/v1/logistics/warehouses/wh-a/stock"), sellerB))
                    .andExpect(status().isForbidden());
            mockMvc.perform(authorized(get("/api/v1/supervisor/reports/warehouses/wh-a/stock"), sellerB))
                    .andExpect(status().isForbidden());
            mockMvc.perform(authorized(get("/api/v1/supervisor/reports/damaged-stock"), buyer))
                    .andExpect(status().isForbidden());
            assertRecordOfAIntact();
        }

        private String staff(String id, String role) throws Exception {
            mockMvc.perform(authorized(post("/api/v1/admin/staff"), admin)
                            .content("""
                                    {"userId":"%1$s","fullName":"Staff %1$s","email":"%1$s@staff.test",
                                     "identityDocument":"DOC-%1$s","password":"staff-secret","role":"%2$s"}"""
                                    .formatted(id, role)))
                    .andExpect(status().isCreated());
            return login(id + "@staff.test", "staff-secret");
        }
    }
}
