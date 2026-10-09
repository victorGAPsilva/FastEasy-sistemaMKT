package com.fasteasy.service.dto;

import com.fasteasy.domain.StatusVenda;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Resumo de uma venda para a tela de histórico.
 */
public record VendaResumo(Long id,
                          String numero,
                          LocalDateTime dataHora,
                          BigDecimal valorTotal,
                          String formasPagamento,
                          StatusVenda status,
                          String operador) {
}
