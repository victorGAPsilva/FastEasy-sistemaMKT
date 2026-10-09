package com.fasteasy.service;

import com.fasteasy.domain.CaixaInvalidoException;
import com.fasteasy.domain.CarrinhoVenda;
import com.fasteasy.domain.CashMovement;
import com.fasteasy.domain.CashRegister;
import com.fasteasy.domain.CashSession;
import com.fasteasy.domain.Category;
import com.fasteasy.domain.FormaPagamento;
import com.fasteasy.domain.Product;
import com.fasteasy.domain.StatusSessaoCaixa;
import com.fasteasy.domain.TipoMovimentacaoCaixa;
import com.fasteasy.repository.CashMovementRepository;
import com.fasteasy.repository.CashRegisterRepository;
import com.fasteasy.repository.CashSessionRepository;
import com.fasteasy.repository.CategoryRepository;
import com.fasteasy.repository.ProductRepository;
import com.fasteasy.repository.SaleRepository;
import com.fasteasy.repository.StockMovementRepository;
import com.fasteasy.service.dto.PagamentoDTO;
import com.fasteasy.service.dto.ResumoCaixaDTO;
import com.fasteasy.service.dto.SessaoCaixaResumo;
import com.fasteasy.testconfig.TestVendaConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração do módulo de gestão de caixa (H2).
 *
 * <p>Exercitam abertura, movimentações, integração com vendas, cálculo do
 * saldo esperado, fechamento, histórico e concorrência com bloqueio
 * pessimista real no banco.</p>
 */
@SpringBootTest(classes = TestVendaConfig.class)
@ActiveProfiles("test")
@DisplayName("Gestão de caixa (integração)")
class CaixaIntegracaoTest {

    @Autowired private SaleService saleService;
    @Autowired private CashSessionService cashSessionService;
    @Autowired private CashRegisterService cashRegisterService;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private SaleRepository saleRepository;
    @Autowired private StockMovementRepository stockMovementRepository;
    @Autowired private CashRegisterRepository cashRegisterRepository;
    @Autowired private CashSessionRepository cashSessionRepository;
    @Autowired private CashMovementRepository cashMovementRepository;

    private Category categoria;

