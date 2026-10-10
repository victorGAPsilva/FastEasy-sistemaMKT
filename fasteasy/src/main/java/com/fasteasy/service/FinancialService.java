package com.fasteasy.service;

import com.fasteasy.domain.FinancialPayment;
import com.fasteasy.domain.FinancialPaymentMethod;
import com.fasteasy.domain.PayableAccount;
import com.fasteasy.domain.PayableAccountStatus;
import com.fasteasy.domain.PayableInstallment;
import com.fasteasy.domain.PayableInstallmentStatus;
import com.fasteasy.repository.FinancialPaymentRepository;
import com.fasteasy.repository.PayableAccountRepository;
import com.fasteasy.repository.PayableInstallmentRepository;
import com.fasteasy.service.dto.FluxoCaixaDiarioDTO;
import com.fasteasy.service.dto.FluxoCaixaPrevistoDTO;
import com.fasteasy.service.dto.MetodoPagamentoTotalDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço para relatórios financeiros e fluxo de caixa.
 *
 * <p>Consolida consultas de fluxo previsto (obrigações futuras) e realizado
 * (pagamentos efetivados).</p>
 */
@Service
@Transactional(readOnly = true)
public class FinancialService {

    private final FinancialPaymentRepository financialPaymentRepository;
    private final PayableAccountRepository payableAccountRepository;
    private final PayableInstallmentRepository installmentRepository;

    public FinancialService(FinancialPaymentRepository financialPaymentRepository,
                            PayableAccountRepository payableAccountRepository,
                            PayableInstallmentRepository installmentRepository) {
        this.financialPaymentRepository = financialPaymentRepository;
        this.payableAccountRepository = payableAccountRepository;
        this.installmentRepository = installmentRepository;
    }

    // ------------------------------------------------------------------
    // Fluxo de Caixa Realizado (saídas registradas)
    // ------------------------------------------------------------------

    /**
     * Fluxo de caixa realizado por dia no período.
     * Inclui apenas pagamentos com status CONFIRMED.
     */
    public List<FluxoCaixaDiarioDTO> fluxoRealizadoPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        List<Object[]> raw = financialPaymentRepository.fluxoRealizadoPorDiaRaw(inicio, fim);
        return raw.stream()
                .map(row -> new FluxoCaixaDiarioDTO(
                        row[0] != null ? ((java.sql.Date) row[0]).toLocalDate() : null,
                        row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO))
                .collect(Collectors.toList());
    }

    /**
     * Totais por método de pagamento no período.
     */
    public List<MetodoPagamentoTotalDTO> totaisPorMetodo(LocalDateTime inicio, LocalDateTime fim) {
        List<Object[]> raw = financialPaymentRepository.totaisPorMetodoRaw(inicio, fim);
        return raw.stream()
                .map(row -> new MetodoPagamentoTotalDTO(
                        row[0] != null ? (FinancialPaymentMethod) row[0] : FinancialPaymentMethod.OUTRO,
                        row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO))
                .collect(Collectors.toList());
    }

    /**
     * Total pago no período.
     */
    public BigDecimal totalPagoNoPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return financialPaymentRepository.totaisPorMetodoRaw(inicio, fim).stream()
                .map(row -> row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ------------------------------------------------------------------
    // Fluxo de Caixa Previsto (obrigações futuras)
    // ------------------------------------------------------------------

    /**
     * Fluxo de caixa previsto para os próximos dias.
     * Agrupa obrigações (parcelas em aberto) por data de vencimento.
     */
    public FluxoCaixaPrevistoDTO fluxoPrevisto(int dias) {
        LocalDate hoje = LocalDate.now();
        LocalDate fim = hoje.plusDays(dias);

        List<PayableInstallmentStatus> statuses = List.of(
                PayableInstallmentStatus.OPEN,
                PayableInstallmentStatus.PARTIALLY_PAID
        );

        List<PayableInstallment> parcelas = installmentRepository.buscarPorPeriodo(statuses, hoje, fim);

        // Filtrar parcelas com saldo pendente
        List<PayableInstallment> parcelasComSaldo = parcelas.stream()
                .filter(p -> p.getSaldoPendente().signum() > 0)
                .collect(Collectors.toList());

        // Calcular totais
        BigDecimal totalPrevisto = parcelasComSaldo.stream()
                .map(PayableInstallment::getSaldoPendente)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalVencidas = parcelasComSaldo.stream()
                .filter(p -> p.getDataVencimento().isBefore(hoje))
                .map(PayableInstallment::getSaldoPendente)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAVencer = parcelasComSaldo.stream()
                .filter(p -> !p.getDataVencimento().isBefore(hoje))
                .map(PayableInstallment::getSaldoPendente)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<FluxoCaixaPrevistoDTO.ObrigacaoPrevistaDTO> obrigacoes = parcelasComSaldo.stream()
                .map(p -> new FluxoCaixaPrevistoDTO.ObrigacaoPrevistaDTO(
                        p.getConta().getId(),
                        p.getConta().getDescricao(),
                        p.getConta().getFornecedor() != null ? p.getConta().getFornecedor().getNomeRazaoSocial() : null,
                        p.getConta().getCategoria() != null ? p.getConta().getCategoria().getNome() : null,
                        p.getDataVencimento(),
                        p.getSaldoPendente(),
                        p.getConta().getStatus(),
                        p.getStatus()))
                .collect(Collectors.toList());

        return new FluxoCaixaPrevistoDTO(hoje, totalPrevisto, totalVencidas, totalAVencer, obrigacoes);
    }

    /**
     * Obrigações vencidas (previstas mas não pagas).
     */
    public List<FluxoCaixaPrevistoDTO.ObrigacaoPrevistaDTO> obrigacoesVencidas() {
        List<PayableInstallmentStatus> statuses = List.of(
                PayableInstallmentStatus.OPEN,
                PayableInstallmentStatus.PARTIALLY_PAID
        );
        List<PayableInstallment> parcelas = installmentRepository.buscarVencidasAte(statuses, LocalDate.now());
        return parcelas.stream()
                .filter(p -> p.getSaldoPendente().signum() > 0)
                .map(p -> new FluxoCaixaPrevistoDTO.ObrigacaoPrevistaDTO(
                        p.getConta().getId(),
                        p.getConta().getDescricao(),
                        p.getConta().getFornecedor() != null ? p.getConta().getFornecedor().getNomeRazaoSocial() : null,
                        p.getConta().getCategoria() != null ? p.getConta().getCategoria().getNome() : null,
                        p.getDataVencimento(),
                        p.getSaldoPendente(),
                        p.getConta().getStatus(),
                        p.getStatus()))
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // Consultas de apoio
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<PayableInstallment> buscarParcelasAVencer(int dias) {
        List<PayableInstallmentStatus> statuses = List.of(
                PayableInstallmentStatus.OPEN,
                PayableInstallmentStatus.PARTIALLY_PAID
        );
        LocalDate fim = LocalDate.now().plusDays(dias);
        return installmentRepository.buscarPorPeriodo(statuses, LocalDate.now(), fim);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalPagoPorMetodoNoPeriodo(FinancialPaymentMethod metodo, LocalDateTime inicio, LocalDateTime fim) {
        return financialPaymentRepository.totaisPorMetodoRaw(inicio, fim).stream()
                .filter(row -> row[0] != null && row[0].equals(metodo))
                .map(row -> row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }
}