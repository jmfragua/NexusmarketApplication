package application.adapters.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import application.support.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@code POST /api/v1/seller/stock/items}: the operation that makes every other stock operation
 * reachable, because it creates the record they act upon.
 */
class OpenStockRecordApiTest extends ApiTestSupport {

    private String admin;

    private String seller;

    @BeforeEach
    void setUp() throws Exception {
        admin = adminToken();
        seller = sellerToken(admin, "own");
        registerProduct(seller, "p-own", "PHYSICAL");
    }

    private void registerProduct(String token, String productId, String type) throws Exception {
        mockMvc.perform(authorized(post("/api/v1/seller/products"), token)
                        .content("{\"productId\":\"%s\",\"productType\":\"%s\"}".formatted(productId, type)))
                .andExpect(status().isCreated());
    }

    private String body(String itemId, String productId, String warehouseId) {
        return "{\"inventoryItemId\":\"%s\",\"productId\":\"%s\",\"warehouseId\":\"%s\"}"
                .formatted(itemId, productId, warehouseId);
    }

    @Test
    @DisplayName("201: opens an empty record, which then accepts a receipt")
    void opensRecordThenReceives() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/seller/stock/items"), seller).content(body("inv-1", "p-own", "wh-own")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("inv-1"))
                .andExpect(jsonPath("$.productId").value("p-own"))
                .andExpect(jsonPath("$.warehouseId").value("wh-own"))
                .andExpect(jsonPath("$.availableQuantity").value(0))
                .andExpect(jsonPath("$.reservedQuantity").value(0))
                .andExpect(jsonPath("$.condition").value("AVAILABLE"));

        mockMvc.perform(authorized(post("/api/v1/seller/stock/receipts"), seller)
                        .content("{\"inventoryItemId\":\"inv-1\",\"movementId\":\"mov-1\",\"quantity\":12}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.movementType").value("INBOUND"));

        mockMvc.perform(authorized(get("/api/v1/seller/products/p-own/stock"), seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].availableQuantity").value(12));
    }

    @Test
    @DisplayName("409: a second record for the same product and warehouse")
    void duplicatedPair() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/seller/stock/items"), seller).content(body("inv-1", "p-own", "wh-own")))
                .andExpect(status().isCreated());

        mockMvc.perform(authorized(post("/api/v1/seller/stock/items"), seller).content(body("inv-2", "p-own", "wh-own")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rule").value("DuplicateInventoryItemException"));
    }

    @Test
    @DisplayName("403: the product of another seller")
    void foreignProduct() throws Exception {
        String other = sellerToken(admin, "other");
        registerProduct(other, "p-other", "PHYSICAL");

        mockMvc.perform(authorized(post("/api/v1/seller/stock/items"), seller).content(body("inv-1", "p-other", "wh-own")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.rule").value("ProductNotOwnedException"));
    }

    @Test
    @DisplayName("403: a warehouse of another seller")
    void foreignWarehouse() throws Exception {
        sellerToken(admin, "other");

        mockMvc.perform(authorized(post("/api/v1/seller/stock/items"), seller).content(body("inv-1", "p-own", "wh-other")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.rule").value("WarehouseNotOwnedException"));
    }

    @Test
    @DisplayName("404: a product or a warehouse that does not exist")
    void missingReferences() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/seller/stock/items"), seller).content(body("inv-1", "p-none", "wh-own")))
                .andExpect(status().isNotFound());
        mockMvc.perform(authorized(post("/api/v1/seller/stock/items"), seller).content(body("inv-1", "p-own", "wh-none")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("409: a digital product holds no stock (RD-INV-05)")
    void digitalProduct() throws Exception {
        registerProduct(seller, "p-digital", "DIGITAL");

        mockMvc.perform(authorized(post("/api/v1/seller/stock/items"), seller).content(body("inv-1", "p-digital", "wh-own")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rule").value("StockRecordNotAllowedException"));
    }

    @Test
    @DisplayName("400: a request without the identifiers")
    void invalidRequest() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/seller/stock/items"), seller).content("{\"productId\":\"p-own\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.rule").value("RequestValidation"));
    }

    @Test
    @DisplayName("403 for a user without the SELLER role, 401 without a token")
    void onlySellers() throws Exception {
        String buyer = buyerToken("curious");

        mockMvc.perform(authorized(post("/api/v1/seller/stock/items"), buyer).content(body("inv-1", "p-own", "wh-own")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.rule").value("AccessDenied"));
        mockMvc.perform(authorized(post("/api/v1/seller/stock/items"), admin).content(body("inv-1", "p-own", "wh-own")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/seller/stock/items").contentType("application/json").content(body("inv-1", "p-own", "wh-own")))
                .andExpect(status().isUnauthorized());
    }
}
