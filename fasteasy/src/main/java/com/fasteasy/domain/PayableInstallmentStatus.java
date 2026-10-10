package com.fasteasy.domain;

/**
 * Status de uma parcela de conta a pagar.
 */
public enum PayableInstallmentStatus {

    OPEN("Aberta"),
    PARTIALLY_PAID("Parcialmente paga"),
    PAID("Paga"),
    CANCELLED("Cancelada");

    private final String descricao;

    PayableInstallmentStatus(String descricao) {
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