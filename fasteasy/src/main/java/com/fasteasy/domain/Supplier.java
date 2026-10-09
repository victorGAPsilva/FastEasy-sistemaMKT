package com.fasteasy.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Entidade Supplier - representa fornecedores do supermercado.
 * Contém CPF/CNPJ opcional com validação de formato.
 */
@Entity
@Table(name = "suppliers")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome ou razão social é obrigatório")
    @Size(max = 150, message = "A razão social deve ter no máximo 150 caracteres")
    @Column(name = "nome_razao_social", nullable = false, length = 150)
    private String nomeRazaoSocial;

    @Column(name = "nome_fantasia", length = 100)
    private String nomeFantasia;

    @Column(name = "cpf_cnpj", length = 20)
    private String cpfCnpj;

    @Column(name = "telefone", length = 20)
    private String telefone;

    @Email(message = "E-mail inválido")
    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "endereco", length = 200)
    private String endereco;

    @Column(name = "observacoes", length = 500)
    private String observacoes;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private LocalDateTime dataCadastro;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    // Construtores
    public Supplier() {
        this.status = "ATIVO";
        this.dataCadastro = LocalDateTime.now();
    }

    public Supplier(String nomeRazaoSocial) {
        this();
        this.nomeRazaoSocial = nomeRazaoSocial;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNomeRazaoSocial() {
        return nomeRazaoSocial;
    }

    public void setNomeRazaoSocial(String nomeRazaoSocial) {
        this.nomeRazaoSocial = nomeRazaoSocial;
    }

    public String getNomeFantasia() {
        return nomeFantasia;
    }

    public void setNomeFantasia(String nomeFantasia) {
        this.nomeFantasia = nomeFantasia;
    }

    public String getCpfCnpj() {
        return cpfCnpj;
    }

    public void setCpfCnpj(String cpfCnpj) {
        this.cpfCnpj = cpfCnpj;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(LocalDateTime dataCadastro) {
        this.dataCadastro = dataCadastro;
    }

    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }

    public void setDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }

    @PreUpdate
    protected void onUpdate() {
        this.dataAtualizacao = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Supplier{" +
                "id=" + id +
                ", nomeRazaoSocial='" + nomeRazaoSocial + '\'' +
                ", nomeFantasia='" + nomeFantasia + '\'' +
                ", cpfCnpj='" + cpfCnpj + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}