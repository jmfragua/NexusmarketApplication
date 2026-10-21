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
 * {@code POST /api/v1/admin/staff}: the only way the logistics and supervision roles come into
 * existence.
 */
class StaffApiTest extends ApiTestSupport {

    private String admin;

    @BeforeEach
    void setUp() throws Exception {
        admin = adminToken();
    }

    private String staff(String id, String email, String document, String role) {
        return """
                {"userId":"%s","fullName":"Personal %s","email":"%s","identityDocument":"%s",
                 "password":"staff-secret","role":"%s"}""".formatted(id, id, email, document, role);
    }

    @Test
    @DisplayName("201: a logistics operator is created, logs in and reaches only its own panel")
    void logisticsOperatorCreated() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/admin/staff"), admin)
                        .content(staff("ops-1", "ops-1@staff.test", "DOC-OPS-1", "LOGISTICS_OPERATOR")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("ops-1"))
                .andExpect(jsonPath("$.role").value("LOGISTICS_OPERATOR"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist());

        String operator = login("ops-1@staff.test", "staff-secret");
        mockMvc.perform(authorized(get("/api/v1/logistics/warehouses"), operator)).andExpect(status().isOk());
        mockMvc.perform(authorized(get("/api/v1/supervisor/reports/refunds"), operator)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("201: a supervisor is created and reads the reports")
    void supervisorCreated() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/admin/staff"), admin)
                        .content(staff("sup-1", "sup-1@staff.test", "DOC-SUP-1", "SUPERVISOR")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("SUPERVISOR"));

        String supervisor = login("sup-1@staff.test", "staff-secret");
        mockMvc.perform(authorized(get("/api/v1/supervisor/reports/refunds"), supervisor)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("409: a repeated email, document or identifier")
    void duplicates() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/admin/staff"), admin)
                        .content(staff("ops-1", "ops-1@staff.test", "DOC-OPS-1", "LOGISTICS_OPERATOR")))
                .andExpect(status().isCreated());

        mockMvc.perform(authorized(post("/api/v1/admin/staff"), admin)
                        .content(staff("ops-2", "ops-1@staff.test", "DOC-OPS-2", "LOGISTICS_OPERATOR")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rule").value("DuplicateEmailException"));
        mockMvc.perform(authorized(post("/api/v1/admin/staff"), admin)
                        .content(staff("ops-3", "ops-3@staff.test", "DOC-OPS-1", "LOGISTICS_OPERATOR")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rule").value("DuplicateIdentityDocumentException"));
        mockMvc.perform(authorized(post("/api/v1/admin/staff"), admin)
                        .content(staff("admin-api", "ops-4@staff.test", "DOC-OPS-4", "SUPERVISOR")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rule").value("DuplicateUserIdentifierException"));
    }

    @Test
    @DisplayName("400: any role other than logistics operator or supervisor")
    void otherRolesRejected() throws Exception {
        for (String role : new String[] {"ADMINISTRATOR", "BUYER", "SELLER", "JEFE"}) {
            mockMvc.perform(authorized(post("/api/v1/admin/staff"), admin)
                            .content(staff("x-" + role, role + "@staff.test", "DOC-" + role, role)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.rule").value("RequestValidation"));
        }
    }

    @Test
    @DisplayName("403 for a buyer and for a seller; 401 without a token")
    void onlyAdministrators() throws Exception {
        String buyer = buyerToken("staff-curious");
        String seller = sellerToken(admin, "staff-curious");
        String body = staff("ops-9", "ops-9@staff.test", "DOC-OPS-9", "LOGISTICS_OPERATOR");

        mockMvc.perform(authorized(post("/api/v1/admin/staff"), buyer).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.rule").value("AccessDenied"));
        mockMvc.perform(authorized(post("/api/v1/admin/staff"), seller).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.rule").value("AccessDenied"));
        mockMvc.perform(post("/api/v1/admin/staff").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());
    }
}
