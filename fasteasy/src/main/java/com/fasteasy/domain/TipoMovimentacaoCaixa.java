package com.fasteasy.domain;

/**
 * Tipos de movimentação financeira manual do caixa.
 *
 * <p>{@link #SUPPLY} e {@link #OTHER_IN} aumentam o dinheiro físico;
 * {@link #WITHDRAWAL} e {@link #OTHER_OUT} diminuem. As movimentações são
 * imutáveis após o registro — correções devem ser feitas por uma movimentação
 * compensatória auditável.</p>
 */
public enum TipoMovimentacaoCaixa {

    SUPPLY("Suprimento", true),
    WITHDRAWAL("Sangria", false),
    OTHER_IN("Outra entrada", true),
    OTHER_OUT("Outra saída", false);

    private final String descricao;
    private final boolean entrada;

    TipoMovimentacaoCaixa(String descricao, boolean entrada) {
        this.descricao = descricao;
        this.entrada = entrada;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Indica se o tipo representa entrada de dinheiro no caixa. */
    public boolean isEntrada() {
        return entrada;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
