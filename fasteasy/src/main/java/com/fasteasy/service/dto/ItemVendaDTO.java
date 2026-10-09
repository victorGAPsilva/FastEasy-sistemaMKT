package com.fasteasy.service.dto;

import java.math.BigDecimal;

/**
 * Dados históricos de um item de venda, para comprovante e consulta.
 */
public record ItemVendaDTO(String nomeProduto,
                           String codigoProduto,
                           BigDecimal quantidade,
                           BigDecimal precoUnitario,
                           BigDecimal desconto,
                           BigDecimal total) {
}
