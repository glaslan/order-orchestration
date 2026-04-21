package com.ftf.order;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String SECRET = "test-secret-key";
    private static final Key KEY = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    private String validToken() {
        return Jwts.builder()
                .claim("id", "cust-1")
                .claim("name", "Alice")
                .claim("email", "alice@example.com")
                .claim("role", "USER")
                .signWith(KEY)
                .compact();
    }

    // --- Public endpoints ---

    @Test
    void rootIsPublic() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    void productsIsPublic() throws Exception {
        mockMvc.perform(get("/products")).andExpect(status().isOk());
    }

    @Test
    void inventorySyncIsPublic() throws Exception {
        mockMvc.perform(post("/api/orders/sync")).andExpect(status().isOk());
    }

    // --- Protected endpoints without token → 401 ---

    @Test
    void getCartRequiresAuth() throws Exception {
        mockMvc.perform(get("/getCart")).andExpect(status().isUnauthorized());
    }

    @Test
    void addToCartRequiresAuth() throws Exception {
        mockMvc.perform(post("/addToCart")
                .param("itemId", "1")
                .param("quantity", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void checkoutRequiresAuth() throws Exception {
        mockMvc.perform(post("/checkout")).andExpect(status().isUnauthorized());
    }

    @Test
    void ordersEndpointRequiresAuth() throws Exception {
        mockMvc.perform(post("/orders")
                .contentType("application/json")
                .content("{\"subscriptionId\":\"sub_1\"}"))
                .andExpect(status().isUnauthorized());
    }

    // --- Protected endpoints with valid token → not 401 ---

    @Test
    void getCartWithValidTokenIsNotUnauthorized() throws Exception {
        mockMvc.perform(get("/getCart")
                .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void addToCartWithValidTokenIsNotUnauthorized() throws Exception {
        mockMvc.perform(post("/addToCart")
                .header("Authorization", "Bearer " + validToken())
                .param("itemId", "1")
                .param("quantity", "1"))
                // 400 because item won't exist in test DB, but NOT 401
                .andExpect(status().is4xxClientError())
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status != 401 : "Expected auth to pass but got 401";
                });
    }
}