    @BeforeEach
    void limpar() {
        stockMovementRepository.deleteAll();
        saleRepository.deleteAll();
        cashMovementRepository.deleteAll();
        cashSessionRepository.deleteAll();
        cashRegisterRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        categoria = categoryRepository.save(new Category("Geral"));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private CashRegister caixa(String nome) {
        return cashRegisterService.cadastrar(new CashRegister(nome));
    }

    private Product produto(String nome, String preco, String estoque) {
        Product p = new Product(nome);
        p.setCategoria(categoria);
        p.setPrecoVenda(new BigDecimal(preco));
        p.setEstoqueAtual(new BigDecimal(estoque));
        p.setStatus("ATIVO");
        return productRepository.save(p);
    }

    private PagamentoDTO dinheiro(String valor, String recebido) {
        return new PagamentoDTO(FormaPagamento.DINHEIRO, new BigDecimal(valor), new BigDecimal(recebido));
    }

    private PagamentoDTO pagamento(FormaPagamento forma, String valor) {
        return new PagamentoDTO(forma, new BigDecimal(valor), null);
    }

    private void vender(Product produto, String quantidade, CashSession sessao, PagamentoDTO... pagamentos) {
        CarrinhoVenda carrinho = new CarrinhoVenda();
        carrinho.adicionar(produto, new BigDecimal(quantidade), new BigDecimal("100000"));
        saleService.finalizarVenda(carrinho, List.of(pagamentos), "Operador", sessao);
    }

    // ------------------------------------------------------------------
    // Abertura
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Abre caixa com saldo inicial zero")
    void abreComSaldoZero() {
        CashRegister c = caixa("Caixa 1");
        CashSession sessao = cashSessionService.abrirSessao(c.getId(), BigDecimal.ZERO, "turno manhã", "Ana");

        assertNotNull(sessao.getId());
        assertEquals(StatusSessaoCaixa.OPEN, sessao.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(sessao.getSaldoInicial()));
        assertTrue(cashSessionService.buscarSessaoAberta(c.getId()).isPresent());
    }

    @Test
    @DisplayName("Abre caixa com saldo inicial positivo")
    void abreComSaldoPositivo() {
        CashRegister c = caixa("Caixa 1");
        CashSession sessao = cashSessionService.abrirSessao(c.getId(), new BigDecimal("150.00"), null, "Ana");
        assertEquals(0, new BigDecimal("150.00").compareTo(sessao.getSaldoInicial()));
    }

    @Test
    @DisplayName("Rejeita abertura com saldo inicial negativo")
    void rejeitaSaldoNegativo() {
        CashRegister c = caixa("Caixa 1");
        assertThrows(CaixaInvalidoException.class,
                () -> cashSessionService.abrirSessao(c.getId(), new BigDecimal("-1.00"), null, "Ana"));
    }

    @Test
    @DisplayName("Impede duas sessões abertas no mesmo caixa")
    void impedeDuasSessoesAbertas() {
        CashRegister c = caixa("Caixa 1");
        cashSessionService.abrirSessao(c.getId(), BigDecimal.ZERO, null, "Ana");
        assertThrows(CaixaInvalidoException.class,
                () -> cashSessionService.abrirSessao(c.getId(), BigDecimal.ZERO, null, "Ana"));
    }

    @Test
    @DisplayName("Concorrência: apenas uma abertura simultânea prospera")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void concorrenciaAbertura() throws Exception {
        CashRegister c = caixa("Caixa 1");
        AtomicInteger sucessos = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CyclicBarrier barreira = new CyclicBarrier(2);
        try {
            Callable<Void> abrir = () -> {
                barreira.await(10, TimeUnit.SECONDS);
                try {
                    cashSessionService.abrirSessao(c.getId(), new BigDecimal("50.00"), null, "Ana");
                    sucessos.incrementAndGet();
                } catch (RuntimeException ignorado) {
                    // esperado quando a sessão já foi aberta
                }
                return null;
            };
            Future<?> f1 = pool.submit(abrir);
            Future<?> f2 = pool.submit(abrir);
            f1.get(20, TimeUnit.SECONDS);
            f2.get(20, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, sucessos.get(), "apenas uma abertura deve ser aceita");
        assertEquals(1, cashSessionRepository.count());
        assertTrue(cashSessionService.buscarSessaoAberta(c.getId()).isPresent());
    }

    // ------------------------------------------------------------------
    // Movimentações
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Registra suprimento e sangria e atualiza o saldo esperado")
    void suprimentoESangria() {
        CashRegister c = caixa("Caixa 1");
        cashSessionService.abrirSessao(c.getId(), new BigDecimal("100.00"), null, "Ana");

        cashSessionService.registrarSuprimento(c.getId(), new BigDecimal("30.00"), "Reforço de troco", "Ana");
        cashSessionService.registrarSangria(c.getId(), new BigDecimal("40.00"), "Retirada para cofre", "Ana");

        ResumoCaixaDTO resumo = cashSessionService.resumoDaSessaoAberta(c.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("30.00").compareTo(resumo.totalSuprimentos()));
        assertEquals(0, new BigDecimal("40.00").compareTo(resumo.totalSangrias()));
        assertEquals(0, new BigDecimal("90.00").compareTo(resumo.saldoEsperado()));
    }

    @Test
    @DisplayName("Rejeita movimentação com valor zero ou negativo")
    void rejeitaValorInvalido() {
        CashRegister c = caixa("Caixa 1");
        cashSessionService.abrirSessao(c.getId(), new BigDecimal("100.00"), null, "Ana");

        assertThrows(CaixaInvalidoException.class,
                () -> cashSessionService.registrarSuprimento(c.getId(), BigDecimal.ZERO, "x", "Ana"));
        assertThrows(CaixaInvalidoException.class,
                () -> cashSessionService.registrarSangria(c.getId(), new BigDecimal("-5"), "x", "Ana"));
    }

    @Test
    @DisplayName("Exige motivo na movimentação")
    void exigeMotivo() {
        CashRegister c = caixa("Caixa 1");
        cashSessionService.abrirSessao(c.getId(), new BigDecimal("100.00"), null, "Ana");
        assertThrows(CaixaInvalidoException.class,
                () -> cashSessionService.registrarSuprimento(c.getId(), new BigDecimal("10"), "  ", "Ana"));
    }

    @Test
    @DisplayName("Rejeita movimentação sem sessão aberta")
    void rejeitaSemSessaoAberta() {
        CashRegister c = caixa("Caixa 1");
        assertThrows(CaixaInvalidoException.class,
                () -> cashSessionService.registrarSuprimento(c.getId(), new BigDecimal("10"), "x", "Ana"));
    }

    @Test
    @DisplayName("Impede sangria que deixaria o saldo esperado negativo")
    void impedeSangriaAcimaDoSaldo() {
        CashRegister c = caixa("Caixa 1");
        cashSessionService.abrirSessao(c.getId(), new BigDecimal("100.00"), null, "Ana");
        cashSessionService.registrarSangria(c.getId(), new BigDecimal("80.00"), "cofre", "Ana");

        assertThrows(CaixaInvalidoException.class,
                () -> cashSessionService.registrarSangria(c.getId(), new BigDecimal("30.00"), "cofre", "Ana"));

        assertEquals(1, cashMovementRepository.countBySessaoId(
                cashSessionService.buscarSessaoAberta(c.getId()).orElseThrow().getId()));
    }

    @Test
    @DisplayName("Concorrência: duas sangrias não deixam o saldo negativo")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void concorrenciaSangrias() throws Exception {
        CashRegister c = caixa("Caixa 1");
        cashSessionService.abrirSessao(c.getId(), new BigDecimal("100.00"), null, "Ana");

        AtomicInteger sucessos = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CyclicBarrier barreira = new CyclicBarrier(2);
        try {
            Callable<Void> sangria = () -> {
                barreira.await(10, TimeUnit.SECONDS);
                try {
                    cashSessionService.registrarSangria(c.getId(), new BigDecimal("80.00"), "cofre", "Ana");
                    sucessos.incrementAndGet();
                } catch (RuntimeException ignorado) {
                    // esperado quando o saldo não é suficiente
                }
                return null;
            };
            Future<?> f1 = pool.submit(sangria);
            Future<?> f2 = pool.submit(sangria);
            f1.get(20, TimeUnit.SECONDS);
            f2.get(20, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, sucessos.get(), "apenas uma sangria deve ser aceita");
        ResumoCaixaDTO resumo = cashSessionService.resumoDaSessaoAberta(c.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("20.00").compareTo(resumo.saldoEsperado()));
    }

    // ------------------------------------------------------------------
    // Integração com vendas e saldo esperado
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Venda em dinheiro com troco entra como recebido menos troco")
    void vendaDinheiroComTroco() {
        CashRegister c = caixa("Caixa 1");
        CashSession sessao = cashSessionService.abrirSessao(c.getId(), new BigDecimal("100.00"), null, "Ana");
        Product p = produto("Produto", "35.00", "10");

        vender(p, "1", sessao, dinheiro("35.00", "50.00"));

        ResumoCaixaDTO resumo = cashSessionService.resumo(sessao.getId());
        // Faturamento em dinheiro = valor aplicado (35), não o valor entregue.
        assertEquals(0, new BigDecimal("35.00").compareTo(resumo.totalPorForma(FormaPagamento.DINHEIRO)));
        // Entrada física de dinheiro = valor entregue (50); troco devolvido = 15.
        assertEquals(0, new BigDecimal("50.00").compareTo(resumo.totalDinheiroRecebido()));
        assertEquals(0, new BigDecimal("15.00").compareTo(resumo.totalTroco()));
        assertEquals(1, resumo.quantidadeVendas());
        // 100 + 50 - 15 = 135
        assertEquals(0, new BigDecimal("135.00").compareTo(resumo.saldoEsperado()));
        // Sem duplicidade: entrada física menos troco = 35 (valor efetivo da venda).
        assertEquals(0, new BigDecimal("35.00").compareTo(
                resumo.totalDinheiroRecebido().subtract(resumo.totalTroco())));
    }

    @Test
    @DisplayName("Exemplo do enunciado produz saldo esperado 145,00")
    void exemploDoEnunciado() {
        CashRegister c = caixa("Caixa 1");
        CashSession sessao = cashSessionService.abrirSessao(c.getId(), new BigDecimal("100.00"), null, "Ana");
        Product p35 = produto("Venda 35", "35.00", "10");
        Product p20 = produto("Venda 20", "20.00", "10");

        vender(p35, "1", sessao, dinheiro("35.00", "50.00"));
        vender(p20, "1", sessao, dinheiro("20.00", "20.00"));
        cashSessionService.registrarSuprimento(c.getId(), new BigDecimal("30.00"), "Reforço", "Ana");
        cashSessionService.registrarSangria(c.getId(), new BigDecimal("40.00"), "Cofre", "Ana");

        ResumoCaixaDTO resumo = cashSessionService.resumo(sessao.getId());
        assertEquals(0, new BigDecimal("145.00").compareTo(resumo.saldoEsperado()));
        assertEquals(2, resumo.quantidadeVendas());
    }

    @Test
    @DisplayName("PIX, débito e crédito são totalizados separadamente e não alteram o dinheiro")
    void formasEletronicasSeparadas() {
        CashRegister c = caixa("Caixa 1");
        CashSession sessao = cashSessionService.abrirSessao(c.getId(), new BigDecimal("100.00"), null, "Ana");
        Product p = produto("Produto", "100.00", "10");

        vender(p, "1", sessao,
                pagamento(FormaPagamento.PIX, "30.00"),
                pagamento(FormaPagamento.DEBITO, "30.00"),
                pagamento(FormaPagamento.CREDITO, "40.00"));

        ResumoCaixaDTO resumo = cashSessionService.resumo(sessao.getId());
        assertEquals(0, new BigDecimal("30.00").compareTo(resumo.totalPorForma(FormaPagamento.PIX)));
        assertEquals(0, new BigDecimal("30.00").compareTo(resumo.totalPorForma(FormaPagamento.DEBITO)));
        assertEquals(0, new BigDecimal("40.00").compareTo(resumo.totalPorForma(FormaPagamento.CREDITO)));
        assertEquals(0, BigDecimal.ZERO.compareTo(resumo.totalPorForma(FormaPagamento.DINHEIRO)));
        assertEquals(0, new BigDecimal("100.00").compareTo(resumo.saldoEsperado()),
                "vendas eletrônicas não aumentam o dinheiro físico");
    }

    // ------------------------------------------------------------------
    // Fechamento
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Fecha caixa com diferença zero")
    void fechaComDiferencaZero() {
        CashRegister c = caixa("Caixa 1");
        cashSessionService.abrirSessao(c.getId(), new BigDecimal("100.00"), null, "Ana");

        ResumoCaixaDTO resumo = cashSessionService.fecharSessao(c.getId(), new BigDecimal("100.00"), "ok", "Ana");
        assertEquals(StatusSessaoCaixa.CLOSED, resumo.status());
        assertNotNull(resumo.dataFechamento());
        assertEquals(0, BigDecimal.ZERO.compareTo(resumo.diferenca()));
        assertFalse(cashSessionService.buscarSessaoAberta(c.getId()).isPresent());
    }

    @Test
    @DisplayName("Calcula sobra de caixa")
    void calculaSobra() {
        CashRegister c = caixa("Caixa 1");
        cashSessionService.abrirSessao(c.getId(), new BigDecimal("100.00"), null, "Ana");
        ResumoCaixaDTO resumo = cashSessionService.fecharSessao(c.getId(), new BigDecimal("110.00"), null, "Ana");
        assertEquals(0, new BigDecimal("10.00").compareTo(resumo.diferenca()));
    }

    @Test
    @DisplayName("Calcula falta de caixa")
    void calculaFalta() {
        CashRegister c = caixa("Caixa 1");
        cashSessionService.abrirSessao(c.getId(), new BigDecimal("100.00"), null, "Ana");
        ResumoCaixaDTO resumo = cashSessionService.fecharSessao(c.getId(), new BigDecimal("90.00"), null, "Ana");
        assertEquals(0, new BigDecimal("-10.00").compareTo(resumo.diferenca()));
    }

    @Test
    @DisplayName("Rejeita total contado negativo")
    void rejeitaContadoNegativo() {
        CashRegister c = caixa("Caixa 1");
        cashSessionService.abrirSessao(c.getId(), new BigDecimal("100.00"), null, "Ana");
        assertThrows(CaixaInvalidoException.class,
                () -> cashSessionService.fecharSessao(c.getId(), new BigDecimal("-1"), null, "Ana"));
    }

    @Test
    @DisplayName("Impede fechamento duplicado")
    void impedeFechamentoDuplicado() {
        CashRegister c = caixa("Caixa 1");
        cashSessionService.abrirSessao(c.getId(), new BigDecimal("100.00"), null, "Ana");
        cashSessionService.fecharSessao(c.getId(), new BigDecimal("100.00"), null, "Ana");
        assertThrows(CaixaInvalidoException.class,
                () -> cashSessionService.fecharSessao(c.getId(), new BigDecimal("100.00"), null, "Ana"));
    }

    @Test
    @DisplayName("Sessão fechada não aceita novas vendas")
    void sessaoFechadaNaoAceitaVenda() {
        CashRegister c = caixa("Caixa 1");
        CashSession sessao = cashSessionService.abrirSessao(c.getId(), new BigDecimal("100.00"), null, "Ana");
        Product p = produto("Produto", "10.00", "10");
        cashSessionService.fecharSessao(c.getId(), new BigDecimal("100.00"), null, "Ana");

        CarrinhoVenda carrinho = new CarrinhoVenda();
        carrinho.adicionar(p, BigDecimal.ONE, new BigDecimal("10"));
        assertThrows(CaixaInvalidoException.class,
                () -> saleService.finalizarVenda(carrinho, List.of(dinheiro("10.00", "10.00")),
                        "Ana", sessao));
        assertEquals(0, saleRepository.count(), "nenhuma venda deve ser gravada");
    }

    @Test
    @DisplayName("Concorrência: apenas um fechamento simultâneo prospera")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void concorrenciaFechamento() throws Exception {
        CashRegister c = caixa("Caixa 1");
        cashSessionService.abrirSessao(c.getId(), new BigDecimal("100.00"), null, "Ana");

        AtomicInteger sucessos = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CyclicBarrier barreira = new CyclicBarrier(2);
        try {
            Callable<Void> fechar = () -> {
                barreira.await(10, TimeUnit.SECONDS);
                try {
                    cashSessionService.fecharSessao(c.getId(), new BigDecimal("100.00"), null, "Ana");
                    sucessos.incrementAndGet();
                } catch (RuntimeException ignorado) {
                    // esperado
                }
                return null;
            };
            Future<?> f1 = pool.submit(fechar);
            Future<?> f2 = pool.submit(fechar);
            f1.get(20, TimeUnit.SECONDS);
            f2.get(20, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, sucessos.get(), "apenas um fechamento deve ser aceito");
    }

    // ------------------------------------------------------------------
    // Histórico
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Histórico lista sessões abertas e fechadas com filtro")
    void historicoDeSessoes() {
        CashRegister c1 = caixa("Caixa 1");
        CashRegister c2 = caixa("Caixa 2");
        CashSession s1 = cashSessionService.abrirSessao(c1.getId(), new BigDecimal("100.00"), null, "Ana");
        cashSessionService.fecharSessao(c1.getId(), new BigDecimal("100.00"), null, "Ana");
        cashSessionService.abrirSessao(c2.getId(), new BigDecimal("50.00"), null, "Bia");

        List<SessaoCaixaResumo> todas = cashSessionService.listarHistorico(null, null, null, null);
        assertEquals(2, todas.size());

        List<SessaoCaixaResumo> fechadas = cashSessionService.listarHistorico(null, null, null, StatusSessaoCaixa.CLOSED);
        assertEquals(1, fechadas.size());
        assertEquals(s1.getId(), fechadas.get(0).id());

        assertNotNull(cashSessionService.detalhe(s1.getId()).resumo());
        assertNotNull(s1.getId());
    }

    @Test
    @DisplayName("Movimentações registradas não podem ser alteradas (somente leitura)")
    void movimentacoesImutaveis() {
        CashRegister c = caixa("Caixa 1");
        cashSessionService.abrirSessao(c.getId(), new BigDecimal("100.00"), null, "Ana");
        CashMovement mov = cashSessionService.registrarSuprimento(c.getId(), new BigDecimal("10.00"), "x", "Ana");

        // Não há API de edição/exclusão; o registro permanece com o valor original.
        assertEquals(0, new BigDecimal("10.00").compareTo(
                cashMovementRepository.findById(mov.getId()).orElseThrow().getValor()));
    }

}
