package com.ridelink.farepayment.service;

import com.ridelink.farepayment.client.RideManagementClient;
import com.ridelink.farepayment.dto.response.PaymentResponse;
import com.ridelink.farepayment.entity.FareRecord;
import com.ridelink.farepayment.entity.PaymentRecord;
import com.ridelink.farepayment.exception.ApiException;
import com.ridelink.farepayment.exception.ResourceNotFoundException;
import com.ridelink.farepayment.repository.PaymentRepository;
import com.ridelink.farepayment.security.RideOwnershipGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final FareService fareService;
    private final RideManagementClient rideManagementClient;
    private final RideOwnershipGuard rideOwnershipGuard;

    public PaymentService(PaymentRepository paymentRepository, FareService fareService,
                           RideManagementClient rideManagementClient, RideOwnershipGuard rideOwnershipGuard) {
        this.paymentRepository = paymentRepository;
        this.fareService = fareService;
        this.rideManagementClient = rideManagementClient;
        this.rideOwnershipGuard = rideOwnershipGuard;
    }

    /**
     * Simulated payment. Runs in ONE database transaction and takes a write lock on the fare row,
     * so two simultaneous payment requests for the same fare are handled one after the other: the
     * second sees the first's SUCCESS record and is rejected, instead of both passing the "already
     * paid?" check at the same moment.
     *
     * noRollbackFor = ApiException: a rejected payment (e.g. wrong amount) throws ApiException, but
     * the FAILED payment record saved just before must still be committed so the failed attempt is
     * kept on file.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public PaymentResponse processPayment(String rideId, Long fareId, BigDecimal amount, String paymentMethod) {
        if (rideId == null || rideId.isBlank()) {
            throw new ApiException("Ride ID is required");
        }
        if (fareId == null) {
            throw new ApiException("Fare ID is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("Amount must be greater than zero");
        }
        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new ApiException("Payment method is required");
        }

        FareRecord fareRecord = fareService.getFareByIdForUpdate(fareId);

        if (!fareRecord.getRideId().equals(rideId)) {
            throw new ApiException("Fare does not belong to this ride");
        }

        // Ownership check before anything else touches ride/fare state, so a passenger
        // probing someone else's rideId doesn't learn its fare status along the way.
        rideOwnershipGuard.assertCallerOwnsRide(rideId);

        // An ESTIMATED FareRecord populates the same finalFare field as a FINAL one
        // (see FareService.estimateFare) - without this check, a passenger could pay
        // against a rough estimate before the ride is even completed.
        if (!"FINAL".equalsIgnoreCase(fareRecord.getFareType())) {
            throw new ApiException(
                    "Payment can only be made against a FINAL fare. This fare record for ride "
                            + rideId + " is still " + fareRecord.getFareType()
                            + " - call /api/fare/final for this ride first.");
        }

        // Prevent paying for the same ride twice (safe against races because of the fare lock).
        if (paymentRepository.existsByRideIdAndStatus(rideId, "SUCCESS")) {
            throw new ApiException("A successful payment already exists for ride " + rideId);
        }

        BigDecimal finalFare = fareRecord.getFinalFare();
        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LocalDateTime now = LocalDateTime.now();

        if (amount.compareTo(finalFare) != 0) {
            // Failed simulated payment: keep a record of the attempt, then reject it.
            paymentRepository.save(new PaymentRecord(
                    rideId,
                    paymentId,
                    fareId,
                    amount,
                    paymentMethod,
                    "FAILED",
                    "Amount does not match final fare",
                    now,
                    now
            ));
            throw new ApiException("Amount does not match final fare");
        }

        paymentRepository.save(new PaymentRecord(
                rideId,
                paymentId,
                fareId,
                amount,
                paymentMethod,
                "SUCCESS",
                null,
                now,
                now
        ));

        // Interservice interaction #2: tell Ride Management Service the ride is paid. Runs only
        // AFTER this transaction has committed (so we never announce a payment that then rolls
        // back, and the fare lock isn't held open during a network call). Best-effort - see
        // RideManagementClient.
        if (rideManagementClient.isEnabled()) {
            runAfterCommit(() -> rideManagementClient.notifyPaymentCompleted(rideId, paymentId, amount));
        }

        return new PaymentResponse(
                paymentId,
                rideId,
                amount,
                "SUCCESS",
                "Payment processed successfully",
                null
        );
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    /**
     * Plain lookup by payment ID, no ownership check - used internally (e.g. by
     * ReceiptService.generateReceipt(), which already runs its own ownership check on the
     * same rideId before calling this).
     */
    public PaymentRecord getPaymentById(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for id: " + paymentId));
    }

    /**
     * For the externally exposed GET /api/payments/{paymentId}/status endpoint: same lookup
     * as getPaymentById(), plus the ownership check so only the ride's own passenger (or an
     * admin) can see its payment status.
     */
    public PaymentRecord getPaymentStatus(String paymentId) {
        PaymentRecord record = getPaymentById(paymentId);
        rideOwnershipGuard.assertCallerOwnsRide(record.getRideId());
        return record;
    }

    public PaymentRecord getPaymentByRideId(String rideId) {
        return paymentRepository.findFirstByRideIdAndStatusOrderByCreatedAtDesc(rideId, "SUCCESS")
                .or(() -> paymentRepository.findFirstByRideIdOrderByCreatedAtDesc(rideId))
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for ride ID: " + rideId));
    }
}
