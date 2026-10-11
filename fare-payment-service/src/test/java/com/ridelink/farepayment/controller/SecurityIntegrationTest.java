package com.ridelink.farepayment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.farepayment.dto.request.EstimateFareRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves the JWT filter + role rules in SecurityConfig actually work end-to-end, using
 * hand-built tokens signed with the same secret configured in
 * src/test/resources/application.properties (this service never issues tokens itself, so
 * there is no production JwtService.generateToken() to reuse here).
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    private static final String TEST_SECRET = "test-secret-key-at-least-32-bytes-long-1234";
    private static final SecretKey SIGNING_KEY = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String tokenFor(String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 60_000))
                .signWith(SIGNING_KEY)
                .compact();
    }

    @Test
    void estimateFareRejectsRequestWithoutToken() throws Exception {
        var request = new EstimateFareRequest("RIDE-SEC-1", new BigDecimal("5"));

        mockMvc.perform(post("/api/fare/estimate")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void estimateFareSucceedsForAnyAuthenticatedRole() throws Exception {
        var request = new EstimateFareRequest("RIDE-SEC-2", new BigDecimal("5"));

        mockMvc.perform(post("/api/fare/estimate")
                        .header("Authorization", "Bearer " + tokenFor("DRIVER"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rideId").value("RIDE-SEC-2"));
    }

    @Test
    void processPaymentRejectsDriverRole() throws Exception {
        String body = "{\"rideId\":\"RIDE-SEC-3\",\"fareId\":1,\"amount\":500,\"paymentMethod\":\"CARD\"}";

        mockMvc.perform(post("/api/payments/process")
                        .header("Authorization", "Bearer " + tokenFor("DRIVER"))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void processPaymentRejectsExpiredToken() throws Exception {
        Date past = new Date(System.currentTimeMillis() - 120_000);
        String expiredToken = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", "PASSENGER")
                .issuedAt(new Date(past.getTime() - 60_000))
                .expiration(past)
                .signWith(SIGNING_KEY)
                .compact();

        String body = "{\"rideId\":\"RIDE-SEC-4\",\"fareId\":1,\"amount\":500,\"paymentMethod\":\"CARD\"}";

        mockMvc.perform(post("/api/payments/process")
                        .header("Authorization", "Bearer " + expiredToken)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isUnauthorized());
    }
}
