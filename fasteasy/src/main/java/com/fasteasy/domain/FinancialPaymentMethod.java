package com.fasteasy.domain;

/**
 * Métodos de pagamento para contas a pagar e despesas.
 *
 * <p>Estende as formas de pagamento do PDV adicionando boleto e transferência,
 * comuns em pagamentos a fornecedores.</p>
 */
public enum FinancialPaymentMethod {

    DINHEIRO("Dinheiro"),
    PIX("PIX"),
    TRANSFERENCIA("Transferência"),
    BOLETO("Boleto"),
    CARTAO("Cartão"),
    OUTRO("Outro");

    private final String descricao;

    FinancialPaymentMethod(String descricao) {
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