package com.fasteasy.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Item de uma compra de mercadorias.
 */
@Entity
@Table(name = "purchase_items")
public class PurchaseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compra_id", nullable = false)
    private Purchase compra;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Product produto;

    @Column(name = "quantidade", precision = 19, scale = 4, nullable = false)
    private BigDecimal quantidade;

    @Column(name = "preco_unitario", precision = 19, scale = 4, nullable = false)
    private BigDecimal precoUnitario;

    @Column(name = "total", precision = 19, scale = 4, nullable = false)
    private BigDecimal total;

    public PurchaseItem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Purchase getCompra() {
        return compra;
    }

    public void setCompra(Purchase compra) {
        this.compra = compra;
    }

    public Product getProduto() {
        return produto;
    }

    public void setProduto(Product produto) {
        this.produto = produto;
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

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    @Override
    public String toString() {
        return "PurchaseItem{" +
                "id=" + id +
                ", produto=" + (produto != null ? produto.getNome() : "null") +
                ", quantidade=" + quantidade +
                ", precoUnitario=" + precoUnitario +
                ", total=" + total +
                '}';
    }
}