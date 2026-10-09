package com.fasteasy.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade StockMovement - histórico auditável de movimentações de estoque.
 *
 * <p>Registra entradas, saídas, ajustes e saídas por venda, preservando a
 * quantidade anterior e posterior para auditoria. As movimentações de venda
 * usam o tipo {@link TipoMovimentacaoEstoque#SALE_EXIT} e guardam o
 * identificador da venda ({@link #vendaId}) quando aplicável.</p>
 */
@Entity
@Table(name = "stock_movements")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Product produto;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoMovimentacaoEstoque tipo;

    /** Quantidade movimentada (sempre positiva). */
    @Column(name = "quantidade", precision = 19, scale = 4, nullable = false)
    private BigDecimal quantidade;

    @Column(name = "quantidade_anterior", precision = 19, scale = 4, nullable = false)
    private BigDecimal quantidadeAnterior;

    @Column(name = "quantidade_posterior", precision = 19, scale = 4, nullable = false)
    private BigDecimal quantidadePosterior;

    @Column(name = "motivo", length = 255)
    private String motivo;

    /** Venda associada (apenas para movimentações do tipo SALE_EXIT). */
    @Column(name = "venda_id")
    private Long vendaId;

    @Column(name = "data_movimentacao", nullable = false)
    private LocalDateTime dataMovimentacao;

    public StockMovement() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduto() {
        return produto;
    }

    public void setProduto(Product produto) {
        this.produto = produto;
    }

    public TipoMovimentacaoEstoque getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimentacaoEstoque tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getQuantidadeAnterior() {
        return quantidadeAnterior;
    }

    public void setQuantidadeAnterior(BigDecimal quantidadeAnterior) {
        this.quantidadeAnterior = quantidadeAnterior;
    }

    public BigDecimal getQuantidadePosterior() {
        return quantidadePosterior;
    }

    public void setQuantidadePosterior(BigDecimal quantidadePosterior) {
        this.quantidadePosterior = quantidadePosterior;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public Long getVendaId() {
        return vendaId;
    }

    public void setVendaId(Long vendaId) {
        this.vendaId = vendaId;
    }

    public LocalDateTime getDataMovimentacao() {
        return dataMovimentacao;
    }

    public void setDataMovimentacao(LocalDateTime dataMovimentacao) {
        this.dataMovimentacao = dataMovimentacao;
    }
}
