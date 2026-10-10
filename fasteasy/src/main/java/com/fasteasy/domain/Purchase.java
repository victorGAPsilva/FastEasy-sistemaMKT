package com.fasteasy.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade Purchase - representa uma compra de mercadorias de fornecedor.
 *
 * <p>Esta é uma entidade mínima para integração com o módulo financeiro.
 * O módulo completo de compras (Parte 6) expandirá esta entidade.</p>
 */
@Entity
@Table(name = "purchases")
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", length = 20, unique = true)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Supplier fornecedor;

    @Column(name = "data_emissao", nullable = false)
    private LocalDateTime dataEmissao;

    @Column(name = "data_prevista_entrega")
    private LocalDateTime dataPrevistaEntrega;

    @Column(name = "valor_total", precision = 19, scale = 4, nullable = false)
    private BigDecimal valorTotal;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "observacoes", length = 500)
    private String observacoes;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PurchaseItem> itens = new ArrayList<>();

    public Purchase() {
        this.status = "PENDING";
        this.dataCriacao = LocalDateTime.now();
        this.valorTotal = BigDecimal.ZERO;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Supplier getFornecedor() {
        return fornecedor;
    }

    public void setFornecedor(Supplier fornecedor) {
        this.fornecedor = fornecedor;
    }

    public LocalDateTime getDataEmissao() {
        return dataEmissao;
    }

    public void setDataEmissao(LocalDateTime dataEmissao) {
        this.dataEmissao = dataEmissao;
    }

    public LocalDateTime getDataPrevistaEntrega() {
        return dataPrevistaEntrega;
    }

    public void setDataPrevistaEntrega(LocalDateTime dataPrevistaEntrega) {
        this.dataPrevistaEntrega = dataPrevistaEntrega;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
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

    public List<PurchaseItem> getItens() {
        return itens;
    }

    public void setItens(List<PurchaseItem> itens) {
        this.itens = itens;
    }

    @PreUpdate
    protected void onUpdate() {
        this.dataAtualizacao = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Purchase{" +
                "id=" + id +
                ", numero='" + numero + '\'' +
                ", fornecedor=" + (fornecedor != null ? fornecedor.getNomeRazaoSocial() : "null") +
                ", valorTotal=" + valorTotal +
                ", status='" + status + '\'' +
                '}';
    }
}