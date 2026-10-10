package com.fasteasy.service.dto;

import com.fasteasy.domain.PayableInstallmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de resumo de uma parcela de conta a pagar.
 */
public record PayableInstallmentResumo(
        Long id,
        Integer numeroParcela,
        LocalDate dataVencimento,
        BigDecimal valorOriginal,
        BigDecimal valorPago,
        BigDecimal saldoPendente,
        PayableInstallmentStatus status
) {
    public boolean isVencida() {
        return dataVencimento != null && dataVencimento.isBefore(LocalDate.now()) &&
                status != PayableInstallmentStatus.PAID && status != PayableInstallmentStatus.CANCELLED;
    }
}