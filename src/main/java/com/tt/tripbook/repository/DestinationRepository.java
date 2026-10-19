package com.tt.tripbook.repository;

import com.tt.tripbook.model.Destination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DestinationRepository extends JpaRepository<Destination, Long> {

    // Case-insensitive "contains" filter on the country, with paging and sorting
    Page<Destination> findByCountryContainingIgnoreCase(String country, Pageable pageable);

    // Duplicate checks: the same city in the same country, ignoring letter case
    boolean existsByCityIgnoreCaseAndCountryIgnoreCase(String city, String country);

    // Same check for updates: the destination being edited must not count as its own duplicate
    boolean existsByCityIgnoreCaseAndCountryIgnoreCaseAndIdNot(String city, String country, Long id);
}
