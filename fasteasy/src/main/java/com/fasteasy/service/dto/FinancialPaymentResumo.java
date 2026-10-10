package com.fasteasy.service.dto;

import com.fasteasy.domain.FinancialPaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de resumo de um pagamento financeiro.
 */
public record FinancialPaymentResumo(
        Long id,
        Long contaId,
        Long parcelaId,
        BigDecimal valor,
        LocalDateTime dataHora,
        FinancialPaymentMethod metodoPagamento,
        String referencia,
        String observacoes,
        String operador,
        String status
) {
}