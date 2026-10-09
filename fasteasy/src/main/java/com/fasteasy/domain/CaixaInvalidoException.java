package com.fasteasy.domain;

/**
 * Exceção de negócio do módulo de caixa.
 *
 * <p>Estende {@link IllegalArgumentException} para que as violações de regra
 * de negócio sejam tratadas como falhas esperadas (mensagens em pt-BR ao
 * operador, sem stack trace) pelo aspecto de logging e pela interface.</p>
 */
public class CaixaInvalidoException extends IllegalArgumentException {

    public CaixaInvalidoException(String message) {
        super(message);
    }
}
