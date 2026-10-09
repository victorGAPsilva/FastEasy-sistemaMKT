package com.fasteasy.domain;

/**
 * Tipo de movimentação de estoque.
 *
 * <p>{@code SALE_EXIT} identifica a saída gerada automaticamente por uma
 * venda concluída, permitindo rastrear o consumo de estoque por venda.</p>
 */
public enum TipoMovimentacaoEstoque {

    ENTRADA("Entrada"),
    SAIDA("Saída"),
    AJUSTE("Ajuste"),
    SALE_EXIT("Saída por venda");

    private final String descricao;

    TipoMovimentacaoEstoque(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
