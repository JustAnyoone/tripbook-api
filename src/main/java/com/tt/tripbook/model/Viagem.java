package com.tt.tripbook.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "viagem")
public class Viagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "a data de ida é obrigatória")
    @Column(name = "data_ida", nullable = false)
    private LocalDate dataIda;

    @Column(name = "data_volta")
    private LocalDate dataVolta;

    @NotNull(message = "o valor é obrigatório")
    @Positive(message = "o valor deve ser maior que zero")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @NotBlank(message = "o status é obrigatório")
    @Pattern(regexp = "PENDENTE|CONFIRMADA|CANCELADA",
            message = "o status deve ser PENDENTE, CONFIRMADA ou CANCELADA")
    @Column(nullable = false)
    private String status;

    @NotNull(message = "o passageiro é obrigatório")
    @ManyToOne(optional = false)
    @JoinColumn(name = "passageiro_id")
    @JsonIgnoreProperties("viagens")
    private Passageiro passageiro;

    @NotNull(message = "o destino é obrigatório")
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
