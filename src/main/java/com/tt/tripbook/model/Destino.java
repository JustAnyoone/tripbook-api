package com.tt.tripbook.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "destino")
public class Destino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "a cidade é obrigatória")
    @Column(nullable = false)
    private String cidade;

    @NotBlank(message = "o país é obrigatória")
    @Column(nullable = false)
    private String pais;

    private String descricao;

    // Sem cascade de remoção: apagar um destino não deve apagar viagens de outras pessoas
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
