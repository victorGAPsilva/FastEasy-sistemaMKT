package com.fasteasy.service.dto;

import com.fasteasy.domain.StatusSessaoCaixa;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de listagem do histórico de sessões de caixa.
 */
public record SessaoCaixaResumo(
        Long id,
        String caixaNome,
        String operador,
        LocalDateTime dataAbertura,
        LocalDateTime dataFechamento,
        BigDecimal saldoInicial,
        BigDecimal saldoEsperado,
        BigDecimal totalContado,
        BigDecimal diferenca,
        StatusSessaoCaixa status) {
}
