package com.fasteasy.service.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de resumo financeiro para dashboard.
 */
public record ResumoFinanceiroDTO(
        BigDecimal totalEmAberto,
        BigDecimal totalVencidas,
        BigDecimal totalAVencer7Dias,
        BigDecimal totalAVencer30Dias,
        BigDecimal totalPagoPeriodo,
        BigDecimal totalPendenteFornecedores,
        BigDecimal totalPendenteCategorias,
        long quantidadeContasAberto,
        long quantidadeContasVencidas,
        List<FornecedorSaldoDTO> topFornecedores,
        List<CategoriaSaldoDTO> topCategorias
) {
}