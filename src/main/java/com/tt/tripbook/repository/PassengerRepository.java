package com.tt.tripbook.repository;

import com.tt.tripbook.model.Passenger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassengerRepository extends JpaRepository<Passenger, Long> {

    boolean existsByDocumentNumber(String documentNumber);

    // Used on update: does another passenger (different id) already have this document number?
    boolean existsByDocumentNumberAndIdNot(String documentNumber, Long id);

    // Case-insensitive "contains" filter on the name, with paging and sorting
    Page<Passenger> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
