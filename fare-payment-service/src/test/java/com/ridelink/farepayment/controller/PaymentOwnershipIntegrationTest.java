package com.ridelink.farepayment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.farepayment.client.RideManagementClient;
import com.ridelink.farepayment.client.dto.RideSummaryResponse;
import com.ridelink.farepayment.dto.request.GenerateReceiptRequest;
import com.ridelink.farepayment.dto.request.ProcessPaymentRequest;
import com.ridelink.farepayment.service.FareService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves RideOwnershipGuard actually stops one passenger from acting on, or reading, a ride
 * that belongs to a different passenger - across payment creation, payment status, receipt
 * generation, and both receipt-lookup endpoints. Ride Management Service is mocked to return
 * a fixed passengerId for the ride; the JWT's "sub" claim is what RideOwnershipGuard compares
 * it against.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "ridelink.services.ride-management.enabled=true")
class PaymentOwnershipIntegrationTest {

    private static final String TEST_SECRET = "test-secret-key-at-least-32-bytes-long-1234";
    private static final SecretKey SIGNING_KEY = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
    private static final String ACTUAL_PASSENGER_ID = "passenger-owner-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FareService fareService;

    @MockBean
    private RideManagementClient rideManagementClient;

    private String tokenFor(String subject, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 60_000))
                .signWith(SIGNING_KEY)
                .compact();
    }

    private Long seedFinalFare(String rideId) {
        when(rideManagementClient.isEnabled()).thenReturn(true);
        when(rideManagementClient.getRide(rideId)).thenReturn(
                new RideSummaryResponse(rideId, ACTUAL_PASSENGER_ID, "driver-1", "COMPLETED", new BigDecimal("10")));

        fareService.calculateFinalFare(rideId, new BigDecimal("10"));
        return fareService.getFareByRideId(rideId).getId();
    }

    @Test
    void processPayment_rejectsAPassengerWhoIsNotTheRidesOwner() throws Exception {
        String rideId = "RIDE-OWNER-1";
        Long fareId = seedFinalFare(rideId);

        var request = new ProcessPaymentRequest(rideId, fareId, new BigDecimal("900"), "CARD");

        mockMvc.perform(post("/api/payments/process")
                        .header("Authorization", "Bearer " + tokenFor("someone-else", "PASSENGER"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not the passenger for ride " + rideId));
    }

    @Test
    void processPayment_allowsTheActualPassenger() throws Exception {
        String rideId = "RIDE-OWNER-2";
        Long fareId = seedFinalFare(rideId);

        var request = new ProcessPaymentRequest(rideId, fareId, new BigDecimal("900"), "CARD");

        mockMvc.perform(post("/api/payments/process")
                        .header("Authorization", "Bearer " + tokenFor(ACTUAL_PASSENGER_ID, "PASSENGER"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void processPayment_allowsAnAdminRegardlessOfOwnership() throws Exception {
        String rideId = "RIDE-OWNER-3";
        Long fareId = seedFinalFare(rideId);

        var request = new ProcessPaymentRequest(rideId, fareId, new BigDecimal("900"), "CARD");

        mockMvc.perform(post("/api/payments/process")
                        .header("Authorization", "Bearer " + tokenFor("some-admin-id", "ADMIN"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void generateReceipt_rejectsAPassengerWhoIsNotTheRidesOwner() throws Exception {
        String rideId = "RIDE-OWNER-4";
        Long fareId = seedFinalFare(rideId);

        var paymentRequest = new ProcessPaymentRequest(rideId, fareId, new BigDecimal("900"), "CARD");
        mockMvc.perform(post("/api/payments/process")
                        .header("Authorization", "Bearer " + tokenFor(ACTUAL_PASSENGER_ID, "PASSENGER"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk());

        var receiptRequest = new GenerateReceiptRequest(rideId, "does-not-matter-check-runs-first");

        mockMvc.perform(post("/api/receipts/generate")
                        .header("Authorization", "Bearer " + tokenFor("someone-else", "PASSENGER"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(receiptRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not the passenger for ride " + rideId));
    }

    @Test
    void getPaymentStatus_rejectsAPassengerWhoIsNotTheRidesOwner() throws Exception {
        String rideId = "RIDE-OWNER-5";
        Long fareId = seedFinalFare(rideId);

        var paymentRequest = new ProcessPaymentRequest(rideId, fareId, new BigDecimal("900"), "CARD");
        String paymentJson = mockMvc.perform(post("/api/payments/process")
                        .header("Authorization", "Bearer " + tokenFor(ACTUAL_PASSENGER_ID, "PASSENGER"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String paymentId = objectMapper.readTree(paymentJson).get("paymentId").asText();

        mockMvc.perform(get("/api/payments/{paymentId}/status", paymentId)
                        .header("Authorization", "Bearer " + tokenFor("someone-else", "PASSENGER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not the passenger for ride " + rideId));

        // The actual passenger can still read their own payment status.
        mockMvc.perform(get("/api/payments/{paymentId}/status", paymentId)
                        .header("Authorization", "Bearer " + tokenFor(ACTUAL_PASSENGER_ID, "PASSENGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId));
    }

    @Test
    void getReceiptById_and_getReceiptByRideId_rejectAPassengerWhoIsNotTheRidesOwner() throws Exception {
        String rideId = "RIDE-OWNER-6";
        Long fareId = seedFinalFare(rideId);

        String paymentJson = mockMvc.perform(post("/api/payments/process")
                        .header("Authorization", "Bearer " + tokenFor(ACTUAL_PASSENGER_ID, "PASSENGER"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new ProcessPaymentRequest(rideId, fareId, new BigDecimal("900"), "CARD"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String paymentId = objectMapper.readTree(paymentJson).get("paymentId").asText();

        String receiptJson = mockMvc.perform(post("/api/receipts/generate")
                        .header("Authorization", "Bearer " + tokenFor(ACTUAL_PASSENGER_ID, "PASSENGER"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new GenerateReceiptRequest(rideId, paymentId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String receiptId = objectMapper.readTree(receiptJson).get("receiptId").asText();

        mockMvc.perform(get("/api/receipts/{receiptId}", receiptId)
                        .header("Authorization", "Bearer " + tokenFor("someone-else", "PASSENGER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not the passenger for ride " + rideId));

        mockMvc.perform(get("/api/receipts/ride/{rideId}", rideId)
                        .header("Authorization", "Bearer " + tokenFor("someone-else", "PASSENGER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not the passenger for ride " + rideId));

        // The actual passenger can still read their own receipt both ways.
        mockMvc.perform(get("/api/receipts/{receiptId}", receiptId)
                        .header("Authorization", "Bearer " + tokenFor(ACTUAL_PASSENGER_ID, "PASSENGER")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/receipts/ride/{rideId}", rideId)
                        .header("Authorization", "Bearer " + tokenFor(ACTUAL_PASSENGER_ID, "PASSENGER")))
                .andExpect(status().isOk());
    }

    @Test
    void finalFare_rejectsAPassengerWhoIsNotTheRidesOwner() throws Exception {
        String rideId = "RIDE-OWNER-7";
        when(rideManagementClient.isEnabled()).thenReturn(true);
        when(rideManagementClient.getRide(rideId)).thenReturn(
                new RideSummaryResponse(rideId, ACTUAL_PASSENGER_ID, "driver-1", "COMPLETED", new BigDecimal("10")));

        mockMvc.perform(post("/api/fare/final")
                        .header("Authorization", "Bearer " + tokenFor("someone-else", "PASSENGER"))
                        .contentType("application/json")
                        .content("{\"rideId\":\"" + rideId + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not the passenger for ride " + rideId));
    }

    @Test
    void finalFare_usesRideManagementDistanceEvenIfThePassengerSendsASmallerOne() throws Exception {
        String rideId = "RIDE-OWNER-8";
        when(rideManagementClient.isEnabled()).thenReturn(true);
        when(rideManagementClient.getRide(rideId)).thenReturn(
                new RideSummaryResponse(rideId, ACTUAL_PASSENGER_ID, "driver-1", "COMPLETED", new BigDecimal("10")));

        mockMvc.perform(post("/api/fare/final")
                        .header("Authorization", "Bearer " + tokenFor(ACTUAL_PASSENGER_ID, "PASSENGER"))
                        .contentType("application/json")
                        .content("{\"rideId\":\"" + rideId + "\",\"distanceKm\":0.1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINAL"))
                .andExpect(jsonPath("$.fareAmount").value(900.0));
    }

    @Test
    void finalFare_isNotAvailableToADriver() throws Exception {
        mockMvc.perform(post("/api/fare/final")
                        .header("Authorization", "Bearer " + tokenFor("some-driver-id", "DRIVER"))
                        .contentType("application/json")
                        .content("{\"rideId\":\"RIDE-OWNER-10\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void successfulPayment_notifiesRideManagementExactlyOnce() throws Exception {
        String rideId = "RIDE-OWNER-9";
        Long fareId = seedFinalFare(rideId);

        mockMvc.perform(post("/api/payments/process")
                        .header("Authorization", "Bearer " + tokenFor(ACTUAL_PASSENGER_ID, "PASSENGER"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new ProcessPaymentRequest(rideId, fareId, new BigDecimal("900"), "CARD"))))
                .andExpect(status().isOk());

        verify(rideManagementClient, times(1)).notifyPaymentCompleted(eq(rideId), anyString(), any());
    }

    @Test
    void wrongAmountPayment_returns400_andDoesNotNotifyRideManagement() throws Exception {
        String rideId = "RIDE-OWNER-11";
        Long fareId = seedFinalFare(rideId);

        mockMvc.perform(post("/api/payments/process")
                        .header("Authorization", "Bearer " + tokenFor(ACTUAL_PASSENGER_ID, "PASSENGER"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new ProcessPaymentRequest(rideId, fareId, new BigDecimal("500"), "CARD"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Amount does not match final fare"));

        verify(rideManagementClient, times(0)).notifyPaymentCompleted(anyString(), anyString(), any());
    }

    @Test
    void malformedJsonBodyReturns400NotServerError() throws Exception {
        mockMvc.perform(post("/api/payments/process")
                        .header("Authorization", "Bearer " + tokenFor(ACTUAL_PASSENGER_ID, "PASSENGER"))
                        .contentType("application/json")
                        .content("{ this is not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
