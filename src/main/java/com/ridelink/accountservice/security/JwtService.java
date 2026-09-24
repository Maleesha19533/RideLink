package com.ridelink.accountservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;


@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(
                secretKey.getBytes(StandardCharsets.UTF_8)
        );
    }

    // Generate JWT Token
    public String generateToken(String email, String role) {

        long expirationTime = 1000 * 60 * 60; // 1 hour

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(
                        new Date(System.currentTimeMillis() + expirationTime)
                )
                .signWith(getSigningKey())
                .compact();
    }

    // Read claims from JWT
    private Claims extractClaims(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Extract email from JWT
    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }
    // Extract role from JWT
    public String extractRole(String token) {
        return extractClaims(token)
                .get("role", String.class);
    }

    // Validate JWT
    public boolean isTokenValid(String token) {

        try {

            Date expiration =
                    extractClaims(token).getExpiration();

            return expiration.after(new Date());

        } catch (Exception e) {

            return false;
        }
    }
}