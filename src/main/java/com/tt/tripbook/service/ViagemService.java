package com.tt.tripbook.service;

import com.tt.tripbook.exception.RecursoNaoEncontradoException;
import com.tt.tripbook.exception.RegraDeNegocioException;
import com.tt.tripbook.model.Destino;
import com.tt.tripbook.model.Passageiro;
import com.tt.tripbook.model.Viagem;
import com.tt.tripbook.repository.ViagemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ViagemService {

    private final ViagemRepository repository;
    private final PassageiroService passageiroService;
    private final DestinoService destinoService;

    public ViagemService(ViagemRepository repository,
                         PassageiroService passageiroService,
                         DestinoService destinoService) {
        this.repository = repository;
        this.passageiroService = passageiroService;
        this.destinoService = destinoService;
    }

    public List<Viagem> listar() {
        return repository.findAll();
    }

    public Viagem buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Viagem não encontrada: id " + id));
    }

    public List<Viagem> listarPorPassageiro(Long passageiroId) {
        passageiroService.buscar(passageiroId); // 404 se o passageiro não existir
        return repository.findByPassageiroId(passageiroId);
    }

    public List<Viagem> listarPorDestino(Long destinoId) {
        destinoService.buscar(destinoId); // 404 se o destino não existir
        return repository.findByDestinoId(destinoId);
    }

    public Viagem criar(Viagem viagem) {
        validarDatas(viagem);
        viagem.setId(null);
        viagem.setPassageiro(resolverPassageiro(viagem));
        viagem.setDestino(resolverDestino(viagem));
        return repository.save(viagem);
    }

    public Viagem atualizar(Long id, Viagem nova) {
        Viagem existente = buscar(id);
        validarDatas(nova);
        existente.setDataIda(nova.getDataIda());
        existente.setDataVolta(nova.getDataVolta());
        existente.setValor(nova.getValor());
        existente.setStatus(nova.getStatus());
        existente.setPassageiro(resolverPassageiro(nova));
        existente.setDestino(resolverDestino(nova));
        return repository.save(existente);
    }

    public void deletar(Long id) {
        repository.delete(buscar(id));
    }

    // Regra: a volta não pode ser anterior à ida
    private void validarDatas(Viagem viagem) {
        if (viagem.getDataVolta() != null && viagem.getDataVolta().isBefore(viagem.getDataIda())) {
            throw new RegraDeNegocioException("A data de volta não pode ser anterior à data de ida");
        }
    }

    // Regra: o passageiro informado precisa existir. O JSON traz só o id,
    // então buscamos o objeto completo no banco.
    private Passageiro resolverPassageiro(Viagem viagem) {
        if (viagem.getPassageiro().getId() == null) {
            throw new RegraDeNegocioException("Informe o id do passageiro");
        }
        return passageiroService.buscar(viagem.getPassageiro().getId());
    }

    private Destino resolverDestino(Viagem viagem) {
        if (viagem.getDestino().getId() == null) {
            throw new RegraDeNegocioException("Informe o id do destino");
        }
        return destinoService.buscar(viagem.getDestino().getId());
    }
}
