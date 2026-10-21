package application.infrastructure.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import application.domain.models.User;
import application.domain.valuesObjects.EmailAddress;
import application.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * The security chain answers with the same {@code {status, rule, message}} body as the domain: 401
 * when the request carries no valid authentication, 403 when the authenticated role does not reach
 * the route.
 */
class SecurityErrorResponsesTest extends ApiTestSupport {

    @Autowired
    private JwtProperties jwtProperties;

    @Nested
    @DisplayName("401 when there is no valid authentication")
    class Unauthenticated {

        @Test
        @DisplayName("without a token")
        void missingToken() throws Exception {
            mockMvc.perform(get("/api/v1/buyer/me/cart"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.rule").value("AuthenticationRequired"))
                    .andExpect(jsonPath("$.message").isNotEmpty());
        }

        @Test
        @DisplayName("with a malformed token")
        void malformedToken() throws Exception {
            mockMvc.perform(get("/api/v1/admin/sellers").header("Authorization", "Bearer not.a.jwt"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.rule").value("AuthenticationRequired"));
        }

        @Test
        @DisplayName("with an expired token")
        void expiredToken() throws Exception {
            buyerToken("expired");
            User<?> buyer = userRepositoryPort.findByEmail(EmailAddress.of("expired@buyers.test")).orElseThrow();
            String expired = new JwtProvider(new JwtProperties(jwtProperties.secret(), -1)).generateToken(buyer);

            mockMvc.perform(get("/api/v1/buyer/me/cart").header("Authorization", "Bearer " + expired))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.rule").value("AuthenticationRequired"));
        }

        @Test
        @DisplayName("with a valid token of a user blocked after it was issued")
        void blockedUser() throws Exception {
            String admin = adminToken();
            String buyer = buyerToken("blocked");
            mockMvc.perform(authorized(post("/api/v1/admin/users/b-blocked/block"), admin)).andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/buyer/me/cart").header("Authorization", "Bearer " + buyer))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("403 when the authenticated role does not reach the route")
    class WrongRole {

        @Test
        @DisplayName("a buyer on an administrator route")
        void buyerOnAdminRoute() throws Exception {
            String buyer = buyerToken("intruder");

            mockMvc.perform(get("/api/v1/admin/sellers").header("Authorization", "Bearer " + buyer))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.rule").value("AccessDenied"));
        }

        @Test
        @DisplayName("an administrator on a buyer route")
        void adminOnBuyerRoute() throws Exception {
            String admin = adminToken();

            mockMvc.perform(get("/api/v1/buyer/me/orders").header("Authorization", "Bearer " + admin))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.rule").value("AccessDenied"));
        }
    }

    @Nested
    @DisplayName("public routes are unaffected")
    class PublicRoutes {

        @Test
        @DisplayName("the catalogue and the API docs need no token")
        void catalogueAndDocsStayOpen() throws Exception {
            mockMvc.perform(get("/api/v1/catalog/products")).andExpect(status().isOk());
            mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("a failed login is still the domain rejection, not an authentication error")
        void failedLoginKeepsItsRule() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"nobody@nexusmarket.test\",\"password\":\"wrong-password\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.rule").value("UnauthorizedOperationException"));
        }
    }
}
