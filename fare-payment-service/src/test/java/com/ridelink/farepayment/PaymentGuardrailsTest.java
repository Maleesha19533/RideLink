package com.ridelink.farepayment;

import com.ridelink.farepayment.dto.response.PaymentResponse;
import com.ridelink.farepayment.entity.FareRecord;
import com.ridelink.farepayment.entity.PaymentRecord;
import com.ridelink.farepayment.exception.ApiException;
import com.ridelink.farepayment.service.FareService;
import com.ridelink.farepayment.service.PaymentService;
import com.ridelink.farepayment.service.ReceiptService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers three bugs found before Ride Management Service existed to test against:
 *   1. Payment could previously be taken against an ESTIMATED fare, not just a FINAL one.
 *   2. Nothing stopped a ride being paid, or receipted, more than once.
 *   3. getFareByRideId() crashed with IncorrectResultSizeDataAccessException once a ride
 *      had more than one FareRecord row (an ESTIMATED one followed by a FINAL one).
 *      getPaymentByRideId() was changed the same way as a defensive measure, even though
 *      a failed processPayment() call currently isn't persisted at all.
 */
@SpringBootTest
class PaymentGuardrailsTest {

    @Autowired
    private FareService fareService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ReceiptService receiptService;

    @Test
    void processPayment_rejectsPaymentAgainstAnEstimatedFare() {
        fareService.estimateFare("RIDE-GUARD-1", new BigDecimal("10"));
        FareRecord estimate = fareService.getFareByRideId("RIDE-GUARD-1");

        ApiException exception = assertThrows(ApiException.class,
                () -> paymentService.processPayment("RIDE-GUARD-1", estimate.getId(), new BigDecimal("900"), "CARD"));

        assertTrue(exception.getMessage().contains("FINAL fare"));
    }

    @Test
    void getFareByRideId_prefersFinalRecordWhenBothEstimateAndFinalExist() {
        fareService.estimateFare("RIDE-GUARD-2", new BigDecimal("5"));
        fareService.calculateFinalFare("RIDE-GUARD-2", new BigDecimal("10"));

        // Before the fix, this line threw IncorrectResultSizeDataAccessException because
        // two FareRecord rows exist for the same rideId.
        FareRecord result = fareService.getFareByRideId("RIDE-GUARD-2");

        assertEquals("FINAL", result.getFareType());
        // BigDecimal.equals() is scale-sensitive ("10" != "10.00" even though numerically
        // equal), and this column round-trips through the DB with 2 decimal places -
        // compareTo() is the correct comparison for BigDecimal value equality.
        assertEquals(0, new BigDecimal("10").compareTo(result.getDistanceKm()));
    }

    @Test
    void processPayment_rejectsASecondSuccessfulPaymentForTheSameRide() {
        fareService.calculateFinalFare("RIDE-GUARD-3", new BigDecimal("10"));
        FareRecord fareRecord = fareService.getFareByRideId("RIDE-GUARD-3");

        PaymentResponse first = paymentService.processPayment(
                "RIDE-GUARD-3", fareRecord.getId(), new BigDecimal("900"), "CARD");
        assertEquals("SUCCESS", first.getStatus());

        ApiException exception = assertThrows(ApiException.class,
                () -> paymentService.processPayment("RIDE-GUARD-3", fareRecord.getId(), new BigDecimal("900"), "CARD"));

        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    void getPaymentByRideId_returnsTheSuccessfulPayment() {
        fareService.calculateFinalFare("RIDE-GUARD-4", new BigDecimal("10"));
        FareRecord fareRecord = fareService.getFareByRideId("RIDE-GUARD-4");

        PaymentResponse success = paymentService.processPayment(
                "RIDE-GUARD-4", fareRecord.getId(), new BigDecimal("900"), "CARD");

        assertEquals("SUCCESS", paymentService.getPaymentByRideId("RIDE-GUARD-4").getStatus());
        assertEquals(success.getPaymentId(), paymentService.getPaymentByRideId("RIDE-GUARD-4").getPaymentId());
    }

    @Test
    void generateReceipt_rejectsASecondReceiptForTheSameRide() {
        fareService.calculateFinalFare("RIDE-GUARD-5", new BigDecimal("10"));
        FareRecord fareRecord = fareService.getFareByRideId("RIDE-GUARD-5");
        PaymentResponse payment = paymentService.processPayment(
                "RIDE-GUARD-5", fareRecord.getId(), new BigDecimal("900"), "CARD");

        receiptService.generateReceipt("RIDE-GUARD-5", payment.getPaymentId());

        ApiException exception = assertThrows(ApiException.class,
                () -> receiptService.generateReceipt("RIDE-GUARD-5", payment.getPaymentId()));

        assertTrue(exception.getMessage().contains("already been generated"));
    }

    @Test
    void processPayment_recordsAFailedPaymentWhenTheAmountIsWrong_andAllowsACorrectRetry() {
        fareService.calculateFinalFare("RIDE-GUARD-6", new BigDecimal("10"));
        FareRecord fareRecord = fareService.getFareByRideId("RIDE-GUARD-6");

        assertThrows(ApiException.class,
                () -> paymentService.processPayment("RIDE-GUARD-6", fareRecord.getId(), new BigDecimal("500"), "CARD"));

        // The failed attempt is kept on file (this is the "failed simulated payment" scenario).
        PaymentRecord failed = paymentService.getPaymentByRideId("RIDE-GUARD-6");
        assertEquals("FAILED", failed.getStatus());
        assertEquals("Amount does not match final fare", failed.getFailureReason());

        // A FAILED attempt must not block a correct payment afterwards.
        PaymentResponse retry = paymentService.processPayment(
                "RIDE-GUARD-6", fareRecord.getId(), new BigDecimal("900"), "CARD");
        assertEquals("SUCCESS", retry.getStatus());
        assertEquals("SUCCESS", paymentService.getPaymentByRideId("RIDE-GUARD-6").getStatus());
    }
}
