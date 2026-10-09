package com.fasteasy.service;

import com.fasteasy.domain.CarrinhoVenda;
import com.fasteasy.domain.FormaPagamento;
import com.fasteasy.domain.Product;
import com.fasteasy.domain.Sale;
import com.fasteasy.domain.UnidadeMedida;
import com.fasteasy.domain.VendaInvalidaException;
import com.fasteasy.repository.ProductRepository;
import com.fasteasy.repository.SaleRepository;
import com.fasteasy.service.dto.PagamentoDTO;
import com.fasteasy.service.dto.VendaDetalhe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testes de unidade do SaleService (repositórios e estoque simulados).
 * Verificam orquestração, validação de pagamentos e regras de finalização.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Serviço de vendas")
class SaleServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private StockService stockService;

    @InjectMocks
    private SaleService saleService;

    private Product produto;

    @BeforeEach
    void setUp() {
        produto = new Product("Arroz 5kg");
        produto.setId(1L);
        produto.setPrecoVenda(new BigDecimal("10.00"));
        produto.setEstoqueAtual(new BigDecimal("100"));
        produto.setStatus("ATIVO");
        produto.setUnidadeMedida(UnidadeMedida.UN);

        when(productRepository.buscarParaAtualizacao(1L)).thenReturn(Optional.of(produto));
        when(saleRepository.saveAndFlush(any(Sale.class))).thenAnswer(inv -> {
            Sale sale = inv.getArgument(0);
            if (sale.getId() == null) {
                sale.setId(1L);
            }
            return sale;
        });
    }

    private CarrinhoVenda carrinho(String quantidade) {
        CarrinhoVenda carrinho = new CarrinhoVenda();
        carrinho.adicionar(produto, new BigDecimal(quantidade), new BigDecimal("100"));
        return carrinho;
    }

    private PagamentoDTO pagamento(FormaPagamento forma, String valor, String recebido) {
        return new PagamentoDTO(forma,
                valor == null ? null : new BigDecimal(valor),
                recebido == null ? null : new BigDecimal(recebido));
    }

    @Test
    @DisplayName("Dinheiro com troco finaliza e registra saída de estoque")
    void finalizaComDinheiroECalculaTroco() {
        CarrinhoVenda carrinho = carrinho("2");
        VendaDetalhe detalhe = saleService.finalizarVenda(
                carrinho, List.of(pagamento(FormaPagamento.DINHEIRO, "20.00", "25.00")), "Maria");

        assertEquals("000001", detalhe.numero());
        assertEquals(0, new BigDecimal("20.00").compareTo(detalhe.valorTotal()));
        assertEquals(0, new BigDecimal("25.00").compareTo(detalhe.valorRecebido()));
        assertEquals(0, new BigDecimal("5.00").compareTo(detalhe.troco()));
        assertEquals(1, detalhe.itens().size());
        assertEquals("Arroz 5kg", detalhe.itens().get(0).nomeProduto());
        assertEquals(0, new BigDecimal("2").compareTo(detalhe.itens().get(0).quantidade()));
        assertEquals(0, new BigDecimal("10.00").compareTo(detalhe.itens().get(0).precoUnitario()));
        assertTrue(carrinho.isFinalizada());

        verify(stockService).registrarSaidaVenda(eq(produto), eq(new BigDecimal("2")), anyString(), eq(1L));
    }

    @Test
    @DisplayName("Pagamento insuficiente é recusado sem dar saída no estoque")
    void recusaPagamentoInsuficiente() {
        CarrinhoVenda carrinho = carrinho("2");
        assertThrows(VendaInvalidaException.class, () -> saleService.finalizarVenda(
                carrinho, List.of(pagamento(FormaPagamento.PIX, "10.00", null)), "Maria"));

        verifyNoInteractions(stockService);
        assertFalse(carrinho.isFinalizada(), "carrinho deve continuar disponível para nova tentativa");
    }

    @Test
    @DisplayName("Pagamento acima do total é recusado")
    void recusaPagamentoExcedente() {
        CarrinhoVenda carrinho = carrinho("2");
        assertThrows(VendaInvalidaException.class, () -> saleService.finalizarVenda(
                carrinho, List.of(pagamento(FormaPagamento.CREDITO, "30.00", null)), "Maria"));
        verifyNoInteractions(stockService);
    }

    @Test
    @DisplayName("PIX no valor exato finaliza sem troco")
    void finalizaComPixExato() {
        CarrinhoVenda carrinho = carrinho("2");
        VendaDetalhe detalhe = saleService.finalizarVenda(
                carrinho, List.of(pagamento(FormaPagamento.PIX, "20.00", null)), "Maria");

        assertEquals(0, BigDecimal.ZERO.compareTo(detalhe.troco()));
        assertEquals(0, new BigDecimal("20.00").compareTo(detalhe.valorRecebido()));
    }

    @Test
    @DisplayName("Pagamento combinado débito + crédito finaliza")
    void finalizaComDebitoCredito() {
        CarrinhoVenda carrinho = carrinho("2");
        VendaDetalhe detalhe = saleService.finalizarVenda(carrinho, List.of(
                pagamento(FormaPagamento.DEBITO, "12.00", null),
                pagamento(FormaPagamento.CREDITO, "8.00", null)), "Maria");

        assertEquals(2, detalhe.pagamentos().size());
        assertEquals(0, new BigDecimal("20.00").compareTo(detalhe.valorRecebido()));
    }

    @Test
    @DisplayName("Combinado dinheiro + PIX calcula troco apenas do dinheiro")
    void finalizaCombinadoComTroco() {
        CarrinhoVenda carrinho = carrinho("2");
        VendaDetalhe detalhe = saleService.finalizarVenda(carrinho, List.of(
                pagamento(FormaPagamento.DINHEIRO, "12.00", "15.00"),
                pagamento(FormaPagamento.PIX, "8.00", null)), "Maria");

        assertEquals(0, new BigDecimal("23.00").compareTo(detalhe.valorRecebido()));
        assertEquals(0, new BigDecimal("3.00").compareTo(detalhe.troco()));
    }

    @Test
    @DisplayName("Dinheiro recebido menor que o aplicado é recusado")
    void recusaDinheiroRecebidoMenor() {
        CarrinhoVenda carrinho = carrinho("2");
        assertThrows(VendaInvalidaException.class, () -> saleService.finalizarVenda(
                carrinho, List.of(pagamento(FormaPagamento.DINHEIRO, "20.00", "15.00")), "Maria"));
    }

    @Test
    @DisplayName("Carrinho vazio não pode ser finalizado")
    void recusaCarrinhoVazio() {
        assertThrows(VendaInvalidaException.class, () -> saleService.finalizarVenda(
                new CarrinhoVenda(), List.of(pagamento(FormaPagamento.PIX, "10.00", null)), "Maria"));
    }

    @Test
    @DisplayName("Produto inativado após adicionar é recusado na finalização")
    void revalidaProdutoInativo() {
        CarrinhoVenda carrinho = carrinho("2");
        produto.setStatus("INATIVO");

        assertThrows(VendaInvalidaException.class, () -> saleService.finalizarVenda(
                carrinho, List.of(pagamento(FormaPagamento.PIX, "20.00", null)), "Maria"));
        verifyNoInteractions(stockService);
        assertFalse(carrinho.isFinalizada());
    }

    @Test
    @DisplayName("Preço alterado após adicionar é recusado na finalização")
    void revalidaPrecoAlterado() {
        CarrinhoVenda carrinho = carrinho("2");
        produto.setPrecoVenda(new BigDecimal("12.00"));

        assertThrows(VendaInvalidaException.class, () -> saleService.finalizarVenda(
                carrinho, List.of(pagamento(FormaPagamento.PIX, "20.00", null)), "Maria"));
        verifyNoInteractions(stockService);
    }

    @Test
    @DisplayName("Não permite finalizar a mesma venda duas vezes")
    void bloqueiaFinalizacaoDuplicada() {
        CarrinhoVenda carrinho = carrinho("2");
        saleService.finalizarVenda(carrinho,
                List.of(pagamento(FormaPagamento.PIX, "20.00", null)), "Maria");

        assertThrows(VendaInvalidaException.class, () -> saleService.finalizarVenda(carrinho,
                List.of(pagamento(FormaPagamento.PIX, "20.00", null)), "Maria"));
        verify(stockService, times(1)).registrarSaidaVenda(any(), any(), anyString(), any());
    }
}
