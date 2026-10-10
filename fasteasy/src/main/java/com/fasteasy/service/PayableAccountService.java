package com.fasteasy.service;

import com.fasteasy.domain.Dinheiro;
import com.fasteasy.domain.ExpenseCategory;
import com.fasteasy.domain.FinanceiroInvalidoException;
import com.fasteasy.domain.PayableAccount;
import com.fasteasy.domain.PayableAccountStatus;
import com.fasteasy.domain.PayableInstallment;
import com.fasteasy.domain.PayableInstallmentStatus;
import com.fasteasy.domain.Purchase;
import com.fasteasy.domain.Supplier;
import com.fasteasy.repository.ExpenseCategoryRepository;
import com.fasteasy.repository.FinancialPaymentRepository;
import com.fasteasy.repository.PayableAccountRepository;
import com.fasteasy.repository.PayableInstallmentRepository;
import com.fasteasy.repository.PurchaseRepository;
import com.fasteasy.repository.SupplierRepository;
import com.fasteasy.service.dto.PayableAccountDetalhe;
import com.fasteasy.service.dto.PayableAccountResumo;
import com.fasteasy.service.dto.PayableInstallmentResumo;
import com.fasteasy.service.dto.FinancialPaymentResumo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Serviço de aplicação para gestão de contas a pagar.
 *
 * <p>Centraliza o cadastro, parcelamento, pagamento, cancelamento
 * e consulta de obrigações financeiras.</p>
 */
@Service
@Transactional
public class PayableAccountService {

    private final PayableAccountRepository payableAccountRepository;
    private final PayableInstallmentRepository installmentRepository;
    private final FinancialPaymentRepository financialPaymentRepository;
    private final SupplierRepository supplierRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final PurchaseRepository purchaseRepository;

    public PayableAccountService(PayableAccountRepository payableAccountRepository,
                                  PayableInstallmentRepository installmentRepository,
                                  FinancialPaymentRepository financialPaymentRepository,
                                  SupplierRepository supplierRepository,
                                  ExpenseCategoryRepository expenseCategoryRepository,
                                  PurchaseRepository purchaseRepository) {
        this.payableAccountRepository = payableAccountRepository;
        this.installmentRepository = installmentRepository;
        this.financialPaymentRepository = financialPaymentRepository;
        this.supplierRepository = supplierRepository;
        this.expenseCategoryRepository = expenseCategoryRepository;
        this.purchaseRepository = purchaseRepository;
    }

    // ------------------------------------------------------------------
    // Cadastro manual de conta a pagar
    // ------------------------------------------------------------------

    /**
     * Cria uma nova conta a pagar manualmente.
     *
     * @param conta       dados da conta (descrição, fornecedor opcional, categoria, valor, datas, parcelas)
     * @param parcelas    lista de parcelas (número, vencimento, valor)
     * @param operador    responsável pelo registro
     * @return a conta criada com as parcelas
     */
    public PayableAccount cadastrar(PayableAccount conta,
                                    List<ParcelaInput> parcelas,
                                    String operador) {
        validarConta(conta, parcelas);

        // Valida e busca referências
        Supplier fornecedor = null;
        if (conta.getFornecedor() != null && conta.getFornecedor().getId() != null) {
            fornecedor = supplierRepository.findById(conta.getFornecedor().getId())
                    .orElseThrow(() -> new FinanceiroInvalidoException("Fornecedor não encontrado: " + conta.getFornecedor().getId()));
        }

        ExpenseCategory categoria = expenseCategoryRepository.findById(conta.getCategoria().getId())
                .orElseThrow(() -> new FinanceiroInvalidoException("Categoria de despesa não encontrada: " + conta.getCategoria().getId()));

        // Se há compra de origem, verifica idempotência
        if (conta.getCompraOrigem() != null && conta.getCompraOrigem().getId() != null) {
            if (payableAccountRepository.findByCompraOrigemId(conta.getCompraOrigem().getId()).isPresent()) {
                throw new FinanceiroInvalidoException("Já existe uma conta a pagar vinculada a esta compra");
            }
            Purchase compra = purchaseRepository.findById(conta.getCompraOrigem().getId())
                    .orElseThrow(() -> new FinanceiroInvalidoException("Compra não encontrada: " + conta.getCompraOrigem().getId()));
            conta.setCompraOrigem(compra);
        }

        // Preenche a conta
        conta.setId(null);
        conta.setDescricao(conta.getDescricao().trim());
        conta.setFornecedor(fornecedor);
        conta.setCategoria(categoria);
        conta.setValorOriginal(Dinheiro.arredondar(conta.getValorOriginal()));
        conta.setValorPago(BigDecimal.ZERO);
        conta.setSaldoPendente(conta.getValorOriginal());
        conta.setStatus(PayableAccountStatus.OPEN);
        conta.setDataCriacao(LocalDateTime.now());
        conta.setObservacoes(normalizar(conta.getObservacoes()));

        // Cria as parcelas
        List<PayableInstallment> parcelasEntidades = criarParcelas(conta, parcelas);
        conta.getParcelas().addAll(parcelasEntidades);

        PayableAccount salva = payableAccountRepository.saveAndFlush(conta);
        return salva;
    }

