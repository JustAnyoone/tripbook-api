package com.tt.tripbook.service;

import com.tt.tripbook.dto.TripResponse;
import com.tt.tripbook.dto.TripShareResponse;
import com.tt.tripbook.exception.BusinessRuleException;
import com.tt.tripbook.exception.ConflictException;
import com.tt.tripbook.exception.ResourceNotFoundException;
import com.tt.tripbook.model.AppUser;
import com.tt.tripbook.model.Trip;
import com.tt.tripbook.model.TripShare;
import com.tt.tripbook.repository.TripRepository;
import com.tt.tripbook.repository.TripShareRepository;
import com.tt.tripbook.repository.UserRepository;
import com.tt.tripbook.security.CurrentUser;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripShareService {

    private final TripShareRepository shareRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    public TripShareService(TripShareRepository shareRepository,
                            TripRepository tripRepository,
                            UserRepository userRepository,
                            CurrentUser currentUser) {
        this.shareRepository = shareRepository;
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
    }

    // Share a trip owned by the current user with another user identified by email.
    @Transactional
    public TripShareResponse share(Long tripId, String guestEmail) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + tripId));

        if (!trip.getPassenger().getOwner().getId().equals(currentUser.id())) {
            throw new BusinessRuleException("You can only share your own trips");
        }

        AppUser guest = userRepository.findByEmail(guestEmail.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + guestEmail));

        if (guest.getId().equals(currentUser.id())) {
            throw new BusinessRuleException("You cannot share a trip with yourself");
        }

        if (shareRepository.existsByTripIdAndGuestId(tripId, guest.getId())) {
            throw new ConflictException("Trip is already shared with " + guestEmail);
        }

        AppUser owner = currentUser.get();

        TripShare share = new TripShare();
        share.setTrip(trip);
        share.setOwner(owner);
        share.setGuest(guest);
        share.setSharedAt(LocalDateTime.now());

        return TripShareResponse.from(shareRepository.save(share));
    }

    // Remove a share (unshare). Only the trip owner can remove it.
    @Transactional
    public void unshare(Long tripId, Long guestId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + tripId));

        if (!trip.getPassenger().getOwner().getId().equals(currentUser.id())) {
            throw new BusinessRuleException("You can only manage shares of your own trips");
        }

        if (!shareRepository.existsByTripIdAndGuestId(tripId, guestId)) {
            throw new ResourceNotFoundException("Share not found");
        }

        shareRepository.deleteByTripIdAndGuestId(tripId, guestId);
    }

    // List all shares for a trip (trip owner only).
    @Transactional(readOnly = true)
    public List<TripShareResponse> listSharesForTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + tripId));

        if (!trip.getPassenger().getOwner().getId().equals(currentUser.id())) {
            throw new BusinessRuleException("You can only list shares of your own trips");
        }

        return shareRepository.findByTripId(tripId).stream()
                .map(TripShareResponse::from)
                .toList();
    }

    // List all trips shared with the current user (from other accounts).
    @Transactional(readOnly = true)
    public List<TripResponse> listSharedWithMe() {
        return shareRepository.findByGuestId(currentUser.id()).stream()
                .map(share -> TripResponse.from(share.getTrip()))
                .toList();
    }
}
