package com.fasteasy.service.dto;

import java.math.BigDecimal;

/**
 * DTO para saldo pendente por fornecedor.
 */
public record FornecedorSaldoDTO(
        Long fornecedorId,
        String fornecedorNome,
        BigDecimal saldoPendente
) {
}