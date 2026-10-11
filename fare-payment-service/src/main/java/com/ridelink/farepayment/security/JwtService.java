package com.ridelink.farepayment.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

/**
 * Verifies JWTs issued by RideLink Account Service. Fare & Payment Service never issues
 * tokens itself - only Account Service does that. Because all four RideLink services are
 * configured with the same {@code RIDELINK_JWT_SECRET}, this service can verify a caller's
 * identity and role locally, on every request, without an extra network call back to
 * Account Service for every authenticated request.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;

    public JwtService(@Value("${ridelink.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
