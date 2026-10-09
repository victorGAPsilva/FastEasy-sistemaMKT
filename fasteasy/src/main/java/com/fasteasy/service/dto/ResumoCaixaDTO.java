package com.fasteasy.service.dto;

import com.fasteasy.domain.Dinheiro;
import com.fasteasy.domain.FormaPagamento;
import com.fasteasy.domain.StatusSessaoCaixa;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Resumo financeiro de uma sessão de caixa.
 *
 * <p>Reúne o saldo inicial, os totais de dinheiro efetivamente recebido e de
 * troco, suprimentos, sangrias e demais movimentações, além dos totais por
 * forma de pagamento e do saldo esperado em dinheiro. Os valores são
 * calculados a partir das vendas/pagamentos e das movimentações — não são
 * armazenados de forma redundante.</p>
 */
public record ResumoCaixaDTO(
        Long sessaoId,
        Long caixaId,
        String caixaNome,
        String operador,
        String operadorFechamento,
        StatusSessaoCaixa status,
        LocalDateTime dataAbertura,
        LocalDateTime dataFechamento,
        BigDecimal saldoInicial,
        BigDecimal totalDinheiroRecebido,
        BigDecimal totalTroco,
        BigDecimal totalSuprimentos,
        BigDecimal totalOutrasEntradas,
        BigDecimal totalSangrias,
        BigDecimal totalOutrasSaidas,
        BigDecimal saldoEsperado,
        BigDecimal totalContado,
        BigDecimal diferenca,
        long quantidadeVendas,
        BigDecimal faturamentoTotal,
        Map<FormaPagamento, BigDecimal> totalPorForma,
        String observacoesAbertura,
        String observacoesFechamento) {

    /** Total aplicado em uma forma de pagamento (zero quando não houver). */
    public BigDecimal totalPorForma(FormaPagamento forma) {
        if (totalPorForma == null || forma == null) {
            return BigDecimal.ZERO;
        }
        return Dinheiro.ouZero(totalPorForma.get(forma));
    }

    public boolean isAberta() {
        return status == StatusSessaoCaixa.OPEN;
    }
}
