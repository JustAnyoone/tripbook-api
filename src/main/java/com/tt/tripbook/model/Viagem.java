package com.tt.tripbook.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

@Schema(description = "Reserva que liga um passageiro a um destino")
@Entity
@Table(name = "viagem")
public class Viagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Schema(description = "Data de partida", example = "2026-12-20")
    @NotNull(message = "a data de ida é obrigatória")
    @Column(name = "data_ida", nullable = false)
    private LocalDate dataIda;

    @Schema(description = "Data de retorno (opcional)", example = "2026-12-30")
    @Column(name = "data_volta")
    private LocalDate dataVolta;

    @Schema(description = "Valor total em reais", example = "3500.50")
    @NotNull(message = "o valor é obrigatório")
    @Positive(message = "o valor deve ser maior que zero")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Schema(description = "Situação da reserva", example = "CONFIRMADA", allowableValues = {"PENDENTE", "CONFIRMADA", "CANCELADA"})
    @NotBlank(message = "o status é obrigatório")
    @Pattern(regexp = "PENDENTE|CONFIRMADA|CANCELADA",
            message = "o status deve ser PENDENTE, CONFIRMADA ou CANCELADA")
    @Column(nullable = false)
    private String status;

    @Schema(description = "Informe apenas o id do passageiro", example = "{\"id\": 1}")
    @NotNull(message = "o passageiro é obrigatório")
    @ManyToOne(optional = false)
    @JoinColumn(name = "passageiro_id")
    @JsonIgnoreProperties("viagens")
    private Passageiro passageiro;

    @Schema(description = "Informe apenas o id do destino", example = "{\"id\": 1}")
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
