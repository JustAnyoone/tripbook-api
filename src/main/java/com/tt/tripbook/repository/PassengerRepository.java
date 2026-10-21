package com.tt.tripbook.repository;

import com.tt.tripbook.model.AppUser;
import com.tt.tripbook.model.Passenger;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

// Every query is scoped to one owner, so an account can never read or change another account's passengers.
public interface PassengerRepository extends JpaRepository<Passenger, Long> {

    Page<Passenger> findByOwnerId(Long ownerId, Pageable pageable);

    // Case-insensitive "contains" filter on the name, with paging and sorting
    Page<Passenger> findByOwnerIdAndNameContainingIgnoreCase(Long ownerId, String name, Pageable pageable);

    Optional<Passenger> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByOwnerIdAndDocumentNumber(Long ownerId, String documentNumber);

    // Used on update: does another passenger (different id) of the same owner already have this document number?
    boolean existsByOwnerIdAndDocumentNumberAndIdNot(Long ownerId, String documentNumber, Long id);

    // Hands passengers created before accounts owned data to one account; returns how many changed
    @Transactional
    @Modifying
    @Query("update Passenger p set p.owner = :owner where p.owner is null")
    int assignOrphansTo(@Param("owner") AppUser owner);
}
