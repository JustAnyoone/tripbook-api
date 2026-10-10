package com.tt.tripbook.controller;

import com.tt.tripbook.model.Destino;
import com.tt.tripbook.service.DestinoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/destinos")
@Tag(name = "Destinos", description = "Cadastro e consulta de destinos de viagem")
public class DestinoController {

    private final DestinoService service;

    public DestinoController(DestinoService service) {
        this.service = service;
    }

    @Operation(summary = "Lista todos os destinos")
    @ApiResponse(responseCode = "200", description = "Lista de destinos")
    @GetMapping
    public List<Destino> listar() {
        return service.listar();
    }

    @Operation(summary = "Busca um destino pelo id")
    @ApiResponse(responseCode = "200", description = "Destino encontrado")
    @ApiResponse(responseCode = "404", description = "Destino não encontrado")
    @GetMapping("/{id}")
    public Destino buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @Operation(summary = "Cria um novo destino")
    @ApiResponse(responseCode = "201", description = "Destino criado")
    @PostMapping
    public ResponseEntity<Destino> criar(@Valid @RequestBody Destino destino) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(destino));
    }

    @Operation(summary = "Atualiza um destino existente")
    @ApiResponse(responseCode = "200", description = "Destino atualizado")
    @ApiResponse(responseCode = "404", description = "Destino não encontrado")
    @PutMapping("/{id}")
    public Destino atualizar(@PathVariable Long id, @Valid @RequestBody Destino destino) {
        return service.atualizar(id, destino);
    }

    @Operation(summary = "Remove um destino")
    @ApiResponse(responseCode = "204", description = "Destino removido")
    @ApiResponse(responseCode = "404", description = "Destino não encontrado")
    @ApiResponse(responseCode = "409", description = "Destino possui viagens associadas")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
