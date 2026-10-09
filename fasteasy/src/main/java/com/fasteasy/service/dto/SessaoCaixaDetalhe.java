package com.fasteasy.service.dto;

import java.util.List;

/**
 * Detalhe completo de uma sessão de caixa: resumo financeiro + movimentações.
 * Usado na tela de fechamento, no relatório e no histórico.
 */
public record SessaoCaixaDetalhe(
        ResumoCaixaDTO resumo,
        java.util.List<MovimentacaoCaixaDTO> movimentacoes) {
}
