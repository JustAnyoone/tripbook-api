package com.tt.tripbook.repository;

import com.tt.tripbook.model.Passageiro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassageiroRepository extends JpaRepository<Passageiro, Long> {

    boolean existsByCpf(String cpf);

    // Usado na atualização: outro passageiro (id diferente) já tem esse CPF?
    boolean existsByCpfAndIdNot(String cpf, Long id);
}
