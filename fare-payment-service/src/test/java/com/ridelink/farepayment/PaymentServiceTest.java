package com.ridelink.farepayment;

import com.ridelink.farepayment.dto.response.FareResponse;
import com.ridelink.farepayment.dto.response.PaymentResponse;
import com.ridelink.farepayment.dto.response.ReceiptResponse;
import com.ridelink.farepayment.entity.FareRecord;
import com.ridelink.farepayment.exception.ApiException;
import com.ridelink.farepayment.exception.ResourceNotFoundException;
import com.ridelink.farepayment.service.FareService;
import com.ridelink.farepayment.service.PaymentService;
import com.ridelink.farepayment.service.ReceiptService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private FareService fareService;

    @Autowired
    private ReceiptService receiptService;

    @Test
    void processPayment_shouldSucceedWhenAmountMatchesFinalFare() {
        FareResponse fareResponse = fareService.calculateFinalFare("RIDE-100", new BigDecimal("10"));
        FareRecord fareRecord = fareService.getFareByRideId("RIDE-100");

        PaymentResponse response = paymentService.processPayment("RIDE-100", fareRecord.getId(), new BigDecimal("900"), "CARD");

        assertEquals("SUCCESS", response.getStatus());
    }

    @Test
    void processPayment_shouldThrowWhenAmountDoesNotMatchFare() {
        FareResponse fareResponse = fareService.calculateFinalFare("RIDE-200", new BigDecimal("10"));
        FareRecord fareRecord = fareService.getFareByRideId("RIDE-200");

        ApiException exception = assertThrows(ApiException.class,
                () -> paymentService.processPayment("RIDE-200", fareRecord.getId(), new BigDecimal("500"), "CARD"));

        assertTrue(exception.getMessage().contains("Amount does not match final fare"));
    }

    @Test
    void processPayment_shouldThrowForMissingRideId() {
        ApiException exception = assertThrows(ApiException.class,
                () -> paymentService.processPayment("   ", 1L, new BigDecimal("500"), "CARD"));

        assertTrue(exception.getMessage().contains("Ride ID is required"));
    }

    @Test
    void processPayment_shouldThrowForMissingFareId() {
        ApiException exception = assertThrows(ApiException.class,
                () -> paymentService.processPayment("RIDE-101", null, new BigDecimal("500"), "CARD"));

        assertTrue(exception.getMessage().contains("Fare ID is required"));
    }

    @Test
    void processPayment_shouldThrowForInvalidAmount() {
        ApiException exception = assertThrows(ApiException.class,
                () -> paymentService.processPayment("RIDE-102", 1L, BigDecimal.ZERO, "CARD"));

        assertTrue(exception.getMessage().contains("Amount must be greater than zero"));
    }

    @Test
    void processPayment_shouldThrowForMissingPaymentMethod() {
        ApiException exception = assertThrows(ApiException.class,
                () -> paymentService.processPayment("RIDE-103", 1L, new BigDecimal("500"), "   "));

        assertTrue(exception.getMessage().contains("Payment method is required"));
    }

    @Test
    void processPayment_shouldThrowWhenFareNotFound() {
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> paymentService.processPayment("RIDE-104", 999999L, new BigDecimal("500"), "CARD"));

        assertTrue(exception.getMessage().contains("Fare not found for id"));
    }

    @Test
    void getPaymentById_shouldThrowWhenPaymentNotFound() {
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> paymentService.getPaymentById("PAY-NOT-FOUND"));

        assertTrue(exception.getMessage().contains("Payment not found for id"));
    }

    @Test
    void getPaymentByRideId_shouldThrowWhenPaymentNotFound() {
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> paymentService.getPaymentByRideId("RIDE-NOT-FOUND"));

        assertTrue(exception.getMessage().contains("Payment not found for ride ID"));
    }

    @Test
    void generateReceipt_shouldWorkForSuccessfulPayment() {
        fareService.calculateFinalFare("RIDE-300", new BigDecimal("10"));
        FareRecord fareRecord = fareService.getFareByRideId("RIDE-300");

        PaymentResponse paymentResponse = paymentService.processPayment("RIDE-300", fareRecord.getId(), new BigDecimal("900"), "CARD");
        ReceiptResponse receiptResponse = receiptService.generateReceipt("RIDE-300", paymentResponse.getPaymentId());

        assertEquals("ISSUED", receiptResponse.getStatus());
        assertEquals("RIDE-300", receiptResponse.getRideId());
    }

    @Test
    void generateReceipt_shouldThrowWhenRideIdMissing() {
        ApiException exception = assertThrows(ApiException.class,
                () -> receiptService.generateReceipt("   ", "PAY-123"));

        assertTrue(exception.getMessage().contains("Ride ID is required"));
    }

    @Test
    void getReceiptById_shouldThrowWhenReceiptNotFound() {
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> receiptService.getReceiptById("RCPT-NOT-FOUND"));

        assertTrue(exception.getMessage().contains("Receipt not found for id"));
    }

    @Test
    void getReceiptByRideId_shouldThrowWhenReceiptNotFound() {
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> receiptService.getReceiptByRideId("RIDE-NOT-FOUND"));

        assertTrue(exception.getMessage().contains("Receipt not found for ride ID"));
    }
}
