package application.adapters.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import application.domain.ports.out.UserRepositoryPort;
import application.domain.support.InMemoryOperationLogRepository;
import application.domain.valuesObjects.OperationType;
import application.infrastructure.seed.DatabaseSeeder;
import application.infrastructure.security.CredentialService;
import application.support.InMemoryAuditTestConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Walks the whole business cycle over HTTP, from an empty platform to a paid order.
 *
 * <p>This is the test that was missing when four blocking defects reached a running system: every
 * unit test built its own aggregates, so none of them noticed that a registered buyer had no cart,
 * or that no administrator could ever be created. Here nothing is pre-built — each step has to be
 * reachable through the API using only what the previous step produced.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(InMemoryAuditTestConfig.class)
@Transactional
class PurchaseFlowEndToEndTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepositoryPort userRepositoryPort;

    @Autowired
    private CredentialService credentialService;

    @Autowired
    private InMemoryOperationLogRepository operationLogs;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
                        .springSecurity())
                .build();
        operationLogs.clear();
    }

    @Test
    @DisplayName("an empty platform reaches a paid order through the API alone")
    void fullPurchaseCycle() throws Exception {
        // 0. Bootstrap: without this there is no administrator, and nothing else can be created.
        new DatabaseSeeder(userRepositoryPort, credentialService,
                "admin-e2e", "admin-e2e@nexusmarket.test", "admin-secret", "DOC-ADMIN-E2E").run();
        String adminToken = login("admin-e2e@nexusmarket.test", "admin-secret");

        // 1. The administrator incorporates a seller together with their first warehouse.
        mockMvc.perform(post("/api/v1/admin/sellers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sellerId":"s-e2e","fullName":"Carlos","email":"carlos-e2e@nexusmarket.test",
                                 "identityDocument":"DOC-S-E2E","password":"seller-secret",
                                 "firstWarehouseId":"wh-e2e"}"""))
                .andExpect(status().isCreated());
        String sellerToken = login("carlos-e2e@nexusmarket.test", "seller-secret");

        // 2. The seller registers a product. It is published, but still invisible: no variants yet.
        mockMvc.perform(post("/api/v1/seller/products")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"p-e2e","productType":"PHYSICAL"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.visibleInCatalog").value(false));

        // 3. The first variant is what actually makes it sellable (RD-CAT-05).
        mockMvc.perform(post("/api/v1/seller/products/p-e2e/variants")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"variantId":"v-e2e","attributes":{"color":"rojo"}}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.visibleInCatalog").value(true));

        // 4. The public catalogue now exposes it, with no authentication at all, inside the page
        // envelope every listing answers with.
        mockMvc.perform(get("/api/v1/catalog/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("p-e2e"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));

        // 5. A buyer registers. The cart must come with them, or the flow dies right here.
        String buyerToken = registerBuyer();
        mockMvc.perform(get("/api/v1/buyer/me/cart").header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());

        // 6. Selection.
        mockMvc.perform(post("/api/v1/buyer/me/cart/items")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cartItemId":"ci-e2e","productId":"p-e2e",
                                 "productVariantId":"v-e2e","quantity":2}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        // 7. Confirmation: the first transition of the order cycle (RD-PED-01).
        mockMvc.perform(post("/api/v1/buyer/me/orders")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"o-e2e","deliveryAddress":"Calle 1 #2-3",
                                 "lines":[{"orderLineId":"ol-e2e","productId":"p-e2e",
                                           "productVariantId":"v-e2e","quantity":2,
                                           "lineAmount":50000,"currency":"COP"}]}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.totalAmount").value(50000));

        // 8. Payment.
        mockMvc.perform(post("/api/v1/buyer/me/orders/o-e2e/payment")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        // 9. The audited operations left their trail: the incorporation and the payment.
        assertEquals(1, operationLogs.findByOperationType(OperationType.SELLER_REGISTERED).size());
        assertEquals(1, operationLogs.findByOperationType(OperationType.ORDER_STATUS_CHANGED).size());
    }

    @Test
    @DisplayName("the catalogue is public but everything else demands a token")
    void publicSurfaceIsLimitedToTheCatalogue() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/products")).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/buyer/me/cart")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/sellers")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/supervisor/reports/refunds")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a role cannot reach the operations of another role (RG-03)")
    void rolesAreIsolated() throws Exception {
        String buyerToken = registerBuyer();

        mockMvc.perform(get("/api/v1/admin/sellers").header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/seller/products").header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a tampered token authenticates nobody")
    void tamperedTokenIsRejected() throws Exception {
        String token = registerBuyer();
        String tampered = token.substring(0, token.lastIndexOf('.')) + ".forged";

        mockMvc.perform(get("/api/v1/buyer/me/orders").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("business rules answer with their own status and name the rule that rejected")
    void businessRulesSurfaceAsHttpErrors() throws Exception {
        String buyerToken = registerBuyer();

        mockMvc.perform(post("/api/v1/buyer/me/orders/does-not-exist/payment")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.rule").value("EntityNotFoundException"));

        mockMvc.perform(post("/api/v1/buyer/me/cart/items")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cartItemId":"x","productId":"nope",
                                 "productVariantId":"v","quantity":1}"""))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/buyer/me/cart/items")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cartItemId":"x","productId":"p","productVariantId":"v","quantity":0}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.rule").value("RequestValidation"));
    }

    @Test
    @DisplayName("listings page themselves and reject impossible page parameters with 400")
    void listingsArePaged() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/products").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.page").value(0));

        mockMvc.perform(get("/api/v1/catalog/products").param("size", "0"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/catalog/products").param("page", "-1"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/catalog/products").param("size", "5000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("a duplicated email is rejected as a conflict (RD-ID-03)")
    void duplicatedEmailIsRejected() throws Exception {
        registerBuyer();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"buyerId":"other","fullName":"Otra","email":"ana-e2e@nexusmarket.test",
                                 "identityDocument":"DOC-OTHER","password":"x123456",
                                 "primaryAddress":"Otra calle"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rule").value("DuplicateEmailException"));
    }

    private String registerBuyer() throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"buyerId":"b-e2e","fullName":"Ana","email":"ana-e2e@nexusmarket.test",
                                 "identityDocument":"DOC-B-E2E","password":"buyer-secret",
                                 "primaryAddress":"Calle 1 #2-3"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return tokenOf(body);
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return tokenOf(body);
    }

    private String tokenOf(String body) throws Exception {
        JsonNode node = JSON.readTree(body);
        String token = node.get("token").asText();
        assertTrue(token != null && !token.isBlank(), "the response must carry a usable token");
        return token;
    }
}
