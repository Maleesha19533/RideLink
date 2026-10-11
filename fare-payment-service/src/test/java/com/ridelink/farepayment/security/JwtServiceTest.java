package com.ridelink.farepayment.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    // Must match a valid HMAC secret length (32+ bytes) - mirrors Account Service's own test secret style
    private static final String SECRET = "test-secret-key-at-least-32-bytes-long-1234";
    private final SecretKey signingKey = Keys.hmacShaKeyFor(SECRET.getBytes());

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET);
    }

    private String buildToken(String subject, String role, long expiresInMillis) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiresInMillis))
                .signWith(signingKey)
                .compact();
    }

    @Test
    void validTokenSignedWithSameSecretIsAccepted() {
        String token = buildToken(UUID.randomUUID().toString(), "PASSENGER", 60_000);
        assertTrue(jwtService.isValid(token));
    }

    @Test
    void claimsExposeSubjectAndRole() {
        String userId = UUID.randomUUID().toString();
        String token = buildToken(userId, "DRIVER", 60_000);

        Claims claims = jwtService.parseClaims(token);

        assertEquals(userId, claims.getSubject());
        assertEquals("DRIVER", claims.get("role", String.class));
    }

    @Test
    void expiredTokenIsRejected() {
        String token = buildToken(UUID.randomUUID().toString(), "PASSENGER", -1_000);
        assertFalse(jwtService.isValid(token));
    }

    @Test
    void tokenSignedWithDifferentSecretIsRejected() {
        SecretKey otherKey = Keys.hmacShaKeyFor("a-completely-different-secret-key-32-bytes+".getBytes());
        String token = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", "PASSENGER")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(otherKey)
                .compact();

        assertFalse(jwtService.isValid(token));
    }

    @Test
    void garbageTokenIsRejected() {
        assertFalse(jwtService.isValid("not-a-real-token"));
    }
}