    /**
     * Gera conta a pagar a partir de uma compra elegível (idempotente).
     *
     * @param compraId    ID da compra
     * @param categoriaId ID da categoria financeira
     * @param parcelas    parcelas (número, vencimento, valor) - se vazio, cria parcela única
     * @param operador    responsável
     * @return conta criada
     */
    public PayableAccount gerarDaCompra(Long compraId, Long categoriaId, List<ParcelaInput> parcelas, String operador) {
        Purchase compra = purchaseRepository.findById(compraId)
                .orElseThrow(() -> new FinanceiroInvalidoException("Compra não encontrada: " + compraId));

        // Idempotência: verifica se já existe conta para esta compra
        Optional<PayableAccount> existente = payableAccountRepository.findByCompraOrigemId(compraId);
        if (existente.isPresent()) {
            throw new FinanceiroInvalidoException("Já existe uma conta a pagar vinculada a esta compra");
        }

        ExpenseCategory categoria = expenseCategoryRepository.findById(categoriaId)
                .orElseThrow(() -> new FinanceiroInvalidoException("Categoria de despesa não encontrada: " + categoriaId));

        // Se não houver parcelas informadas, cria uma única parcela com vencimento na data da compra + 30 dias
        List<ParcelaInput> parcelasFinais = parcelas;
        if (parcelasFinais == null || parcelasFinais.isEmpty()) {
            ParcelaInput p = new ParcelaInput();
            p.setNumeroParcela(1);
            p.setDataVencimento(compra.getDataEmissao().toLocalDate().plusDays(30));
            p.setValor(compra.getValorTotal());
            parcelasFinais = List.of(p);
        }

        validarParcelas(parcelasFinais, compra.getValorTotal());

        PayableAccount conta = new PayableAccount();
        conta.setDescricao("Compra " + compra.getNumero() + " - " + compra.getFornecedor().getNomeRazaoSocial());
        conta.setFornecedor(compra.getFornecedor());
        conta.setCompraOrigem(compra);
        conta.setCategoria(categoria);
        conta.setDataEmissao(compra.getDataEmissao().toLocalDate());
        conta.setDataCompetencia(compra.getDataEmissao().toLocalDate());
        conta.setValorOriginal(Dinheiro.arredondar(compra.getValorTotal()));
        conta.setValorPago(BigDecimal.ZERO);
        conta.setSaldoPendente(conta.getValorOriginal());
        conta.setStatus(PayableAccountStatus.OPEN);
        conta.setDataCriacao(LocalDateTime.now());
        conta.setObservacoes("Gerada automaticamente da compra " + compra.getNumero());

        List<PayableInstallment> parcelasEntidades = criarParcelas(conta, parcelasFinais);
        conta.getParcelas().addAll(parcelasEntidades);

        return payableAccountRepository.saveAndFlush(conta);
    }

    // ------------------------------------------------------------------
    // Pagamentos
    // ------------------------------------------------------------------

