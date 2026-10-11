package com.ridelink.farepayment;

import com.ridelink.farepayment.dto.response.FareResponse;
import com.ridelink.farepayment.entity.FareRecord;
import com.ridelink.farepayment.repository.FareRepository;
import com.ridelink.farepayment.service.FareService;
import com.ridelink.farepayment.util.FareCalculator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A ride must never end up with two FINAL fares - payment locks a single fare row, so two FINAL
 * rows would let the same ride be paid twice. The "is there already a FINAL fare?" check in
 * FareService is only a fast path; these tests prove the DATABASE enforces the rule, that it does
 * not get in the way of the legitimate "many ESTIMATED rows per ride" case, and that simultaneous
 * requests all end up with the same single FINAL fare.
 *
 * Runs with the Ride Management integration switched off (see src/test/resources), so the
 * distance in the request is used.
 */
@SpringBootTest
class FinalFareUniquenessTest {

    @Autowired
    private FareService fareService;

    @Autowired
    private FareRepository fareRepository;

    private FareRecord fare(String rideId, String type) {
        return new FareRecord(rideId, new BigDecimal("10"), FareCalculator.BASE_FARE, FareCalculator.PER_KM_RATE,
                new BigDecimal("900.00"), new BigDecimal("900.00"), type, LocalDateTime.now());
    }

    private long countFinalRows(String rideId) {
        return fareRepository.findAll().stream()
                .filter(f -> rideId.equals(f.getRideId()) && FareRecord.TYPE_FINAL.equals(f.getFareType()))
                .count();
    }

    @Test
    void database_rejectsASecondFinalFareForTheSameRide() {
        fareRepository.saveAndFlush(fare("RIDE-UNIQ-1", FareRecord.TYPE_FINAL));

        assertThrows(DataIntegrityViolationException.class,
                () -> fareRepository.saveAndFlush(fare("RIDE-UNIQ-1", FareRecord.TYPE_FINAL)));

        assertEquals(1, countFinalRows("RIDE-UNIQ-1"));
    }

    @Test
    void database_stillAllowsManyEstimatesForTheSameRide() {
        fareService.estimateFare("RIDE-UNIQ-2", new BigDecimal("3"));
        fareService.estimateFare("RIDE-UNIQ-2", new BigDecimal("5"));
        fareService.estimateFare("RIDE-UNIQ-2", new BigDecimal("8"));

        long estimates = fareRepository.findAll().stream()
                .filter(f -> "RIDE-UNIQ-2".equals(f.getRideId())
                        && FareRecord.TYPE_ESTIMATED.equals(f.getFareType()))
                .count();
        assertEquals(3, estimates);
    }

    @Test
    void estimateAndFinalCanCoexist_andEstimateRowsNeverCarryTheFinalMarker() {
        FareResponse estimate = fareService.estimateFare("RIDE-UNIQ-3", new BigDecimal("4"));
        FareResponse fin = fareService.calculateFinalFare("RIDE-UNIQ-3", new BigDecimal("10"));

        assertNotEquals(estimate.getFareId(), fin.getFareId());
        assertNull(fareRepository.findById(estimate.getFareId()).orElseThrow().getFinalRideId());
        assertEquals("RIDE-UNIQ-3", fareRepository.findById(fin.getFareId()).orElseThrow().getFinalRideId());
    }

    @Test
    void differentRidesEachGetTheirOwnFinalFare() {
        FareResponse a = fareService.calculateFinalFare("RIDE-UNIQ-4A", new BigDecimal("10"));
        FareResponse b = fareService.calculateFinalFare("RIDE-UNIQ-4B", new BigDecimal("10"));

        assertNotEquals(a.getFareId(), b.getFareId());
    }

    @Test
    void repeatedFinalFareCall_returnsTheSameFare() {
        FareResponse first = fareService.calculateFinalFare("RIDE-UNIQ-5", new BigDecimal("10"));
        FareResponse second = fareService.calculateFinalFare("RIDE-UNIQ-5", new BigDecimal("1"));

        assertEquals(first.getFareId(), second.getFareId());
        assertEquals(first.getFareAmount(), second.getFareAmount());
        assertEquals(1, countFinalRows("RIDE-UNIQ-5"));
    }

    @Test
    void simultaneousFinalFareRequests_produceExactlyOneFinalFare() throws Exception {
        final String rideId = "RIDE-UNIQ-RACE";
        final int threads = 8;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<FareResponse>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    return fareService.calculateFinalFare(rideId, new BigDecimal("10"));
                }));
            }

            ready.await(10, TimeUnit.SECONDS);
            go.countDown();

            Set<Long> fareIds = new HashSet<>();
            for (Future<FareResponse> future : futures) {
                // get() rethrows any exception a request threw - none of them may fail.
                fareIds.add(future.get(30, TimeUnit.SECONDS).getFareId());
            }

            assertEquals(1, fareIds.size(), "every request must be handed the same FINAL fare");
            assertEquals(1, countFinalRows(rideId), "only one FINAL row may exist for the ride");
        } finally {
            pool.shutdownNow();
        }
    }
}
