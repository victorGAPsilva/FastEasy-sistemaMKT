package com.fasteasy.domain;

/**
 * Indica que não há estoque suficiente para concluir uma saída.
 *
 * <p>Estende {@link IllegalArgumentException} para ser tratada como erro de
 * regra de negócio, com mensagem amigável em pt-BR exibida ao operador.</p>
 */
public class EstoqueInsuficienteException extends IllegalArgumentException {

    public EstoqueInsuficienteException(String message) {
        super(message);
    }
}
