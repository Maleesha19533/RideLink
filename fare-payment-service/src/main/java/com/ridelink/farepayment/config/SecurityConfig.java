package com.ridelink.farepayment.config;

import com.ridelink.farepayment.security.JsonAccessDeniedHandler;
import com.ridelink.farepayment.security.JsonAuthenticationEntryPoint;
import com.ridelink.farepayment.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Authorization model for Fare & Payment Service:
 * - Fare estimate (/api/fare/estimate) is callable by any authenticated RideLink user - a
 *   fare estimate isn't sensitive per-passenger information. The FINAL fare
 *   (/api/fare/final) is passenger/admin only and additionally requires the caller to be that
 *   ride's passenger (FareController calls RideOwnershipGuard).
 * - Every payment and receipt endpoint (process/status/generate/get) additionally requires
 *   the caller to actually be that ride's passenger, via RideOwnershipGuard called from
 *   PaymentService/ReceiptService - admins bypass that check. Initiating a payment or
 *   generating a receipt is further restricted at the URL level below to PASSENGER/ADMIN
 *   roles only (drivers can't even attempt those two).
 *
 * Tokens are issued exclusively by Account Service and verified here locally (see
 * JwtAuthFilter) - this service never performs a login of its own.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint;
    private final JsonAccessDeniedHandler jsonAccessDeniedHandler;

    public SecurityConfig(
            JwtAuthFilter jwtAuthFilter,
            JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint,
            JsonAccessDeniedHandler jsonAccessDeniedHandler
    ) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.jsonAuthenticationEntryPoint = jsonAuthenticationEntryPoint;
        this.jsonAccessDeniedHandler = jsonAccessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jsonAuthenticationEntryPoint)
                        .accessDeniedHandler(jsonAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/fare/final").hasAnyRole("PASSENGER", "ADMIN")
                        .requestMatchers("/api/payments/process").hasAnyRole("PASSENGER", "ADMIN")
                        .requestMatchers("/api/receipts/generate").hasAnyRole("PASSENGER", "ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
