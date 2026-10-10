package com.fasteasy.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Categoria de despesa operacional.
 *
 * <p>Diferente de {@link Category} (categoria de produto), esta enumera
 * tipos de despesas financeiras como aluguel, energia, salários, etc.</p>
 */
@Entity
@Table(name = "expense_categories")
public class ExpenseCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 100, unique = true)
    private String nome;

    @Column(name = "descricao", length = 255)
    private String descricao;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    public ExpenseCategory() {
        this.status = "ATIVO";
        this.dataCriacao = LocalDateTime.now();
    }

    public ExpenseCategory(String nome) {
        this();
        this.nome = nome;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }

    public void setDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }

    public boolean isAtivo() {
        return "ATIVO".equals(status);
    }

    @PreUpdate
    protected void onUpdate() {
        this.dataAtualizacao = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "ExpenseCategory{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}