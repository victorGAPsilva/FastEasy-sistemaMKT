package com.fasteasy.domain;

/**
 * Formas de pagamento aceitas no PDV.
 *
 * <p>O registro representa apenas a forma informada pelo operador. Não há
 * integração com bancos, adquirentes ou APIs de pagamento — portanto nenhuma
 * transação é confirmada externamente.</p>
 */
public enum FormaPagamento {

    DINHEIRO("Dinheiro"),
    PIX("PIX"),
    DEBITO("Débito"),
    CREDITO("Crédito");

    private final String descricao;

    FormaPagamento(String descricao) {
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
