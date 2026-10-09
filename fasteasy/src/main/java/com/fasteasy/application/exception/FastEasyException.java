package com.fasteasy.application.exception;

/**
 * Exceção base do FastEasy para erros de regra de negócio.
 * Carrega uma mensagem amigável exibida ao usuário.
 */
public class FastEasyException extends RuntimeException {

    public FastEasyException(String message) {
        super(message);
    }

    public FastEasyException(String message, Throwable cause) {
        super(message, cause);
    }
}
