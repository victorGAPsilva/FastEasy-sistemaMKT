package com.fasteasy.service.dto;

import com.fasteasy.domain.FinancialPaymentMethod;
import java.math.BigDecimal;

/**
 * DTO para total de pagamentos por método.
 */
public record MetodoPagamentoTotalDTO(
        FinancialPaymentMethod metodo,
        BigDecimal total
) {
}