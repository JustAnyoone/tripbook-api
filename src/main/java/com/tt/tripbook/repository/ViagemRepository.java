package com.tt.tripbook.repository;

import com.tt.tripbook.model.Viagem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ViagemRepository extends JpaRepository<Viagem, Long> {

    // Atravessam o relacionamento: viagem.passageiro.id e viagem.destino.id
    List<Viagem> findByPassageiroId(Long passageiroId);

    List<Viagem> findByDestinoId(Long destinoId);

    boolean existsByDestinoId(Long destinoId);
}