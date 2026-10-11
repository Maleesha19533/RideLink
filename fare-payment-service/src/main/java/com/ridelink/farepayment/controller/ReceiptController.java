package com.ridelink.farepayment.controller;

import com.ridelink.farepayment.dto.request.GenerateReceiptRequest;
import com.ridelink.farepayment.dto.response.ReceiptResponse;
import com.ridelink.farepayment.service.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/receipts")
@Tag(name = "Receipt API", description = "Receipt generation and retrieval")
public class ReceiptController {

    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate receipt", description = "Creates a receipt after a successful payment.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Receipt generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Payment or ride not found")
    })
    public ResponseEntity<ReceiptResponse> generateReceipt(@Valid @RequestBody GenerateReceiptRequest request) {
        ReceiptResponse response = receiptService.generateReceipt(request.getRideId(), request.getPaymentId());
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{receiptId}")
    @Operation(summary = "Get receipt by receipt ID", description = "Retrieves a receipt by its unique receipt ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Receipt returned successfully"),
            @ApiResponse(responseCode = "404", description = "Receipt not found")
    })
    public ResponseEntity<ReceiptResponse> getReceiptById(@PathVariable String receiptId) {
        ReceiptResponse response = receiptService.getReceiptById(receiptId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ride/{rideId}")
    @Operation(summary = "Get receipt by ride ID", description = "Retrieves the receipt associated with a specific ride.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Receipt returned successfully"),
            @ApiResponse(responseCode = "404", description = "Receipt not found")
    })
    public ResponseEntity<ReceiptResponse> getReceiptByRideId(@PathVariable String rideId) {
        ReceiptResponse response = receiptService.getReceiptByRideId(rideId);
        return ResponseEntity.ok(response);
    }
}
