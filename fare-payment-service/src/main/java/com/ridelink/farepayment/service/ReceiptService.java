package com.ridelink.farepayment.service;

import com.ridelink.farepayment.dto.response.ReceiptResponse;
import com.ridelink.farepayment.entity.FareRecord;
import com.ridelink.farepayment.entity.PaymentRecord;
import com.ridelink.farepayment.entity.ReceiptRecord;
import com.ridelink.farepayment.exception.ApiException;
import com.ridelink.farepayment.exception.ResourceNotFoundException;
import com.ridelink.farepayment.repository.ReceiptRepository;
import com.ridelink.farepayment.security.RideOwnershipGuard;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final PaymentService paymentService;
    private final FareService fareService;
    private final RideOwnershipGuard rideOwnershipGuard;

    public ReceiptService(ReceiptRepository receiptRepository, PaymentService paymentService,
                           FareService fareService, RideOwnershipGuard rideOwnershipGuard) {
        this.receiptRepository = receiptRepository;
        this.paymentService = paymentService;
        this.fareService = fareService;
        this.rideOwnershipGuard = rideOwnershipGuard;
    }

    public ReceiptResponse generateReceipt(String rideId, String paymentId) {
        if (rideId == null || rideId.isBlank()) {
            throw new ApiException("Ride ID is required");
        }
        if (paymentId == null || paymentId.isBlank()) {
            throw new ApiException("Payment ID is required");
        }

        rideOwnershipGuard.assertCallerOwnsRide(rideId);

        PaymentRecord paymentRecord = paymentService.getPaymentById(paymentId);
        if (!paymentRecord.getRideId().equals(rideId)) {
            throw new ApiException("Payment does not belong to this ride");
        }

        if (!"SUCCESS".equals(paymentRecord.getStatus())) {
            throw new ApiException("Receipt can only be generated for successful payments");
        }

        // Fix: prevent generating more than one receipt for the same ride.
        if (receiptRepository.existsByRideId(rideId)) {
            throw new ApiException("A receipt has already been generated for ride " + rideId);
        }

        FareRecord fareRecord = fareService.getFareByRideId(rideId);
        String receiptId = "RCPT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LocalDateTime now = LocalDateTime.now();

        ReceiptRecord receiptRecord = new ReceiptRecord(
                receiptId,
                rideId,
                paymentId,
                paymentRecord.getAmount(),
                fareRecord.getFinalFare(),
                now,
                "ISSUED"
        );

        receiptRepository.save(receiptRecord);

        return new ReceiptResponse(
                receiptId,
                rideId,
                paymentId,
                paymentRecord.getAmount(),
                fareRecord.getFinalFare(),
                now,
                "ISSUED"
        );
    }

    public ReceiptResponse getReceiptById(String receiptId) {
        ReceiptRecord record = receiptRepository.findByReceiptId(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found for id: " + receiptId));

        rideOwnershipGuard.assertCallerOwnsRide(record.getRideId());

        return new ReceiptResponse(
                record.getReceiptId(),
                record.getRideId(),
                record.getPaymentId(),
                record.getAmount(),
                record.getFinalFare(),
                record.getIssuedAt(),
                record.getStatus()
        );
    }

    public ReceiptResponse getReceiptByRideId(String rideId) {
        rideOwnershipGuard.assertCallerOwnsRide(rideId);

        ReceiptRecord record = receiptRepository.findByRideId(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found for ride ID: " + rideId));

        return new ReceiptResponse(
                record.getReceiptId(),
                record.getRideId(),
                record.getPaymentId(),
                record.getAmount(),
                record.getFinalFare(),
                record.getIssuedAt(),
                record.getStatus()
        );
    }
}
