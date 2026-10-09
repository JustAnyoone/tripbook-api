package com.tt.tripbook.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Entity
@Table(name = "passageiro")
public class Passageiro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "o nome é obrigatório") //É uma validação, que recusa o dado antes de chegar ao banco e permite uma mensagem
    @Column(nullable = false) //Restrição do banco
    private String nome;

    @NotBlank(message = "o CPF é obrigatório")
    @Pattern(regexp = "\\d{11}", message = "o CPF deve ter 11 dígitos numéricos")
    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @NotBlank(message = "o e-mail é obrigatório")
    @Email(message = "e-mail inválido")
    @Column(nullable = false)
    private String email;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    // Composição: a viagem não existe sem o passageiro
    @OneToMany(mappedBy = "passageiro", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("passageiro")
    private List<Viagem> viagens = new ArrayList<>();

    

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
    public List<Viagem> getViagens() { return viagens; }
    public void setViagens(List<Viagem> viagens) { this.viagens = viagens; }

    
}
