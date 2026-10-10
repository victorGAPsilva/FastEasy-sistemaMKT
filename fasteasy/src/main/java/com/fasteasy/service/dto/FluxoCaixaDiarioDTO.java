package com.fasteasy.service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para fluxo de caixa realizado por dia.
 */
public record FluxoCaixaDiarioDTO(
        LocalDate data,
        BigDecimal totalSaidas
) {
}