package com.fasteasy.domain;

import java.math.BigDecimal;

/**
 * Item de um carrinho de venda (em memória).
 *
 * <p>Guarda um "snapshot" do produto no momento em que foi adicionado
 * (nome, código, unidade e preço), garantindo que o total seja calculado com
 * os valores acordados durante o atendimento.</p>
 */
public class ItemCarrinho {

    private Long produtoId;
    private String nome;
    private String codigo;
    private UnidadeMedida unidadeMedida;
    private BigDecimal precoUnitario = BigDecimal.ZERO;
    private BigDecimal quantidade = BigDecimal.ZERO;
    private BigDecimal desconto = BigDecimal.ZERO;

    public ItemCarrinho() {
    }

    public ItemCarrinho(Long produtoId, String nome, String codigo,
                        UnidadeMedida unidadeMedida, BigDecimal precoUnitario,
                        BigDecimal quantidade) {
        this.produtoId = produtoId;
        this.nome = nome;
        this.codigo = codigo;
        this.unidadeMedida = unidadeMedida;
        this.precoUnitario = Dinheiro.ouZero(precoUnitario);
        this.quantidade = Dinheiro.ouZero(quantidade);
    }

    /** Total bruto do item (preço × quantidade), sem desconto, arredondado. */
    public BigDecimal getTotalBruto() {
        return Dinheiro.arredondar(precoUnitario.multiply(quantidade));
    }

    /** Total do item já com desconto aplicado (nunca negativo), arredondado. */
    public BigDecimal getTotal() {
        BigDecimal total = getTotalBruto().subtract(Dinheiro.ouZero(desconto));
        if (total.signum() < 0) {
            total = BigDecimal.ZERO;
        }
        return Dinheiro.arredondar(total);
    }

    public Long getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(Long produtoId) {
        this.produtoId = produtoId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public UnidadeMedida getUnidadeMedida() {
        return unidadeMedida;
    }

    public void setUnidadeMedida(UnidadeMedida unidadeMedida) {
        this.unidadeMedida = unidadeMedida;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = Dinheiro.ouZero(precoUnitario);
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = Dinheiro.ouZero(quantidade);
    }

    public BigDecimal getDesconto() {
        return desconto;
    }

    public void setDesconto(BigDecimal desconto) {
        this.desconto = Dinheiro.ouZero(desconto);
    }
}
