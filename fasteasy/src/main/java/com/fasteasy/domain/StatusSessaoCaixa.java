package com.fasteasy.domain;

/**
 * Status de uma sessão de caixa.
 *
 * <p>Uma sessão é criada como {@link #OPEN} na abertura e passa para
 * {@link #CLOSED} no fechamento, quando os valores finais e a diferença são
 * registrados. Uma sessão fechada nunca é reaberta: para continuar operando é
 * necessário abrir uma nova sessão.</p>
 */
public enum StatusSessaoCaixa {

    OPEN("Aberto"),
    CLOSED("Fechado");

    private final String descricao;

    StatusSessaoCaixa(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isAberta() {
        return this == OPEN;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
