package com.fasteasy.domain;

/**
 * Enum para unidades de medida de produtos.
 * Valores padronizados para o sistema FastEasy.
 */
public enum UnidadeMedida {
    UN("UN", "Unidade"),
    KG("KG", "Quilograma"),
    G("G", "Grama"),
    L("L", "Litro"),
    ML("ML", "Mililitro"),
    PCT("PCT", "Pacote"),
    CX("CX", "Caixa");

    private final String codigo;
    private final String descricao;

    UnidadeMedida(String codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return codigo + " - " + descricao;
    }
}