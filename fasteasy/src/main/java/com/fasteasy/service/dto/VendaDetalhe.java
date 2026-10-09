package com.fasteasy.service.dto;

import com.fasteasy.domain.StatusVenda;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detalhe completo de uma venda, usado no comprovante e na consulta.
 */
public record VendaDetalhe(Long id,
                           String numero,
                           LocalDateTime dataHora,
                           StatusVenda status,
                           BigDecimal subtotal,
                           BigDecimal descontoTotal,
                           BigDecimal valorTotal,
                           BigDecimal valorRecebido,
                           BigDecimal troco,
                           String operador,
                           List<ItemVendaDTO> itens,
                           List<PagamentoVendaDTO> pagamentos) {
}
