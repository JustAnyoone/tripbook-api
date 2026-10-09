//@Service -> regra de "nao existe, entao 404"

package com.tt.tripbook.service;

import com.tt.tripbook.exception.ConflitoException;
import com.tt.tripbook.exception.RecursoNaoEncontradoException;
import com.tt.tripbook.model.Destino;
import com.tt.tripbook.repository.DestinoRepository;
import com.tt.tripbook.repository.ViagemRepository;

import com.tt.tripbook.exception.ConflitoException;
import com.tt.tripbook.repository.ViagemRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DestinoService {

    private final DestinoRepository repository;
    private final ViagemRepository viagemRepository;

    public DestinoService(DestinoRepository repository, ViagemRepository viagemRepository) {
        this.repository = repository;
        this.viagemRepository = viagemRepository;
    }

    public List<Destino> listar() {
        return repository.findAll();
    }

    public Destino buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Destino não encontrado: id " + id));
    }

    public Destino criar(Destino destino) {
        destino.setId(null); // garante INSERT, mesmo que o cliente envie um id
        return repository.save(destino);
    }

    public Destino atualizar(Long id, Destino novo) {
        Destino existente = buscar(id);
        existente.setCidade(novo.getCidade());
        existente.setPais(novo.getPais());
        existente.setDescricao(novo.getDescricao());
        return repository.save(existente);
    }

    public void deletar(Long id) {
    Destino existente = buscar(id);
    if (viagemRepository.existsByDestinoId(id)) {
        throw new ConflitoException("Não é possível remover o destino: existem viagens associadas a ele");
    }
    repository.delete(existente);
    }
}
