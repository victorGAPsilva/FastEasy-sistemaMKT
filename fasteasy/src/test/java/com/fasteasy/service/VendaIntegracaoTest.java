package com.fasteasy.service;

import com.fasteasy.domain.CarrinhoVenda;
import com.fasteasy.domain.Category;
import com.fasteasy.domain.EstoqueInsuficienteException;
import com.fasteasy.domain.FormaPagamento;
import com.fasteasy.domain.Product;
import com.fasteasy.domain.Sale;
import com.fasteasy.domain.TipoMovimentacaoEstoque;
import com.fasteasy.domain.UnidadeMedida;
import com.fasteasy.domain.VendaInvalidaException;
import com.fasteasy.repository.CategoryRepository;
import com.fasteasy.repository.ProductRepository;
import com.fasteasy.repository.SaleRepository;
import com.fasteasy.repository.StockMovementRepository;
import com.fasteasy.service.dto.PagamentoDTO;
import com.fasteasy.service.dto.VendaDetalhe;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração do módulo de vendas usando banco H2 real.
 *
 * <p>Verifica atomicidade (rollback), persistência de valores históricos,
 * baixa de estoque com movimentação SALE_EXIT e concorrência no último item.
 * Não usa {@code @Transactional} para que cada chamada de serviço confirme ou
 * reverta em sua própria transação.</p>
 */
@SpringBootTest(classes = TestVendaConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@DisplayName("Integração de vendas")
class VendaIntegracaoTest {

    @Autowired
    private SaleService saleService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @BeforeEach
    void limparBase() {
        stockMovementRepository.deleteAll();
        saleRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    private Product produto(String nome, String preco, String estoque) {
        Category categoria = categoryRepository.save(new Category("Mercearia"));
        Product produto = new Product(nome);
        produto.setCategoria(categoria);
        produto.setPrecoVenda(new BigDecimal(preco));
        produto.setEstoqueAtual(new BigDecimal(estoque));
        produto.setEstoqueMinimo(1);
        produto.setUnidadeMedida(UnidadeMedida.UN);
        produto.setStatus("ATIVO");
        return productRepository.save(produto);
    }

    private CarrinhoVenda carrinho(Product produto, String quantidade) {
        CarrinhoVenda carrinho = new CarrinhoVenda();
        carrinho.adicionar(produto, new BigDecimal(quantidade), produto.getEstoqueAtual());
        return carrinho;
    }

    private PagamentoDTO pix(String valor) {
        return new PagamentoDTO(FormaPagamento.PIX, new BigDecimal(valor), null);
    }

    @Test
    @DisplayName("Finaliza a venda, baixa o estoque e registra SALE_EXIT")
    void finalizaEDebitaEstoque() {
        Product produto = produto("Arroz 5kg", "10.00", "10");
        VendaDetalhe detalhe = saleService.finalizarVenda(
                carrinho(produto, "3"), List.of(pix("30.00")), "Maria");

        assertEquals(1, saleRepository.count());
        assertEquals(String.format("%06d", detalhe.id()), detalhe.numero());

        BigDecimal estoqueFinal = productRepository.findById(produto.getId()).orElseThrow().getEstoqueAtual();
        assertEquals(0, new BigDecimal("7").compareTo(estoqueFinal));
        assertEquals(1, stockMovementRepository.countByVendaIdAndTipo(
                detalhe.id(), TipoMovimentacaoEstoque.SALE_EXIT));
    }

    @Test
    @DisplayName("Falha em um item reverte toda a venda (rollback atômico)")
    void rollbackQuandoItemSemEstoque() {
        Product a = produto("Produto A", "10.00", "10");
        Product b = produto("Produto B", "5.00", "1");

        CarrinhoVenda carrinho = new CarrinhoVenda();
        carrinho.adicionar(a, new BigDecimal("2"), new BigDecimal("10"));
        // Informa estoque fictício para que o carrinho aceite e a falha ocorra na finalização.
        carrinho.adicionar(b, new BigDecimal("5"), new BigDecimal("10"));

        assertThrows(EstoqueInsuficienteException.class, () -> saleService.finalizarVenda(
                carrinho, List.of(pix("45.00")), "Maria"));

        assertEquals(0, saleRepository.count(), "nenhuma venda deve ser gravada");
        assertEquals(0, stockMovementRepository.count(), "nenhuma movimentação deve permanecer");
        assertEquals(0, new BigDecimal("10").compareTo(
                productRepository.findById(a.getId()).orElseThrow().getEstoqueAtual()),
                "estoque do primeiro item deve ser revertido");
        assertFalse(carrinho.isFinalizada(), "carrinho deve permitir nova tentativa");
    }

    @Test
    @DisplayName("Valores históricos do item são preservados após alterar o produto")
    void preservaValoresHistoricos() {
        Product produto = produto("Feijão", "8.00", "10");
        VendaDetalhe detalhe = saleService.finalizarVenda(
                carrinho(produto, "2"), List.of(pix("16.00")), "Maria");

        produto.setNome("Feijão Premium");
        produto.setPrecoVenda(new BigDecimal("99.00"));
        productRepository.save(produto);

        VendaDetalhe recarregado = saleService.buscarDetalhe(detalhe.id());
        assertEquals("Feijão", recarregado.itens().get(0).nomeProduto());
        assertEquals(0, new BigDecimal("8.00").compareTo(recarregado.itens().get(0).precoUnitario()));
        assertEquals(0, new BigDecimal("16.00").compareTo(recarregado.itens().get(0).total()));
    }

    @Test
    @DisplayName("Não grava a venda duas vezes com cliques repetidos")
    void naoDuplicaVenda() {
        Product produto = produto("Leite", "5.00", "10");
        CarrinhoVenda carrinho = carrinho(produto, "1");

        saleService.finalizarVenda(carrinho, List.of(pix("5.00")), "Maria");
        assertThrows(VendaInvalidaException.class,
                () -> saleService.finalizarVenda(carrinho, List.of(pix("5.00")), "Maria"));

        assertEquals(1, saleRepository.count());
    }

    @Test
    @DisplayName("Concorrência: apenas uma venda consome o último item")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void concorrenciaNoUltimoItem() throws Exception {
        Product produto = produto("Último item", "10.00", "1");

        CarrinhoVenda c1 = carrinho(produto, "1");
        CarrinhoVenda c2 = carrinho(produto, "1");
        List<PagamentoDTO> pagamentos = List.of(pix("10.00"));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CyclicBarrier barreira = new CyclicBarrier(2);
        try {
            Callable<Boolean> venda1 = () -> executar(c1, pagamentos, barreira);
            Callable<Boolean> venda2 = () -> executar(c2, pagamentos, barreira);
            Future<Boolean> f1 = pool.submit(venda1);
            Future<Boolean> f2 = pool.submit(venda2);

            boolean r1 = f1.get(20, TimeUnit.SECONDS);
            boolean r2 = f2.get(20, TimeUnit.SECONDS);
            assertEquals(1, (r1 ? 1 : 0) + (r2 ? 1 : 0),
                    "exatamente uma venda deve ser concluída");
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, saleRepository.count());
        assertEquals(0, BigDecimal.ZERO.compareTo(
                productRepository.findById(produto.getId()).orElseThrow().getEstoqueAtual()));
    }

    private boolean executar(CarrinhoVenda carrinho, List<PagamentoDTO> pagamentos, CyclicBarrier barreira) {
        try {
            barreira.await(10, TimeUnit.SECONDS);
            saleService.finalizarVenda(carrinho, pagamentos, "Concorrente");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
