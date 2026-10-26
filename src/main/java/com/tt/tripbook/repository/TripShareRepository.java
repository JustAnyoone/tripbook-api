package com.tt.tripbook.repository;

import com.tt.tripbook.model.TripShare;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripShareRepository extends JpaRepository<TripShare, Long> {

    // All shares for trips owned by this user (shares they created)
    List<TripShare> findByOwnerId(Long ownerId);

    // All trips shared with this user (trips from other accounts they can see)
    List<TripShare> findByGuestId(Long guestId);

    // All shares for a specific trip
    List<TripShare> findByTripId(Long tripId);

    Optional<TripShare> findByTripIdAndGuestId(Long tripId, Long guestId);

    boolean existsByTripIdAndGuestId(Long tripId, Long guestId);

    void deleteByTripIdAndGuestId(Long tripId, Long guestId);
}
