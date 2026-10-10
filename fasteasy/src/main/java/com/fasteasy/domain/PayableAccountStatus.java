package com.fasteasy.domain;

/**
 * Status de uma conta a pagar.
 */
public enum PayableAccountStatus {

    OPEN("Aberta"),
    PARTIALLY_PAID("Parcialmente paga"),
    PAID("Paga"),
    CANCELLED("Cancelada");

    private final String descricao;

    PayableAccountStatus(String descricao) {
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