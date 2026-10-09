package com.fasteasy.service;

import com.fasteasy.domain.EstoqueInsuficienteException;
import com.fasteasy.domain.Product;
import com.fasteasy.domain.StockMovement;
import com.fasteasy.domain.TipoMovimentacaoEstoque;
import com.fasteasy.domain.VendaInvalidaException;
import com.fasteasy.repository.ProductRepository;
import com.fasteasy.repository.StockMovementRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes de unidade do StockService (repositórios simulados).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Serviço de estoque")
class StockServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @InjectMocks
    private StockService stockService;

    private Product produto;

    @BeforeEach
    void setUp() {
        produto = new Product("Arroz");
        produto.setId(1L);
        produto.setPrecoVenda(new BigDecimal("10.00"));
        produto.setEstoqueAtual(new BigDecimal("5"));
        when(productRepository.buscarParaAtualizacao(1L)).thenReturn(Optional.of(produto));
        when(stockMovementRepository.save(any(StockMovement.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Entrada soma à quantidade atual")
    void registraEntrada() {
        StockMovement mov = stockService.registrarEntrada(1L, new BigDecimal("3"), "Compra");

        assertEquals(0, new BigDecimal("8").compareTo(produto.getEstoqueAtual()));
        assertEquals(TipoMovimentacaoEstoque.ENTRADA, mov.getTipo());
        assertEquals(0, new BigDecimal("5").compareTo(mov.getQuantidadeAnterior()));
        assertEquals(0, new BigDecimal("8").compareTo(mov.getQuantidadePosterior()));
    }

    @Test
    @DisplayName("Saída subtrai da quantidade atual")
    void registraSaida() {
        StockMovement mov = stockService.registrarSaida(1L, new BigDecimal("2"), "Perda");

        assertEquals(0, new BigDecimal("3").compareTo(produto.getEstoqueAtual()));
        assertEquals(TipoMovimentacaoEstoque.SAIDA, mov.getTipo());
    }

    @Test
    @DisplayName("Saída por venda usa tipo SALE_EXIT e guarda a venda")
    void registraSaidaVenda() {
        StockMovement mov = stockService.registrarSaidaVenda(produto, new BigDecimal("2"), "Venda 000001", 99L);

        assertEquals(TipoMovimentacaoEstoque.SALE_EXIT, mov.getTipo());
        assertEquals(99L, mov.getVendaId());
        assertEquals(0, new BigDecimal("3").compareTo(produto.getEstoqueAtual()));
    }

    @Test
    @DisplayName("Saída maior que o estoque é recusada e não altera o estoque")
    void recusaSaidaSemEstoque() {
        assertThrows(EstoqueInsuficienteException.class,
                () -> stockService.registrarSaida(1L, new BigDecimal("6"), "Perda"));
        assertEquals(0, new BigDecimal("5").compareTo(produto.getEstoqueAtual()));
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ajuste define a quantidade absoluta")
    void ajustaEstoque() {
        StockMovement mov = stockService.ajustarEstoque(1L, new BigDecimal("12"), "Inventário");

        assertEquals(0, new BigDecimal("12").compareTo(produto.getEstoqueAtual()));
        assertEquals(TipoMovimentacaoEstoque.AJUSTE, mov.getTipo());
        assertEquals(0, new BigDecimal("7").compareTo(mov.getQuantidade()));
    }

    @Test
    @DisplayName("Quantidade de movimentação deve ser maior que zero")
    void recusaQuantidadeInvalida() {
        assertThrows(VendaInvalidaException.class,
                () -> stockService.registrarEntrada(1L, BigDecimal.ZERO, "x"));
        assertThrows(VendaInvalidaException.class,
                () -> stockService.registrarSaida(1L, new BigDecimal("-1"), "x"));
    }

    @Test
    @DisplayName("Produto inexistente gera erro de negócio")
    void produtoInexistente() {
        when(productRepository.buscarParaAtualizacao(2L)).thenReturn(Optional.empty());
        assertThrows(VendaInvalidaException.class,
                () -> stockService.registrarEntrada(2L, BigDecimal.ONE, "x"));
    }
}
