package com.tt.tripbook.controller;

import com.tt.tripbook.model.Viagem;
import com.tt.tripbook.service.ViagemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/viagens")
@Tag(name = "Viagens", description = "Reservas de viagem: ligam um passageiro a um destino")
public class ViagemController {

    private final ViagemService service;

    public ViagemController(ViagemService service) {
        this.service = service;
    }

    @Operation(summary = "Lista todas as viagens")
    @ApiResponse(responseCode = "200", description = "Lista de viagens")
    @GetMapping
    public List<Viagem> listar() {
        return service.listar();
    }

    @Operation(summary = "Busca uma viagem pelo id")
    @ApiResponse(responseCode = "200", description = "Viagem encontrada")
    @ApiResponse(responseCode = "404", description = "Viagem não encontrada")
    @GetMapping("/{id}")
    public Viagem buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @Operation(summary = "Lista as viagens de um passageiro")
    @ApiResponse(responseCode = "200", description = "Viagens do passageiro")
    @ApiResponse(responseCode = "404", description = "Passageiro não encontrado")
    @GetMapping("/passageiro/{passageiroId}")
    public List<Viagem> porPassageiro(@PathVariable Long passageiroId) {
        return service.listarPorPassageiro(passageiroId);
    }

    @Operation(summary = "Lista as viagens para um destino")
    @ApiResponse(responseCode = "200", description = "Viagens do destino")
    @ApiResponse(responseCode = "404", description = "Destino não encontrado")
    @GetMapping("/destino/{destinoId}")
    public List<Viagem> porDestino(@PathVariable Long destinoId) {
        return service.listarPorDestino(destinoId);
    }

    @Operation(summary = "Cria uma viagem",
               description = "Informe apenas o id do passageiro e do destino, por exemplo: \"passageiro\": { \"id\": 1 }")
    @ApiResponse(responseCode = "201", description = "Viagem criada")
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou datas incoerentes")
    @ApiResponse(responseCode = "404", description = "Passageiro ou destino não encontrado")
    @PostMapping
    public ResponseEntity<Viagem> criar(@Valid @RequestBody Viagem viagem) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(viagem));
    }

    @Operation(summary = "Atualiza uma viagem existente")
    @ApiResponse(responseCode = "200", description = "Viagem atualizada")
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou datas incoerentes")
    @ApiResponse(responseCode = "404", description = "Viagem, passageiro ou destino não encontrado")
    @PutMapping("/{id}")
    public Viagem atualizar(@PathVariable Long id, @Valid @RequestBody Viagem viagem) {
        return service.atualizar(id, viagem);
    }

    @Operation(summary = "Remove uma viagem")
    @ApiResponse(responseCode = "204", description = "Viagem removida")
    @ApiResponse(responseCode = "404", description = "Viagem não encontrada")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
