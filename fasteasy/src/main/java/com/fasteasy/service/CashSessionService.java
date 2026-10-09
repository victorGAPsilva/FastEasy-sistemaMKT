package com.fasteasy.service;

import com.fasteasy.domain.CaixaInvalidoException;
import com.fasteasy.domain.CashMovement;
import com.fasteasy.domain.CashRegister;
import com.fasteasy.domain.CashSession;
import com.fasteasy.domain.Dinheiro;
import com.fasteasy.domain.FormaPagamento;
import com.fasteasy.domain.Sale;
import com.fasteasy.domain.SalePayment;
import com.fasteasy.domain.StatusSessaoCaixa;
import com.fasteasy.domain.StatusVenda;
import com.fasteasy.domain.TipoMovimentacaoCaixa;
import com.fasteasy.repository.CashMovementRepository;
import com.fasteasy.repository.CashRegisterRepository;
import com.fasteasy.repository.CashSessionRepository;
import com.fasteasy.repository.SaleRepository;
import com.fasteasy.service.dto.MovimentacaoCaixaDTO;
import com.fasteasy.service.dto.ResumoCaixaDTO;
import com.fasteasy.service.dto.SessaoCaixaDetalhe;
import com.fasteasy.service.dto.SessaoCaixaResumo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Serviço de aplicação responsável pela gestão financeira do caixa.
 *
 * <p>Concentra a abertura e o fechamento da sessão, o registro de
 * movimentações (suprimento, sangria e ajustes) e o cálculo do saldo esperado
 * em dinheiro. Todo o fluxo é transacional; as operações concorrentes na mesma
 * sessão são serializadas por bloqueio pessimista de escrita.</p>
 *
 * <p>O saldo esperado segue a regra:</p>
 * <pre>
 * saldo esperado = saldo inicial
 *                + pagamentos de vendas recebidos em dinheiro
 *                - trocos devolvidos
 *                + suprimentos e outras entradas
 *                - sangrias e outras saídas
 * </pre>
 *
 * <p>Formas de pagamento eletrônicas (PIX, débito e crédito) são totalizadas
 * separadamente e não aumentam o dinheiro físico disponível.</p>
 */
@Service
@Transactional
public class CashSessionService {

    private final CashSessionRepository cashSessionRepository;
    private final CashRegisterRepository cashRegisterRepository;
    private final CashMovementRepository cashMovementRepository;
    private final SaleRepository saleRepository;

    public CashSessionService(CashSessionRepository cashSessionRepository,
                              CashRegisterRepository cashRegisterRepository,
                              CashMovementRepository cashMovementRepository,
                              SaleRepository saleRepository) {
        this.cashSessionRepository = cashSessionRepository;
        this.cashRegisterRepository = cashRegisterRepository;
        this.cashMovementRepository = cashMovementRepository;
        this.saleRepository = saleRepository;
    }

    // ------------------------------------------------------------------
    // Abertura
    // ------------------------------------------------------------------

    /**
     * Abre uma nova sessão de caixa com o saldo inicial informado.
     *
     * <p>O cadastro do caixa é bloqueado para escrita, garantindo que duas
     * aberturas simultâneas no mesmo caixa não criem duas sessões abertas.</p>
     */
    public CashSession abrirSessao(Long caixaId, BigDecimal saldoInicial, String observacoes, String operador) {
        if (caixaId == null) {
            throw new CaixaInvalidoException("Selecione o caixa a abrir");
        }
        BigDecimal saldo = Dinheiro.arredondar(Dinheiro.ouZero(saldoInicial));
        if (saldo.signum() < 0) {
            throw new CaixaInvalidoException("O saldo inicial não pode ser negativo");
        }

        CashRegister caixa = cashRegisterRepository.buscarParaAtualizacao(caixaId)
                .orElseThrow(() -> new CaixaInvalidoException("Caixa não encontrado: " + caixaId));
        if (!caixa.isAtivo()) {
            throw new CaixaInvalidoException("O caixa \"" + caixa.getNome() + "\" está inativo");
        }
        if (cashSessionRepository.existsByCaixaIdAndStatus(caixaId, StatusSessaoCaixa.OPEN)) {
            throw new CaixaInvalidoException(
                    "O caixa \"" + caixa.getNome() + "\" já possui uma sessão aberta. Feche-a antes de abrir outra.");
        }

        CashSession sessao = new CashSession();
        sessao.setCaixa(caixa);
        sessao.setSaldoInicial(saldo);
        sessao.setObservacoesAbertura(normalizar(observacoes));
        sessao.setOperador(normalizar(operador));
        sessao.setStatus(StatusSessaoCaixa.OPEN);
        sessao.setDataAbertura(LocalDateTime.now());
        return cashSessionRepository.saveAndFlush(sessao);
    }