    /**
     * Registra um pagamento em uma conta (pode ser total ou parcial).
     * Se a conta tiver parcelas em aberto, o pagamento é aplicado na primeira parcela aberta (FIFO).
     *
     * @param contaId       ID da conta
     * @param parcelaId     ID da parcela específica (opcional; se nulo, aplica na primeira em aberto)
     * @param valor         valor do pagamento
     * @param metodo        método de pagamento
     * @param referencia    referência opcional (número do boleto, comprovante, etc.)
     * @param observacoes   observações
     * @param operador      responsável
     * @return o pagamento registrado
     */
    public com.fasteasy.domain.FinancialPayment registrarPagamento(Long contaId,
                                                                   Long parcelaId,
                                                                   BigDecimal valor,
                                                                   com.fasteasy.domain.FinancialPaymentMethod metodo,
                                                                   String referencia,
                                                                   String observacoes,
                                                                   String operador) {
        BigDecimal v = Dinheiro.arredondar(valor);
        if (v == null || v.signum() <= 0) {
            throw new FinanceiroInvalidoException("O valor do pagamento deve ser maior que zero");
        }
        if (metodo == null) {
            throw new FinanceiroInvalidoException("O método de pagamento é obrigatório");
        }

        // Bloqueia a conta para atualização
        PayableAccount conta = payableAccountRepository.buscarParaAtualizacao(contaId)
                .orElseThrow(() -> new FinanceiroInvalidoException("Conta a pagar não encontrada: " + contaId));

        if (conta.getStatus() == PayableAccountStatus.CANCELLED) {
            throw new FinanceiroInvalidoException("Não é possível pagar uma conta cancelada");
        }
        if (conta.getStatus() == PayableAccountStatus.PAID) {
            throw new FinanceiroInvalidoException("Conta já está quitada");
        }

        // Determina a parcela a pagar
        PayableInstallment parcela;
        if (parcelaId != null) {
            parcela = installmentRepository.buscarParaAtualizacao(parcelaId)
                    .orElseThrow(() -> new FinanceiroInvalidoException("Parcela não encontrada: " + parcelaId));
            if (!parcela.getConta().getId().equals(contaId)) {
                throw new FinanceiroInvalidoException("A parcela não pertence à conta informada");
            }
        } else {
            // FIFO: primeira parcela aberta
            parcela = installmentRepository.findByContaIdAndStatus(contaId, PayableInstallmentStatus.OPEN)
                    .stream().findFirst()
                    .orElseThrow(() -> new FinanceiroInvalidoException("Não há parcelas em aberto para esta conta"));
        }

        if (parcela.getStatus() == PayableInstallmentStatus.CANCELLED) {
            throw new FinanceiroInvalidoException("Parcela cancelada não pode receber pagamento");
        }
        if (parcela.getStatus() == PayableInstallmentStatus.PAID) {
            throw new FinanceiroInvalidoException("Parcela já está quitada");
        }

        BigDecimal saldoParcela = parcela.getSaldoPendente();
        if (v.compareTo(saldoParcela) > 0) {
            throw new FinanceiroInvalidoException("O pagamento de " + Dinheiro.formatar(v)
                    + " excede o saldo pendente da parcela (" + Dinheiro.formatar(saldoParcela) + ")");
        }

        // Cria o registro de pagamento
        com.fasteasy.domain.FinancialPayment pagamento = new com.fasteasy.domain.FinancialPayment();
        pagamento.setConta(conta);
        pagamento.setParcela(parcela);
        pagamento.setValor(v);
        pagamento.setMetodoPagamento(metodo);
        pagamento.setReferencia(normalizar(referencia));
        pagamento.setObservacoes(normalizar(observacoes));
        pagamento.setOperador(normalizar(operador));
        pagamento.setStatus("CONFIRMED");
        pagamento.setDataHora(LocalDateTime.now());

        financialPaymentRepository.saveAndFlush(pagamento);

        // Atualiza a parcela
        parcela.setValorPago(parcela.getValorPago().add(v));
        parcela.setSaldoPendente(parcela.getValorOriginal().subtract(parcela.getValorPago()));
        if (parcela.getSaldoPendente().signum() == 0) {
            parcela.setStatus(PayableInstallmentStatus.PAID);
        } else {
            parcela.setStatus(PayableInstallmentStatus.PARTIALLY_PAID);
        }
        installmentRepository.saveAndFlush(parcela);

        // Atualiza a conta
        conta.setValorPago(conta.getValorPago().add(v));
        conta.setSaldoPendente(conta.getValorOriginal().subtract(conta.getValorPago()));
        if (conta.getSaldoPendente().signum() == 0) {
            conta.setStatus(PayableAccountStatus.PAID);
        } else {
            conta.setStatus(PayableAccountStatus.PARTIALLY_PAID);
        }
        payableAccountRepository.saveAndFlush(conta);

        return pagamento;
    }

