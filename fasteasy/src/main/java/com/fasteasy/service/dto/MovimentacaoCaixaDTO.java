package com.fasteasy.service.dto;

import com.fasteasy.domain.TipoMovimentacaoCaixa;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de leitura de uma movimentação de caixa (suprimento, sangria ou ajuste).
 */
public record MovimentacaoCaixaDTO(
        Long id,
        TipoMovimentacaoCaixa tipo,
        BigDecimal valor,
        String motivo,
        LocalDateTime dataHora,
        String operador) {

    public boolean isEntrada() {
        return tipo != null && tipo.isEntrada();
    }
}
