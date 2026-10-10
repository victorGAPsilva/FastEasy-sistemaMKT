package com.fasteasy.service.dto;

import com.fasteasy.domain.ExpenseCategory;
import com.fasteasy.domain.PayableAccountStatus;
import com.fasteasy.domain.PayableInstallmentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO para fluxo de caixa previsto (obrigações futuras).
 */
public record FluxoCaixaPrevistoDTO(
        LocalDate data,
        BigDecimal totalPrevisto,
        BigDecimal totalVencidas,
        BigDecimal totalAVencer,
        List<ObrigacaoPrevistaDTO> obrigacoes
) {

    public record ObrigacaoPrevistaDTO(
            Long contaId,
            String descricao,
            String fornecedorNome,
            String categoriaNome,
            LocalDate dataVencimento,
            BigDecimal valor,
            PayableAccountStatus statusConta,
            PayableInstallmentStatus statusParcela
    ) {
    }
}