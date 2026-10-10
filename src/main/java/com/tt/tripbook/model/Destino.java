package com.tt.tripbook.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

@Schema(description = "Local de destino de uma viagem")
@Entity
@Table(name = "destino")
public class Destino {

    @Schema(description = "Identificador gerado automaticamente", accessMode = Schema.AccessMode.READ_ONLY)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Schema(description = "Nome da cidade", example = "Lisboa")
    @NotBlank(message = "a cidade é obrigatória")
    @Column(nullable = false)
    private String cidade;

    @Schema(description = "País do destino", example = "Portugal")
    @NotBlank(message = "o país é obrigatória")
    @Column(nullable = false)
    private String pais;

    @Schema(description = "Texto livre sobre o destino", example = "Capital portuguesa, conhecida pelos bondes e pelo Tejo")
    private String descricao;

    // Sem cascade de remoção: apagar um destino não deve apagar viagens de outras pessoas
    @Schema(description = "Viagens associadas a este destino", accessMode = Schema.AccessMode.READ_ONLY)
    @OneToMany(mappedBy = "destino")
    @JsonIgnoreProperties("destino")
    private List<Viagem> viagens = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }
    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public List<Viagem> getViagens() { return viagens; }
    public void setViagens(List<Viagem> viagens) { this.viagens = viagens; }
}
