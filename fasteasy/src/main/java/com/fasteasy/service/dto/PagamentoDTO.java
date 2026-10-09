package com.fasteasy.service.dto;

import com.fasteasy.domain.FormaPagamento;

import java.math.BigDecimal;

/**
 * Pagamento informado pelo operador no PDV.
 *
 * @param formaPagamento forma de pagamento
 * @param valor          valor aplicado à venda
 * @param valorRecebido  valor entregue pelo cliente (para dinheiro pode ser
 *                       maior que {@code valor}, gerando troco; ignorado nas
 *                       demais formas)
 */
public record PagamentoDTO(FormaPagamento formaPagamento, BigDecimal valor, BigDecimal valorRecebido) {
}
