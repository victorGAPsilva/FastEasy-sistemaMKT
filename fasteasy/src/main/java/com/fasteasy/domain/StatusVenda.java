package com.fasteasy.domain;

/**
 * Status de uma venda.
 *
 * <p>O FastEasy não persiste carrinhos em andamento: a venda é gravada já
 * como {@link #COMPLETED}. {@link #IN_PROGRESS} e {@link #CANCELLED} ficam
 * previstos para evoluções futuras (ex.: cancelamento auditável).</p>
 */
public enum StatusVenda {

    IN_PROGRESS("Em andamento"),
    COMPLETED("Concluída"),
    CANCELLED("Cancelada");

    private final String descricao;

    StatusVenda(String descricao) {
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
