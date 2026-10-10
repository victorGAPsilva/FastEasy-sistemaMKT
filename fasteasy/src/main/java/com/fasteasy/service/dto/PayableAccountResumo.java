package com.fasteasy.service.dto;

import com.fasteasy.domain.ExpenseCategory;
import com.fasteasy.domain.PayableAccountStatus;
import com.fasteasy.domain.PayableInstallmentStatus;
import com.fasteasy.domain.Supplier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de resumo de uma conta a pagar para listagem.
 */
public record PayableAccountResumo(
        Long id,
        String descricao,
        String fornecedorNome,
        Long fornecedorId,
        String categoriaNome,
        Long categoriaId,
        LocalDate dataEmissao,
        LocalDate dataCompetencia,
        LocalDate proximoVencimento,
        BigDecimal valorOriginal,
        BigDecimal valorPago,
        BigDecimal saldoPendente,
        PayableAccountStatus status,
        int quantidadeParcelas,
        int parcelasPagas,
        String observacoes
) {
    public boolean isVencida() {
        return proximoVencimento != null && proximoVencimento.isBefore(LocalDate.now()) &&
                status != PayableAccountStatus.PAID && status != PayableAccountStatus.CANCELLED;
    }

    public boolean isProximaVencimento(int dias) {
        if (proximoVencimento == null) return false;
        LocalDate limite = LocalDate.now().plusDays(dias);
        return !proximoVencimento.isAfter(limite) &&
                status != PayableAccountStatus.PAID && status != PayableAccountStatus.CANCELLED;
    }
}