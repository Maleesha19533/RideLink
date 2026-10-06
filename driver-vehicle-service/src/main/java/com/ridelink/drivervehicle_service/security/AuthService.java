package com.ridelink.drivervehicle_service.security;

import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final JwtService jwtService;

    public AuthService(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public Claims validateDriver(String authorizationHeader) {

        if (authorizationHeader == null ||
                !authorizationHeader.startsWith("Bearer ")) {

            throw new SecurityException(
                    "Authorization token is required"
            );
        }

        String token = authorizationHeader.substring(7);

        if (!jwtService.isTokenValid(token)) {
            throw new SecurityException(
                    "Invalid or expired token"
            );
        }

        Claims claims = jwtService.extractClaims(token);

        String role = claims.get("role", String.class);

        if (!"DRIVER".equalsIgnoreCase(role) &&
                !"ADMIN".equalsIgnoreCase(role)) {

            throw new SecurityException(
                    "Driver or Admin access required"
            );
        }

        return claims;
    }
}