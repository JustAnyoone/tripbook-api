package com.tt.tripbook.service;

import com.tt.tripbook.exception.ConflitoException;
import com.tt.tripbook.exception.RecursoNaoEncontradoException;
import com.tt.tripbook.model.Passageiro;
import com.tt.tripbook.repository.PassageiroRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PassageiroService {

    private final PassageiroRepository repository;

    public PassageiroService(PassageiroRepository repository) {
        this.repository = repository;
    }

    public List<Passageiro> listar() {
        return repository.findAll();
    }

    public Passageiro buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Passageiro não encontrado: id " + id));
    }

    public Passageiro criar(Passageiro passageiro) {
        if (repository.existsByCpf(passageiro.getCpf())) {
            throw new ConflitoException("Já existe um passageiro com o CPF informado");
        }
        passageiro.setId(null);
        // Viagens são criadas pelo endpoint de viagens, nunca por aqui
        passageiro.setViagens(new ArrayList<>());
        return repository.save(passageiro);
    }

    public Passageiro atualizar(Long id, Passageiro novo) {
        Passageiro existente = buscar(id);
        if (repository.existsByCpfAndIdNot(novo.getCpf(), id)) {
            throw new ConflitoException("Já existe outro passageiro com o CPF informado");
        }
        existente.setNome(novo.getNome());
        existente.setCpf(novo.getCpf());
        existente.setEmail(novo.getEmail());
        existente.setDataNascimento(novo.getDataNascimento());
        // A lista de viagens não é copiada de propósito: com orphanRemoval = true,
        // uma lista vinda do cliente poderia apagar viagens existentes.
        return repository.save(existente);
    }

    public void deletar(Long id) {
        // As viagens do passageiro são removidas junto (cascade + orphanRemoval)
        repository.delete(buscar(id));
    }
}
