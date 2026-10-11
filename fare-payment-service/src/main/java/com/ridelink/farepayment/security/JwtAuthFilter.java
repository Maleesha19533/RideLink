package com.ridelink.farepayment.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Verifies the bearer token's signature and expiry using the shared secret, then trusts its
 * "sub" (user id) and "role" claims for authorization in this service.
 *
 * Unlike Account Service's equivalent filter, this service does NOT re-check the account's
 * live status against a database on every request - it doesn't own the users table and
 * querying Account Service's DB directly would violate the assignment's data-ownership rule.
 * This is a documented trade-off: a token stays valid here for its full lifetime even if the
 * account is suspended in Account Service after the token was issued. Mitigating this (e.g.
 * short token lifetimes, or an API gateway that checks account status centrally) is a design
 * point worth covering in the report's security section.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (jwtService.isValid(token)) {
                Claims claims = jwtService.parseClaims(token);
                String userId = claims.getSubject();
                String role = claims.get("role", String.class);

                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            // An invalid/expired token simply leaves the SecurityContext unauthenticated -
            // the request falls through as anonymous and Spring Security returns 401/403.
        }

        filterChain.doFilter(request, response);
    }
}
