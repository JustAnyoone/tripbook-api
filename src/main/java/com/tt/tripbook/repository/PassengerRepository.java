package com.tt.tripbook.repository;

import com.tt.tripbook.model.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassengerRepository extends JpaRepository<Passenger, Long> {

    boolean existsByDocumentNumber(String documentNumber);

    // Usado na atualização: outro Passenger (id diferente) já tem esse DocumentNumber?
    boolean existsByDocumentNumberAndIdNot(String documentNumber, Long id);
}
