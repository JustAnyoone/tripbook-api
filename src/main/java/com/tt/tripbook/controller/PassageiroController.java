package com.tt.tripbook.controller;

import com.tt.tripbook.model.Passageiro;
import com.tt.tripbook.service.PassageiroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/passageiros")
@Tag(name = "Passageiros", description = "Cadastro e consulta de passageiros")
public class PassageiroController {

    private final PassageiroService service;

    public PassageiroController(PassageiroService service) {
        this.service = service;
    }

    @Operation(summary = "Lista todos os passageiros")
    @ApiResponse(responseCode = "200", description = "Lista de passageiros")
    @GetMapping
    public List<Passageiro> listar() {
        return service.listar();
    }

    @Operation(summary = "Busca um passageiro pelo id")
    @ApiResponse(responseCode = "200", description = "Passageiro encontrado")
    @ApiResponse(responseCode = "404", description = "Passageiro não encontrado")
    @GetMapping("/{id}")
    public Passageiro buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @Operation(summary = "Cadastra um novo passageiro")
    @ApiResponse(responseCode = "201", description = "Passageiro criado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "409", description = "CPF já cadastrado")
    @PostMapping
    public ResponseEntity<Passageiro> criar(@Valid @RequestBody Passageiro passageiro) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(passageiro));
    }

    @Operation(summary = "Atualiza um passageiro existente")
    @ApiResponse(responseCode = "200", description = "Passageiro atualizado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "404", description = "Passageiro não encontrado")
    @ApiResponse(responseCode = "409", description = "CPF já pertence a outro passageiro")
    @PutMapping("/{id}")
    public Passageiro atualizar(@PathVariable Long id, @Valid @RequestBody Passageiro passageiro) {
        return service.atualizar(id, passageiro);
    }

    @Operation(summary = "Remove um passageiro e as viagens dele")
    @ApiResponse(responseCode = "204", description = "Passageiro removido")
    @ApiResponse(responseCode = "404", description = "Passageiro não encontrado")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
