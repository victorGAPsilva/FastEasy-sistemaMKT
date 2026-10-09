package com.fasteasy.domain;

/**
 * Indica uma operação de venda inválida (carrinho, pagamento ou finalização).
 *
 * <p>Estende {@link IllegalArgumentException} para ser tratada como erro de
 * regra de negócio, com mensagem amigável em pt-BR exibida ao operador.</p>
 */
public class VendaInvalidaException extends IllegalArgumentException {

    public VendaInvalidaException(String message) {
        super(message);
    }

    public VendaInvalidaException(String message, Throwable cause) {
        super(message, cause);
    }
}
