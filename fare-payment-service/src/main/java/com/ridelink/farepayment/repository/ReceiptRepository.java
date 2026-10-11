package com.ridelink.farepayment.repository;

import com.ridelink.farepayment.entity.ReceiptRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReceiptRepository extends JpaRepository<ReceiptRecord, Long> {
    Optional<ReceiptRecord> findByReceiptId(String receiptId);
    Optional<ReceiptRecord> findByRideId(String rideId);
    Optional<ReceiptRecord> findByPaymentId(String paymentId);
    boolean existsByRideId(String rideId);
}
