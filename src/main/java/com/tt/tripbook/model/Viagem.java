package com.tt.tripbook.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "viagem")
public class Viagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_ida", nullable = false)
    private LocalDate dataIda;

    @Column(name = "data_volta")
    private LocalDate dataVolta;

    @Column(nullable = false, precision = 10, scale = 2) // Vira decimal (10,2) no banco
    private BigDecimal valor;

    @Column(nullable = false)
    private String status;

    @ManyToOne(optional = false)
    @JoinColumn(name = "passageiro_id")
    @JsonIgnoreProperties("viagens")
    private Passageiro passageiro;

    @ManyToOne(optional = false)
    @JoinColumn(name = "destino_id")
    @JsonIgnoreProperties("viagens")
    private Destino destino;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getDataIda() { return dataIda; }
    public void setDataIda(LocalDate dataIda) { this.dataIda = dataIda; }
    public LocalDate getDataVolta() { return dataVolta; }
    public void setDataVolta(LocalDate dataVolta) { this.dataVolta = dataVolta; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Passageiro getPassageiro() { return passageiro; }
    public void setPassageiro(Passageiro passageiro) { this.passageiro = passageiro; }
    public Destino getDestino() { return destino; }
    public void setDestino(Destino destino) { this.destino = destino; }
}