    // ------------------------------------------------------------------
    // Movimentações
    // ------------------------------------------------------------------

    /** Registra um suprimento (entrada de dinheiro no caixa). */
    public CashMovement registrarSuprimento(Long caixaId, BigDecimal valor, String motivo, String operador) {
        return registrarMovimentacao(caixaId, TipoMovimentacaoCaixa.SUPPLY, valor, motivo, operador);
    }

    /** Registra uma sangria (retirada de dinheiro do caixa). */
    public CashMovement registrarSangria(Long caixaId, BigDecimal valor, String motivo, String operador) {
        return registrarMovimentacao(caixaId, TipoMovimentacaoCaixa.WITHDRAWAL, valor, motivo, operador);
    }

    /**
     * Registra uma movimentação financeira manual.
     *
     * <p>Exige sessão aberta e valor maior que zero. Para retiradas
     * ({@link TipoMovimentacaoCaixa#WITHDRAWAL} e
     * {@link TipoMovimentacaoCaixa#OTHER_OUT}), garante de forma atômica que o
     * saldo esperado em dinheiro não fique negativo.</p>
     */
    public CashMovement registrarMovimentacao(Long caixaId,
                                              TipoMovimentacaoCaixa tipo,
                                              BigDecimal valor,
                                              String motivo,
                                              String operador) {
        if (tipo == null) {
            throw new CaixaInvalidoException("O tipo da movimentação é obrigatório");
        }
        BigDecimal v = Dinheiro.arredondar(valor);
        if (v == null || v.signum() <= 0) {
            throw new CaixaInvalidoException("O valor da movimentação deve ser maior que zero");
        }
        String m = normalizar(motivo);
        if (m == null) {
            throw new CaixaInvalidoException("O motivo da movimentação é obrigatório");
        }
        if (caixaId == null) {
            throw new CaixaInvalidoException("Selecione o caixa");
        }

        CashSession sessao = cashSessionRepository
                .buscarAbertaParaAtualizacao(caixaId, StatusSessaoCaixa.OPEN)
                .orElseThrow(() -> new CaixaInvalidoException("Não há caixa aberto para registrar a movimentação"));

        if (!tipo.isEntrada()) {
            BigDecimal disponivel = montarResumo(sessao).saldoEsperado();
            if (v.compareTo(disponivel) > 0) {
                throw new CaixaInvalidoException("A retirada de " + Dinheiro.formatar(v)
                        + " excede o saldo disponível em dinheiro (" + Dinheiro.formatar(disponivel) + ")");
            }
        }

        CashMovement movimento = new CashMovement();
        movimento.setSessao(sessao);
        movimento.setTipo(tipo);
        movimento.setValor(v);
        movimento.setMotivo(normalizar(motivo));
        movimento.setOperador(normalizar(operador));
        movimento.setDataHora(LocalDateTime.now());
        return cashMovementRepository.saveAndFlush(movimento);
    }

    // ------------------------------------------------------------------
    // Fechamento
    // ------------------------------------------------------------------

