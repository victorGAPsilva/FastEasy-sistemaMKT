package com.fasteasy.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Entidade SaleItem - item de uma venda.
 *
 * <p>Preserva o nome, o código e o preço do produto no momento da venda
 * (dados históricos), além de quantidade, desconto e total do item.</p>
 */
@Entity
@Table(name = "sale_items")
public class SaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venda_id", nullable = false)
    private Sale venda;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Product produto;

    /** Nome do produto no momento da venda. */
    @Column(name = "nome_produto", nullable = false, length = 150)
    private String nomeProduto;

    /** Código de barras ou SKU no momento da venda. */
    @Column(name = "codigo_produto", length = 50)
    private String codigoProduto;

    @Column(name = "quantidade", precision = 19, scale = 4, nullable = false)
    private BigDecimal quantidade;

    @Column(name = "preco_unitario", precision = 19, scale = 4, nullable = false)
    private BigDecimal precoUnitario;

    @Column(name = "desconto", precision = 19, scale = 4, nullable = false)
    private BigDecimal desconto = BigDecimal.ZERO;

    @Column(name = "total", precision = 19, scale = 4, nullable = false)
    private BigDecimal total;

    public SaleItem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Sale getVenda() {
        return venda;
    }

    public void setVenda(Sale venda) {
        this.venda = venda;
    }

    public Product getProduto() {
        return produto;
    }

    public void setProduto(Product produto) {
        this.produto = produto;
    }

    public String getNomeProduto() {
        return nomeProduto;
    }

    public void setNomeProduto(String nomeProduto) {
        this.nomeProduto = nomeProduto;
    }

    public String getCodigoProduto() {
        return codigoProduto;
    }

    public void setCodigoProduto(String codigoProduto) {
        this.codigoProduto = codigoProduto;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public BigDecimal getDesconto() {
        return desconto;
    }

    public void setDesconto(BigDecimal desconto) {
        this.desconto = desconto;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }
}
