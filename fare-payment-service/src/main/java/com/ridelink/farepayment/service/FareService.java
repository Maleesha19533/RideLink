package com.ridelink.farepayment.service;

import com.ridelink.farepayment.client.RideManagementClient;
import com.ridelink.farepayment.client.dto.RideSummaryResponse;
import com.ridelink.farepayment.dto.response.FareResponse;
import com.ridelink.farepayment.entity.FareRecord;
import com.ridelink.farepayment.exception.ApiException;
import com.ridelink.farepayment.exception.ResourceNotFoundException;
import com.ridelink.farepayment.repository.FareRepository;
import com.ridelink.farepayment.util.FareCalculator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class FareService {

    private final FareRepository fareRepository;
    private final RideManagementClient rideManagementClient;

    public FareService(FareRepository fareRepository, RideManagementClient rideManagementClient) {
        this.fareRepository = fareRepository;
        this.rideManagementClient = rideManagementClient;
    }

    public FareResponse estimateFare(String rideId, BigDecimal distanceKm) {
        if (rideId == null || rideId.isBlank()) {
            throw new ApiException("Ride ID is required");
        }

        validateDistance(distanceKm);

        BigDecimal baseFare = FareCalculator.BASE_FARE;
        BigDecimal perKmRate = FareCalculator.PER_KM_RATE;
        BigDecimal calculatedFare = FareCalculator.calculateFare(distanceKm);

        FareRecord fareRecord = new FareRecord(
                rideId,
                distanceKm,
                baseFare,
                perKmRate,
                calculatedFare,
                calculatedFare,
                "ESTIMATED",
                LocalDateTime.now()
        );

        FareRecord savedFare = fareRepository.save(fareRecord);

        return new FareResponse(
                savedFare.getId(),
                rideId,
                distanceKm,
                baseFare,
                perKmRate,
                calculatedFare,
                "ESTIMATED"
        );
    }

    /**
     * Calculates (or returns the already-calculated) FINAL fare for a completed ride.
     *
     * When the Ride Management integration is on, the distance used is the one Ride Management
     * recorded for the ride - the distanceKm in the caller's request is IGNORED. Otherwise a
     * passenger could send a tiny distance for a long ride and pay far less than the real fare.
     * (With the integration switched off, for isolated testing, the request's distance is used.)
     *
     * A ride only ever gets one FINAL fare: calling this again returns the existing one, so
     * nobody can generate a second, cheaper FINAL fare for a ride that already has one. This holds
     * even for simultaneous requests - it is enforced by a database unique constraint, not just by
     * the "does one exist yet?" check.
     */
    public FareResponse calculateFinalFare(String rideId, BigDecimal requestedDistanceKm) {
        if (rideId == null || rideId.isBlank()) {
            throw new ApiException("Ride ID is required");
        }

        BigDecimal distanceKm;
        if (rideManagementClient.isEnabled()) {
            RideSummaryResponse ride = rideManagementClient.getRide(rideId);
            if (!ride.isCompleted()) {
                throw new ApiException(
                        "Final fare can only be calculated once the ride is completed (current status: "
                                + ride.status() + ")");
            }
            if (ride.distanceKm() == null) {
                throw new ApiException("Ride Management Service has no distance recorded for ride "
                        + rideId + " - cannot calculate the final fare");
            }
            distanceKm = ride.distanceKm();
        } else {
            distanceKm = requestedDistanceKm;
        }

        validateDistance(distanceKm);

        Optional<FareRecord> existingFinal =
                fareRepository.findFirstByRideIdAndFareTypeOrderByCreatedAtDesc(rideId, FareRecord.TYPE_FINAL);
        if (existingFinal.isPresent()) {
            return toResponse(existingFinal.get());
        }

        BigDecimal baseFare = FareCalculator.BASE_FARE;
        BigDecimal perKmRate = FareCalculator.PER_KM_RATE;
        BigDecimal finalFare = FareCalculator.calculateFare(distanceKm);

        FareRecord fareRecord = new FareRecord(
                rideId,
                distanceKm,
                baseFare,
                perKmRate,
                finalFare,
                finalFare,
                FareRecord.TYPE_FINAL,
                LocalDateTime.now()
        );

        // The check above is only a fast path: two simultaneous requests can both pass it. The real
        // guarantee is the unique constraint on FareRecord.finalRideId (see that field's comment).
        // The request that loses the race gets a constraint violation here; it then returns the
        // winner's FINAL fare, exactly as a later repeat call would, so callers never see a
        // duplicate FINAL fare or an error.
        //
        // NOTE: this method must NOT be @Transactional. The insert has to commit (or fail) inside
        // saveAndFlush's own transaction so the violation surfaces right here and the lookup below
        // runs in a fresh transaction that can see the winner's committed row.
        try {
            return toResponse(fareRepository.saveAndFlush(fareRecord));
        } catch (DataIntegrityViolationException raceLost) {
            return fareRepository.findFirstByRideIdAndFareTypeOrderByCreatedAtDesc(rideId, FareRecord.TYPE_FINAL)
                    .map(this::toResponse)
                    .orElseThrow(() -> raceLost);
        }
    }

    private FareResponse toResponse(FareRecord fare) {
        return new FareResponse(
                fare.getId(),
                fare.getRideId(),
                fare.getDistanceKm(),
                fare.getBaseFare(),
                fare.getPerKmRate(),
                fare.getFinalFare(),
                fare.getFareType()
        );
    }

    public FareRecord getFareById(Long fareId) {
        return fareRepository.findById(fareId)
                .orElseThrow(() -> new ResourceNotFoundException("Fare not found for id: " + fareId));
    }

    /** Same as getFareById but takes a database write lock - see FareRepository.findByIdForUpdate. */
    public FareRecord getFareByIdForUpdate(Long fareId) {
        return fareRepository.findByIdForUpdate(fareId)
                .orElseThrow(() -> new ResourceNotFoundException("Fare not found for id: " + fareId));
    }

    public FareRecord getFareByRideId(String rideId) {
        return fareRepository.findFirstByRideIdAndFareTypeOrderByCreatedAtDesc(rideId, FareRecord.TYPE_FINAL)
                .or(() -> fareRepository.findFirstByRideIdAndFareTypeOrderByCreatedAtDesc(
                        rideId, FareRecord.TYPE_ESTIMATED))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Fare not found for ride ID: " + rideId));
    }

    private void validateDistance(BigDecimal distanceKm) {
        if (distanceKm == null || distanceKm.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException("Distance must be a valid non-negative number");
        }
    }
}