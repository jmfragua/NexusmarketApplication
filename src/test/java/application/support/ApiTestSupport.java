package application.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import application.domain.ports.out.UserRepositoryPort;
import application.infrastructure.seed.DatabaseSeeder;
import application.infrastructure.security.CredentialService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Real application context over H2, driven through HTTP with the real security chain. Each test
 * runs in a transaction that is rolled back, so every test starts from an empty platform.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(InMemoryAuditTestConfig.class)
@Transactional
public abstract class ApiTestSupport {

    protected static final ObjectMapper JSON = new ObjectMapper();

    protected static final String ADMIN_EMAIL = "admin-api@nexusmarket.test";

    protected static final String ADMIN_PASSWORD = "admin-api-secret";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    protected UserRepositoryPort userRepositoryPort;

    @Autowired
    protected CredentialService credentialService;

    protected MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    protected String adminToken() throws Exception {
        new DatabaseSeeder(userRepositoryPort, credentialService, "admin-api", ADMIN_EMAIL, ADMIN_PASSWORD,
                "DOC-ADMIN-API").run();
        return login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    protected String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JSON.readTree(body).get("token").asText();
    }

    /** Self registers a buyer whose email and document derive from {@code key}. */
    protected String buyerToken(String key) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"buyerId":"b-%1$s","fullName":"Comprador %1$s","email":"%1$s@buyers.test",
                                 "identityDocument":"DOC-B-%1$s","password":"buyer-secret",
                                 "primaryAddress":"Calle 1 #2-3"}""".formatted(key)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JSON.readTree(body).get("token").asText();
    }

    /** Incorporates seller {@code s-<key>} with first warehouse {@code wh-<key>} and logs them in. */
    protected String sellerToken(String adminToken, String key) throws Exception {
        mockMvc.perform(authorized(post("/api/v1/admin/sellers"), adminToken)
                        .content("""
                                {"sellerId":"s-%1$s","fullName":"Tienda %1$s","email":"%1$s@sellers.test",
                                 "identityDocument":"DOC-S-%1$s","password":"seller-secret",
                                 "firstWarehouseId":"wh-%1$s"}""".formatted(key)))
                .andExpect(status().isCreated());
        return login(key + "@sellers.test", "seller-secret");
    }

    protected MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }
}
