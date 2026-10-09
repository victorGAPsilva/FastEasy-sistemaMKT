package com.fasteasy.service.dto;

import com.fasteasy.domain.FormaPagamento;

import java.math.BigDecimal;

/**
 * Pagamento registrado em uma venda, para comprovante e consulta.
 */
public record PagamentoVendaDTO(FormaPagamento formaPagamento,
                                BigDecimal valor,
                                BigDecimal valorRecebido,
                                BigDecimal troco) {
}