    /**
     * Fecha a sessão aberta do caixa informado.
     *
     * <p>Recalcula o saldo esperado, registra o total contado e a diferença e
     * marca a sessão como {@link StatusSessaoCaixa#CLOSED}. A sessão é
     * bloqueada para escrita, impedindo fechamento duplicado ou concorrente e
     * novas vendas após o fechamento.</p>
     */
    public ResumoCaixaDTO fecharSessao(Long caixaId, BigDecimal totalContado, String observacoes, String operador) {
        if (caixaId == null) {
            throw new CaixaInvalidoException("Selecione o caixa a fechar");
        }
        BigDecimal contado = Dinheiro.arredondar(totalContado);
        if (contado == null || contado.signum() < 0) {
            throw new CaixaInvalidoException("O total contado deve ser um valor não negativo");
        }

        CashSession sessao = cashSessionRepository.buscarAbertaParaAtualizacao(caixaId, StatusSessaoCaixa.OPEN)
                .orElseThrow(() -> new CaixaInvalidoException("Não há sessão aberta para o caixa informado"));

        ResumoCaixaDTO resumo = montarResumo(sessao);
        BigDecimal esperado = resumo.saldoEsperado();

        sessao.setSaldoEsperado(esperado);
        sessao.setTotalContado(contado);
        sessao.setDiferenca(Dinheiro.arredondar(contado.subtract(esperado)));
        sessao.setStatus(StatusSessaoCaixa.CLOSED);
        sessao.setDataFechamento(LocalDateTime.now());
        sessao.setObservacoesFechamento(normalizar(observacoes));
        sessao.setOperadorFechamento(normalizar(operador));

        CashSession salva = cashSessionRepository.saveAndFlush(sessao);
        return montarResumo(salva);
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Optional<CashSession> buscarSessaoAberta(Long caixaId) {
        if (caixaId == null) {
            return Optional.empty();
        }
        return cashSessionRepository.findFirstByCaixaIdAndStatusOrderByDataAberturaDesc(caixaId, StatusSessaoCaixa.OPEN);
    }

    @Transactional(readOnly = true)
    public Optional<CashSession> buscarPorId(Long sessaoId) {
        return cashSessionRepository.findById(sessaoId);
    }

    /** Resumo financeiro de uma sessão específica. */
    @Transactional(readOnly = true)
    public ResumoCaixaDTO resumo(Long sessaoId) {
        CashSession sessao = cashSessionRepository.findById(sessaoId)
                .orElseThrow(() -> new CaixaInvalidoException("Sessão de caixa não encontrada: " + sessaoId));
        return montarResumo(sessao);
    }

    /** Resumo financeiro da sessão aberta de um caixa, se houver. */
    @Transactional(readOnly = true)
    public Optional<ResumoCaixaDTO> resumoDaSessaoAberta(Long caixaId) {
        return buscarSessaoAberta(caixaId).map(this::montarResumo);
    }

    /** Detalhe completo (resumo + movimentações) de uma sessão. */
    @Transactional(readOnly = true)
    public SessaoCaixaDetalhe detalhe(Long sessaoId) {
        CashSession sessao = cashSessionRepository.findById(sessaoId)
                .orElseThrow(() -> new CaixaInvalidoException("Sessão de caixa não encontrada: " + sessaoId));
        return new SessaoCaixaDetalhe(montarResumo(sessao), listarMovimentacoes(sessaoId, null));
    }

    /** Lista as movimentações de uma sessão, opcionalmente filtrando por tipo. */
    @Transactional(readOnly = true)
    public List<MovimentacaoCaixaDTO> listarMovimentacoes(Long sessaoId, TipoMovimentacaoCaixa tipo) {
        List<CashMovement> movimentos = tipo == null
                ? cashMovementRepository.findBySessaoIdOrderByDataHoraAsc(sessaoId)
                : cashMovementRepository.findBySessaoIdAndTipoOrderByDataHoraAsc(sessaoId, tipo);
        return movimentos.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /** Histórico de sessões com filtros opcionais. */
    @Transactional(readOnly = true)
    public List<SessaoCaixaResumo> listarHistorico(Long caixaId,
                                                   LocalDateTime inicio,
                                                   LocalDateTime fim,
                                                   StatusSessaoCaixa status) {
        return cashSessionRepository.buscarHistorico(caixaId, inicio, fim, status).stream()
                .map(this::toResumo)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // Cálculo do resumo
    // ------------------------------------------------------------------

    private ResumoCaixaDTO montarResumo(CashSession sessao) {
        List<CashMovement> movimentos = cashMovementRepository.findBySessaoIdOrderByDataHoraAsc(sessao.getId());
        List<Sale> vendas = sessao.getId() == null
                ? List.of()
                : saleRepository.listarPorSessaoComPagamentos(sessao.getId(), StatusVenda.COMPLETED);

        BigDecimal dinheiroRecebido = BigDecimal.ZERO;
        BigDecimal troco = BigDecimal.ZERO;
        BigDecimal faturamento = BigDecimal.ZERO;
        Map<FormaPagamento, BigDecimal> porForma = new EnumMap<>(FormaPagamento.class);
        for (FormaPagamento forma : FormaPagamento.values()) {
            porForma.put(forma, BigDecimal.ZERO);
        }

        for (Sale venda : vendas) {
            for (SalePayment pagamento : venda.getPagamentos()) {
                BigDecimal valor = Dinheiro.ouZero(pagamento.getValor());
                porForma.merge(pagamento.getFormaPagamento(), valor, BigDecimal::add);
                faturamento = faturamento.add(valor);

                if (pagamento.getFormaPagamento() == FormaPagamento.DINHEIRO) {
                    BigDecimal recebido = Dinheiro.ouZero(pagamento.getValorRecebido());
                    dinheiroRecebido = dinheiroRecebido.add(recebido);
                    // Troco = valor entregue - valor aplicado (nunca negativo).
                    BigDecimal t = recebido.subtract(valor);
                    if (t.signum() > 0) {
                        troco = troco.add(t);
                    }
                }
            }
        }

        BigDecimal suprimentos = BigDecimal.ZERO;
        BigDecimal outrasEntradas = BigDecimal.ZERO;
        BigDecimal sangrias = BigDecimal.ZERO;
        BigDecimal outrasSaidas = BigDecimal.ZERO;
        for (CashMovement movimento : movimentos) {
            BigDecimal valor = Dinheiro.ouZero(movimento.getValor());
            switch (movimento.getTipo()) {
                case SUPPLY -> suprimentos = suprimentos.add(valor);
                case OTHER_IN -> outrasEntradas = outrasEntradas.add(valor);
                case WITHDRAWAL -> sangrias = sangrias.add(valor);
                case OTHER_OUT -> outrasSaidas = outrasSaidas.add(valor);
            }
        }

        BigDecimal saldoInicial = Dinheiro.ouZero(sessao.getSaldoInicial());
        BigDecimal esperado = saldoInicial
                .add(dinheiroRecebido)
                .subtract(troco)
                .add(suprimentos)
                .add(outrasEntradas)
                .subtract(sangrias)
                .subtract(outrasSaidas);
        esperado = Dinheiro.arredondar(esperado);

        BigDecimal contado = sessao.getTotalContado() == null ? null : Dinheiro.arredondar(sessao.getTotalContado());
        BigDecimal diferenca = contado == null ? null : Dinheiro.arredondar(contado.subtract(esperado));

        return new ResumoCaixaDTO(
                sessao.getId(),
                sessao.getCaixa() == null ? null : sessao.getCaixa().getId(),
                sessao.getCaixa() == null ? null : sessao.getCaixa().getNome(),
                sessao.getOperador(),
                sessao.getOperadorFechamento(),
                sessao.getStatus(),
                sessao.getDataAbertura(),
                sessao.getDataFechamento(),
                Dinheiro.arredondar(saldoInicial),
                Dinheiro.arredondar(dinheiroRecebido),
                Dinheiro.arredondar(troco),
                Dinheiro.arredondar(suprimentos),
                Dinheiro.arredondar(outrasEntradas),
                Dinheiro.arredondar(sangrias),
                Dinheiro.arredondar(outrasSaidas),
                esperado,
                contado,
                diferenca,
                vendas.size(),
                Dinheiro.arredondar(faturamento),
                porForma,
                sessao.getObservacoesAbertura(),
                sessao.getObservacoesFechamento());
    }

    private SessaoCaixaResumo toResumo(CashSession sessao) {
        return new SessaoCaixaResumo(
                sessao.getId(),
                sessao.getCaixa() == null ? null : sessao.getCaixa().getNome(),
                sessao.getOperador(),
                sessao.getDataAbertura(),
                sessao.getDataFechamento(),
                Dinheiro.arredondar(sessao.getSaldoInicial()),
                sessao.getSaldoEsperado(),
                sessao.getTotalContado(),
                sessao.getDiferenca(),
                sessao.getStatus());
    }

    private MovimentacaoCaixaDTO toDTO(CashMovement movimento) {
        return new MovimentacaoCaixaDTO(
                movimento.getId(),
                movimento.getTipo(),
                Dinheiro.arredondar(movimento.getValor()),
                movimento.getMotivo(),
                movimento.getDataHora(),
                movimento.getOperador());
    }

    private String normalizar(String texto) {
        if (texto == null) {
            return null;
        }
        String t = texto.trim();
        return t.isEmpty() ? null : t;
    }
}
