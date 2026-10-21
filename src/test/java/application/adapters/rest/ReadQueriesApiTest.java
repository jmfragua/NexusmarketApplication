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
 * The read-only queries each role was missing. Every one of them reads only what its role manages:
 * a buyer its own profile and orders, a seller its own products, logistics and the administrator the
 * operation they run.
 */
class ReadQueriesApiTest extends ApiTestSupport {

    private String admin;

    private String seller;

    private String buyer;

    private String operator;

    @BeforeEach
    void setUp() throws Exception {
        admin = adminToken();
        seller = sellerToken(admin, "q");
        buyer = buyerToken("q");
        mockMvc.perform(authorized(post("/api/v1/admin/staff"), admin)
                        .content("""
                                {"userId":"ops-q","fullName":"Operador","email":"ops-q@staff.test",
                                 "identityDocument":"DOC-OPS-Q","password":"staff-secret",
                                 "role":"LOGISTICS_OPERATOR"}"""))
                .andExpect(status().isCreated());
        operator = login("ops-q@staff.test", "staff-secret");
    }

    /** Seller publishes p-q, the buyer orders one unit and pays: order o-q ends PAID. */
    private void paidOrder() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/seller/products"), seller)
                .content("{\"productId\":\"p-q\",\"productType\":\"PHYSICAL\"}")).andExpect(status().isCreated());
        mockMvc.perform(authorized(post("/api/v1/seller/products/p-q/variants"), seller)
                .content("{\"variantId\":\"v-q\",\"attributes\":{\"name\":\"Termo\",\"price\":\"68500\"}}"))
                .andExpect(status().isCreated());
        mockMvc.perform(authorized(post("/api/v1/buyer/me/cart/items"), buyer)
                .content("{\"cartItemId\":\"ci-q\",\"productId\":\"p-q\",\"productVariantId\":\"v-q\",\"quantity\":1}"))
                .andExpect(status().isCreated());
        mockMvc.perform(authorized(post("/api/v1/buyer/me/orders"), buyer)
                .content("""
                        {"orderId":"o-q","deliveryAddress":"Calle 1 #2-3","lines":[{"orderLineId":"ol-q",
                         "productId":"p-q","productVariantId":"v-q","quantity":1,"lineAmount":68500,
                         "currency":"COP"}]}""")).andExpect(status().isCreated());
        mockMvc.perform(authorized(post("/api/v1/buyer/me/orders/o-q/payment"), buyer)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /buyer/me: the buyer's own profile with every address")
    void buyerProfile() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/buyer/me/addresses"), buyer).content("{\"address\":\"Carrera 7 #40-12\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(authorized(get("/api/v1/buyer/me"), buyer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("b-q"))
                .andExpect(jsonPath("$.primaryAddress").value("Calle 1 #2-3"))
                .andExpect(jsonPath("$.additionalAddresses[0]").value("Carrera 7 #40-12"))
                .andExpect(jsonPath("$.commercialStatus").value("ENABLED"));
        mockMvc.perform(authorized(get("/api/v1/buyer/me"), seller)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("shipments of an order: empty until packed, then visible to logistics and to its buyer only")
    void shipmentsOfAnOrder() throws Exception {
        paidOrder();
        mockMvc.perform(authorized(get("/api/v1/logistics/orders/o-q/shipments"), operator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(authorized(post("/api/v1/logistics/shipments/packaging"), operator)
                        .content("{\"shipmentId\":\"shp-q\",\"orderId\":\"o-q\",\"warehouseId\":\"wh-q\",\"deliveryAddress\":\"Calle 1 #2-3\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(authorized(get("/api/v1/logistics/orders/o-q/shipments"), operator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("shp-q"))
                .andExpect(jsonPath("$.content[0].warehouseId").value("wh-q"));
        mockMvc.perform(authorized(get("/api/v1/buyer/me/orders/o-q/shipments"), buyer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("shp-q"))
                .andExpect(jsonPath("$.content[0].deliveryAddress").value("Calle 1 #2-3"));

        String otherBuyer = buyerToken("q-other");
        mockMvc.perform(authorized(get("/api/v1/buyer/me/orders/o-q/shipments"), otherBuyer))
                .andExpect(status().isForbidden());
        mockMvc.perform(authorized(get("/api/v1/logistics/orders/missing/shipments"), operator))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /seller/products/{id}: own product in any status; 403 for another's, 404 if unknown")
    void sellerProduct() throws Exception {
        paidOrder();
        mockMvc.perform(authorized(get("/api/v1/seller/products/p-q"), seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("p-q"))
                .andExpect(jsonPath("$.variants[0].attributes.name").value("Termo"));

        String other = sellerToken(admin, "q-other");
        mockMvc.perform(authorized(get("/api/v1/seller/products/p-q"), other))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.rule").value("ProductNotOwnedException"));
        mockMvc.perform(authorized(get("/api/v1/seller/products/none"), seller)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /admin/users and /admin/warehouses: paged listings for the administrator only")
    void adminListings() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/admin/warehouses"), admin).content("{\"warehouseId\":\"wh-market\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(authorized(get("/api/v1/admin/users").param("size", "200"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content[?(@.id == 'ops-q')].role").value("LOGISTICS_OPERATOR"))
                .andExpect(jsonPath("$.content[?(@.id == 'b-q')].role").value("BUYER"));
        mockMvc.perform(authorized(get("/api/v1/admin/users").param("size", "1"), admin))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalPages").value(4));

        mockMvc.perform(authorized(get("/api/v1/admin/warehouses"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.id == 'wh-market')].warehouseType").value("MARKETPLACE"))
                .andExpect(jsonPath("$.content[?(@.id == 'wh-q')].sellerId").value("s-q"));

        mockMvc.perform(authorized(get("/api/v1/admin/users"), buyer)).andExpect(status().isForbidden());
        mockMvc.perform(authorized(get("/api/v1/admin/warehouses"), operator)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /admin/orders/{id}/invoice: 404 until invoiced, then the invoice a refund needs")
    void invoiceOfOrder() throws Exception {
        paidOrder();
        mockMvc.perform(authorized(get("/api/v1/admin/orders/o-q/invoice"), admin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.rule").value("InvoiceNotIssuedException"));

        mockMvc.perform(authorized(post("/api/v1/admin/invoices"), admin).content("{\"invoiceId\":\"inv-q\",\"orderId\":\"o-q\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(authorized(get("/api/v1/admin/orders/o-q/invoice"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("inv-q"))
                .andExpect(jsonPath("$.totalAmount").value(68500));
        mockMvc.perform(authorized(get("/api/v1/admin/orders/missing/invoice"), admin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.rule").value("EntityNotFoundException"));
    }
}
