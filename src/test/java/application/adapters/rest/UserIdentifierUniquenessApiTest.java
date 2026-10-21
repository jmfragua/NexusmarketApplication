package application.adapters.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import application.domain.models.User;
import application.domain.valuesObjects.UserId;
import application.domain.valuesObjects.UserRole;
import application.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Identifiers are chosen by the client, so a registration that reuses one must be refused instead of
 * replacing the user that holds it (RD-ID-02). Before this rule was enforced, an anonymous visitor
 * could self register with the administrator's identifier and turn the administrator into a buyer.
 */
class UserIdentifierUniquenessApiTest extends ApiTestSupport {

    @Test
    @DisplayName("self registration with the administrator's identifier is refused and changes nothing")
    void buyerCannotTakeOverTheAdministrator() throws Exception {
        adminToken();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"buyerId":"admin-api","fullName":"Intruso","email":"intruso@x.test",
                                 "identityDocument":"DOC-INTRUSO","password":"intruso-pass",
                                 "primaryAddress":"Calle 9"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rule").value("DuplicateUserIdentifierException"));

        User<?> admin = userRepositoryPort.findByIdentifier(UserId.of("admin-api")).orElseThrow();
        assertEquals(UserRole.ADMINISTRATOR, admin.getRole());
        assertEquals(ADMIN_EMAIL, admin.getEmail().getValue());
        login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    @Test
    @DisplayName("a seller cannot be incorporated with the identifier of an existing user")
    void sellerCannotReuseAnIdentifier() throws Exception {
        String admin = adminToken();
        buyerToken("taken");

        mockMvc.perform(authorized(post("/api/v1/admin/sellers"), admin)
                        .content("""
                                {"sellerId":"b-taken","fullName":"Tienda","email":"tienda@x.test",
                                 "identityDocument":"DOC-TIENDA","password":"seller-secret",
                                 "firstWarehouseId":"wh-tienda"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rule").value("DuplicateUserIdentifierException"));

        assertEquals(UserRole.BUYER, userRepositoryPort.findByIdentifier(UserId.of("b-taken")).orElseThrow().getRole());
    }
}
