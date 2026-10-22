package com.tt.tripbook.service;

import com.tt.tripbook.dto.DashboardResponse;
import com.tt.tripbook.dto.DashboardResponse.CurrencyTotal;
import com.tt.tripbook.dto.DashboardResponse.StatusCount;
import com.tt.tripbook.dto.DashboardResponse.TopDestination;
import com.tt.tripbook.dto.TripResponse;
import com.tt.tripbook.model.TripStatus;
import com.tt.tripbook.repository.TripRepository;
import com.tt.tripbook.security.CurrentUser;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Numbers for the first screen. Everything is computed by the database (group by queries), so the
// cost does not grow with the number of trips the way loading them all would.
@Service
public class DashboardService {

    private static final int LIST_SIZE = 5;

    private final TripRepository trips;
    private final CurrentUser currentUser;
    private final Clock clock;

    public DashboardService(TripRepository trips, CurrentUser currentUser, Clock clock) {
        this.trips = trips;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardResponse summary() {
        Long ownerId = currentUser.id();
        LocalDate today = LocalDate.now(clock);

        List<StatusCount> statusCounts = trips.countByStatus(ownerId).stream()
                .map(row -> new StatusCount(row.status().name(), row.trips()))
                .toList();
        List<CurrencyTotal> totals = trips.totalsByCurrency(ownerId, TripStatus.CANCELLED).stream()
                .map(row -> new CurrencyTotal(row.currency().name(), row.trips(), row.total()))
                .toList();
        List<TripResponse> upcoming = trips
                .findUpcoming(ownerId, TripStatus.CANCELLED, today,
                        PageRequest.of(0, LIST_SIZE, Sort.by("departureDate").ascending()))
                .stream().map(TripResponse::from).toList();
        List<TopDestination> top = trips.topDestinations(ownerId, TripStatus.CANCELLED, PageRequest.of(0, LIST_SIZE))
                .stream()
                .map(row -> new TopDestination(row.city(), row.country(), row.trips()))
                .toList();
        return new DashboardResponse(statusCounts, totals, upcoming, top);
    }
}
