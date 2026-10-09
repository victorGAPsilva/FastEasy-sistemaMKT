package com.fasteasy.application.exception;

/**
 * Indica uma falha de validação de dados de entrada.
 *
 * Estende {@link IllegalArgumentException} para que possa ser tratada
 * tanto como erro de validação específico quanto como argumento inválido.
 */
public class ValidacaoException extends IllegalArgumentException {

    public ValidacaoException(String message) {
        super(message);
    }

    public ValidacaoException(String message, Throwable cause) {
        super(message, cause);
    }
}