    // ------------------------------------------------------------------
    // Cancelamento
    // ------------------------------------------------------------------

    /**
     * Cancela uma conta a pagar (apenas se não houver pagamentos confirmados).
     */
    public void cancelar(Long contaId, String motivo, String operador) {
        PayableAccount conta = payableAccountRepository.buscarParaAtualizacao(contaId)
                .orElseThrow(() -> new FinanceiroInvalidoException("Conta a pagar não encontrada: " + contaId));

        if (conta.getStatus() == PayableAccountStatus.CANCELLED) {
            throw new FinanceiroInvalidoException("Conta já está cancelada");
        }
        if (conta.getValorPago().signum() > 0) {
            throw new FinanceiroInvalidoException("Não é possível cancelar uma conta com pagamentos registrados. Faça o estorno dos pagamentos primeiro.");
        }

        conta.setStatus(PayableAccountStatus.CANCELLED);
        conta.setObservacoes((conta.getObservacoes() != null ? conta.getObservacoes() + "; " : "")
                + "Cancelada por " + normalizar(operador) + ": " + normalizar(motivo));
        payableAccountRepository.saveAndFlush(conta);

        // Cancela as parcelas
        for (PayableInstallment p : conta.getParcelas()) {
            if (p.getStatus() != PayableInstallmentStatus.PAID) {
                p.setStatus(PayableInstallmentStatus.CANCELLED);
            }
        }
        installmentRepository.saveAll(conta.getParcelas());
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Optional<PayableAccount> buscarPorId(Long id) {
        return payableAccountRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public PayableAccountDetalhe detalhe(Long id) {
        PayableAccount conta = payableAccountRepository.findById(id)
                .orElseThrow(() -> new FinanceiroInvalidoException("Conta a pagar não encontrada: " + id));
        return toDetalhe(conta);
    }

    @Transactional(readOnly = true)
    public List<PayableAccountResumo> pesquisar(Long fornecedorId,
                                                Long categoriaId,
                                                PayableAccountStatus status,
                                                LocalDate dataInicio,
                                                LocalDate dataFim) {
        return payableAccountRepository.pesquisar(fornecedorId, categoriaId, status, dataInicio, dataFim)
                .stream().map(this::toResumo).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PayableAccountResumo> buscarVencidas() {
        List<PayableAccountStatus> statuses = List.of(PayableAccountStatus.OPEN, PayableAccountStatus.PARTIALLY_PAID);
        return payableAccountRepository.buscarComVencimentoAte(statuses, LocalDate.now())
                .stream().map(this::toResumo).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PayableAccountResumo> buscarProximasVencimento(int dias) {
        List<PayableAccountStatus> statuses = List.of(PayableAccountStatus.OPEN, PayableAccountStatus.PARTIALLY_PAID);
        LocalDate limite = LocalDate.now().plusDays(dias);
        return payableAccountRepository.buscarComVencimentoAte(statuses, limite)
                .stream().map(this::toResumo).collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // Relatórios
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public com.fasteasy.service.dto.ResumoFinanceiroDTO resumoFinanceiro() {
        List<PayableAccountStatus> statusesAberto = List.of(PayableAccountStatus.OPEN, PayableAccountStatus.PARTIALLY_PAID);

        BigDecimal totalEmAberto = payableAccountRepository.somaSaldosPendentes(statusesAberto);
        BigDecimal totalVencidas = BigDecimal.ZERO; // calcular via query se necessário
        BigDecimal totalAVencer7Dias = BigDecimal.ZERO;
        BigDecimal totalAVencer30Dias = BigDecimal.ZERO;

        // Mapear resultados brutos para DTOs
        List<com.fasteasy.service.dto.FornecedorSaldoDTO> topFornecedores =
                payableAccountRepository.saldosPorFornecedorRaw(statusesAberto).stream()
                        .map(row -> new com.fasteasy.service.dto.FornecedorSaldoDTO(
                                row[0] != null ? (Long) row[0] : null,
                                row[1] != null ? (String) row[1] : null,
                                row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO))
                        .collect(Collectors.toList());

        List<com.fasteasy.service.dto.CategoriaSaldoDTO> topCategorias =
                payableAccountRepository.saldosPorCategoriaRaw(statusesAberto).stream()
                        .map(row -> new com.fasteasy.service.dto.CategoriaSaldoDTO(
                                row[0] != null ? (Long) row[0] : null,
                                row[1] != null ? (String) row[1] : null,
                                row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO))
                        .collect(Collectors.toList());

        long qtdAberto = payableAccountRepository.countByStatusIn(statusesAberto);
        long qtdVencidas = 0L;

        return new com.fasteasy.service.dto.ResumoFinanceiroDTO(
                totalEmAberto, totalVencidas, totalAVencer7Dias, totalAVencer30Dias,
                BigDecimal.ZERO, totalEmAberto, BigDecimal.ZERO,
                qtdAberto, qtdVencidas,
                topFornecedores, topCategorias
        );
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private List<PayableInstallment> criarParcelas(PayableAccount conta, List<ParcelaInput> parcelasInput) {
        List<PayableInstallment> parcelas = new ArrayList<>();
        for (ParcelaInput input : parcelasInput) {
            PayableInstallment parcela = new PayableInstallment();
            parcela.setConta(conta);
            parcela.setNumeroParcela(input.getNumeroParcela());
            parcela.setDataVencimento(input.getDataVencimento());
            parcela.setValorOriginal(Dinheiro.arredondar(input.getValor()));
            parcela.setValorPago(BigDecimal.ZERO);
            parcela.setSaldoPendente(parcela.getValorOriginal());
            parcela.setStatus(PayableInstallmentStatus.OPEN);
            parcela.setDataCriacao(LocalDateTime.now());
            parcelas.add(parcela);
        }
        return parcelas;
    }

    private void validarConta(PayableAccount conta, List<ParcelaInput> parcelas) {
        if (conta == null) {
            throw new FinanceiroInvalidoException("A conta é obrigatória");
        }
        if (conta.getDescricao() == null || conta.getDescricao().trim().isEmpty()) {
            throw new FinanceiroInvalidoException("A descrição é obrigatória");
        }
        if (conta.getCategoria() == null || conta.getCategoria().getId() == null) {
            throw new FinanceiroInvalidoException("A categoria é obrigatória");
        }
        if (conta.getValorOriginal() == null || Dinheiro.arredondar(conta.getValorOriginal()).signum() <= 0) {
            throw new FinanceiroInvalidoException("O valor total deve ser maior que zero");
        }
        if (conta.getDataEmissao() == null) {
            throw new FinanceiroInvalidoException("A data de emissão é obrigatória");
        }
        validarParcelas(parcelas, conta.getValorOriginal());
    }

    private void validarParcelas(List<ParcelaInput> parcelas, BigDecimal valorTotal) {
        if (parcelas == null || parcelas.isEmpty()) {
            throw new FinanceiroInvalidoException("Pelo menos uma parcela é obrigatória");
        }

        BigDecimal soma = BigDecimal.ZERO;
        for (ParcelaInput p : parcelas) {
            if (p.getNumeroParcela() == null || p.getNumeroParcela() <= 0) {
                throw new FinanceiroInvalidoException("Número da parcela inválido");
            }
            if (p.getDataVencimento() == null) {
                throw new FinanceiroInvalidoException("Data de vencimento obrigatória para todas as parcelas");
            }
            if (p.getValor() == null || Dinheiro.arredondar(p.getValor()).signum() <= 0) {
                throw new FinanceiroInvalidoException("Valor da parcela deve ser maior que zero");
            }
            soma = soma.add(Dinheiro.arredondar(p.getValor()));
        }

        // Verifica se a soma das parcelas confere com o valor total (com tolerância de arredondamento)
        BigDecimal diff = Dinheiro.arredondar(soma.subtract(valorTotal));
        if (diff.abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new FinanceiroInvalidoException("A soma das parcelas (" + Dinheiro.formatar(soma)
                    + ") difere do valor total (" + Dinheiro.formatar(valorTotal) + ")");
        }
    }

    private PayableAccountResumo toResumo(PayableAccount conta) {
        List<PayableInstallmentResumo> parcelasResumo = conta.getParcelas().stream()
                .map(p -> new PayableInstallmentResumo(
                        p.getId(), p.getNumeroParcela(), p.getDataVencimento(),
                        p.getValorOriginal(), p.getValorPago(), p.getSaldoPendente(), p.getStatus()))
                .collect(Collectors.toList());

        // Próximo vencimento
        LocalDate proximoVenc = conta.getParcelas().stream()
                .filter(p -> p.getStatus() != PayableInstallmentStatus.PAID && p.getStatus() != PayableInstallmentStatus.CANCELLED)
                .map(PayableInstallment::getDataVencimento)
                .min(LocalDate::compareTo)
                .orElse(null);

        long parcelasPagas = conta.getParcelas().stream()
                .filter(p -> p.getStatus() == PayableInstallmentStatus.PAID)
                .count();

        return new PayableAccountResumo(
                conta.getId(),
                conta.getDescricao(),
                conta.getFornecedor() != null ? conta.getFornecedor().getNomeRazaoSocial() : null,
                conta.getFornecedor() != null ? conta.getFornecedor().getId() : null,
                conta.getCategoria() != null ? conta.getCategoria().getNome() : null,
                conta.getCategoria() != null ? conta.getCategoria().getId() : null,
                conta.getDataEmissao(),
                conta.getDataCompetencia(),
                proximoVenc,
                conta.getValorOriginal(),
                conta.getValorPago(),
                conta.getSaldoPendente(),
                conta.getStatus(),
                conta.getParcelas().size(),
                (int) parcelasPagas,
                conta.getObservacoes()
        );
    }

    private PayableAccountDetalhe toDetalhe(PayableAccount conta) {
        List<PayableInstallmentResumo> parcelasResumo = conta.getParcelas().stream()
                .map(p -> new PayableInstallmentResumo(
                        p.getId(), p.getNumeroParcela(), p.getDataVencimento(),
                        p.getValorOriginal(), p.getValorPago(), p.getSaldoPendente(), p.getStatus()))
                .collect(Collectors.toList());

        List<FinancialPaymentResumo> pagamentosResumo = financialPaymentRepository.findByContaIdOrderByDataHoraAsc(conta.getId())
                .stream()
                .map(p -> new FinancialPaymentResumo(
                        p.getId(), p.getConta().getId(),
                        p.getParcela() != null ? p.getParcela().getId() : null,
                        p.getValor(), p.getDataHora(), p.getMetodoPagamento(),
                        p.getReferencia(), p.getObservacoes(), p.getOperador(), p.getStatus()))
                .collect(Collectors.toList());

        return new PayableAccountDetalhe(toResumo(conta), parcelasResumo, pagamentosResumo);
    }

    private String normalizar(String texto) {
        if (texto == null) return null;
        String t = texto.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * Input para criação de parcelas.
     */
    public static class ParcelaInput {
        private Integer numeroParcela;
        private LocalDate dataVencimento;
        private BigDecimal valor;

        public Integer getNumeroParcela() { return numeroParcela; }
        public void setNumeroParcela(Integer numeroParcela) { this.numeroParcela = numeroParcela; }
        public LocalDate getDataVencimento() { return dataVencimento; }
        public void setDataVencimento(LocalDate dataVencimento) { this.dataVencimento = dataVencimento; }
        public BigDecimal getValor() { return valor; }
        public void setValor(BigDecimal valor) { this.valor = valor; }
    }
}