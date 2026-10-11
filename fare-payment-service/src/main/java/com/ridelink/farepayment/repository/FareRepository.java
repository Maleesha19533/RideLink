package com.ridelink.farepayment.repository;

import com.ridelink.farepayment.entity.FareRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FareRepository extends JpaRepository<FareRecord, Long> {
    Optional<FareRecord> findFirstByRideIdAndFareTypeOrderByCreatedAtDesc(String rideId, String fareType);

    /**
     * Loads a fare row with a database write lock (SELECT ... FOR UPDATE). Used by payment
     * processing so two simultaneous payment requests for the same fare are handled one after
     * the other - the second one then sees the first one's SUCCESS record and is rejected,
     * instead of both passing the "already paid?" check at the same moment.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FareRecord f where f.id = :id")
    Optional<FareRecord> findByIdForUpdate(@Param("id") Long id);
}
