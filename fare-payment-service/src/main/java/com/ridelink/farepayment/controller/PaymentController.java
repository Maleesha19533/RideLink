package com.ridelink.farepayment.controller;

import com.ridelink.farepayment.dto.request.ProcessPaymentRequest;
import com.ridelink.farepayment.dto.response.PaymentResponse;
import com.ridelink.farepayment.dto.response.PaymentStatusResponse;
import com.ridelink.farepayment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payment API", description = "Simulated payment processing and payment status")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/process")
    @Operation(summary = "Process a payment", description = "Simulates payment processing using a deterministic rule.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or amount mismatch"),
            @ApiResponse(responseCode = "404", description = "Fare not found")
    })
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody ProcessPaymentRequest request) {
        PaymentResponse response = paymentService.processPayment(
                request.getRideId(),
                request.getFareId(),
                request.getAmount(),
                request.getPaymentMethod());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{paymentId}/status")
    @Operation(summary = "Check payment status", description = "Returns the payment status for a payment ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment status retrieved"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(@PathVariable String paymentId) {
        return ResponseEntity.ok(PaymentStatusResponse.from(paymentService.getPaymentStatus(paymentId)));
    }
}
