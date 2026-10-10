package com.fasteasy.service.dto;

import java.math.BigDecimal;

/**
 * DTO para saldo pendente por categoria de despesa.
 */
public record CategoriaSaldoDTO(
        Long categoriaId,
        String categoriaNome,
        BigDecimal saldoPendente
) {
}