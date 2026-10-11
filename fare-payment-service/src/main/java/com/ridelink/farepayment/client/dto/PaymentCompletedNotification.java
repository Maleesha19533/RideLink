package com.ridelink.farepayment.client.dto;

import java.math.BigDecimal;

/**
 * Body sent to PATCH {ride-management-base-url}/api/internal/rides/{rideId}/payment-status.
 * Same "assumed contract" note as {@link RideSummaryResponse} applies here.
 */
public record PaymentCompletedNotification(
        String paymentId,
        BigDecimal amount,
        String status
) {
}
