package com.fasteasy.domain;

/**
 * Exceção de negócio para operações financeiras inválidas.
 *
 * <p>Estende {@link IllegalArgumentException} para que o aspect de logging
 * a trate como WARN (não ERROR) e a UI possa exibir a mensagem ao usuário.</p>
 */
public class FinanceiroInvalidoException extends IllegalArgumentException {

    public FinanceiroInvalidoException(String message) {
        super(message);
    }
}