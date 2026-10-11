package com.ridelink.farepayment.repository;

import com.ridelink.farepayment.entity.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentRecord, Long> {
    Optional<PaymentRecord> findByPaymentId(String paymentId);

    boolean existsByRideIdAndStatus(String rideId, String status);

    // A ride can end up with more than one PaymentRecord row (e.g. a failed attempt
    // followed by a successful one) even though existsByRideIdAndStatus above stops a
    // *second successful* payment. Prefer the SUCCESS row if one exists; otherwise fall
    // back to whichever attempt is most recent, so this never breaks on multiple rows.
    Optional<PaymentRecord> findFirstByRideIdAndStatusOrderByCreatedAtDesc(String rideId, String status);
    Optional<PaymentRecord> findFirstByRideIdOrderByCreatedAtDesc(String rideId);
}
