package com.fasteasy.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de unidade do carrinho de venda (sem banco, sem interface).
 */
@DisplayName("Carrinho de venda")
class CarrinhoVendaTest {

    private Product produto(long id, String nome, String preco, String estoque) {
        Product p = new Product(nome);
        p.setId(id);
        p.setPrecoVenda(new BigDecimal(preco));
        p.setEstoqueAtual(new BigDecimal(estoque));
        p.setUnidadeMedida(UnidadeMedida.UN);
        return p;
    }

    private CarrinhoVenda carrinhoCom(Product produto, String quantidade, String estoqueDisponivel) {
        CarrinhoVenda carrinho = new CarrinhoVenda();
        carrinho.adicionar(produto, new BigDecimal(quantidade), new BigDecimal(estoqueDisponivel));
        return carrinho;
    }

    @Test
    @DisplayName("Adiciona um novo item e calcula o total")
    void adicionaItem() {
        CarrinhoVenda carrinho = carrinhoCom(produto(1L, "Arroz", "10.00", "10"), "2", "10");

        assertEquals(1, carrinho.getItens().size());
        assertEquals(0, new BigDecimal("2").compareTo(carrinho.getQuantidadeTotalItens()));
        assertEquals(0, new BigDecimal("20.00").compareTo(carrinho.getSubtotal()));
        assertEquals(0, new BigDecimal("20.00").compareTo(carrinho.getTotal()));
    }

    @Test
    @DisplayName("Adicionar o mesmo produto incrementa a quantidade")
    void adicionaProdutoRepetido() {
        Product p = produto(1L, "Feijão", "8.00", "10");
        CarrinhoVenda carrinho = carrinhoCom(p, "1", "10");
        carrinho.adicionar(p, new BigDecimal("2"), new BigDecimal("10"));

        assertEquals(1, carrinho.getItens().size());
        assertEquals(0, new BigDecimal("3").compareTo(carrinho.getItens().get(0).getQuantidade()));
        assertEquals(0, new BigDecimal("24.00").compareTo(carrinho.getTotal()));
    }

    @Test
    @DisplayName("Não permite quantidade acima do estoque disponível")
    void rejeitaQuantidadeAcimaDoEstoque() {
        Product p = produto(1L, "Leite", "5.00", "1");
        assertThrows(EstoqueInsuficienteException.class,
                () -> carrinhoCom(p, "2", "1"));
    }

    @Test
    @DisplayName("Adicionar produto inativo é rejeitado")
    void rejeitaProdutoInativo() {
        Product p = produto(1L, "Inativo", "5.00", "10");
        p.setStatus("INATIVO");
        assertThrows(VendaInvalidaException.class, () -> carrinhoCom(p, "1", "10"));
    }

    @Test
    @DisplayName("Quantidade deve ser maior que zero")
    void rejeitaQuantidadeInvalida() {
        Product p = produto(1L, "Café", "12.00", "10");
        assertThrows(VendaInvalidaException.class, () -> carrinhoCom(p, "0", "10"));
        assertThrows(VendaInvalidaException.class, () -> carrinhoCom(p, "-1", "10"));
    }

    @Test
    @DisplayName("Alterar quantidade respeita o estoque")
    void alteraQuantidade() {
        CarrinhoVenda carrinho = carrinhoCom(produto(1L, "Açúcar", "4.00", "10"), "1", "10");
        carrinho.alterarQuantidade(1L, new BigDecimal("5"), new BigDecimal("10"));
        assertEquals(0, new BigDecimal("20.00").compareTo(carrinho.getTotal()));

        assertThrows(EstoqueInsuficienteException.class,
                () -> carrinho.alterarQuantidade(1L, new BigDecimal("11"), new BigDecimal("10")));
    }

    @Test
    @DisplayName("Diminuir não pode resultar em quantidade zero ou negativa")
    void diminuirNaoPermiteZero() {
        CarrinhoVenda carrinho = carrinhoCom(produto(1L, "Sal", "3.00", "10"), "1", "10");
        assertThrows(VendaInvalidaException.class, () -> carrinho.diminuir(1L, BigDecimal.ONE));
    }

    @Test
    @DisplayName("Aumentar e remover atualizam o carrinho")
    void aumentaERemove() {
        CarrinhoVenda carrinho = carrinhoCom(produto(1L, "Óleo", "7.00", "10"), "1", "10");
        carrinho.aumentar(1L, BigDecimal.ONE, new BigDecimal("10"));
        assertEquals(0, new BigDecimal("14.00").compareTo(carrinho.getTotal()));

        carrinho.remover(1L);
        assertTrue(carrinho.isVazio());
        assertEquals(0, BigDecimal.ZERO.compareTo(carrinho.getTotal()));
    }

    @Test
    @DisplayName("Desconto é aplicado ao total e validado")
    void aplicaDesconto() {
        CarrinhoVenda carrinho = carrinhoCom(produto(1L, "Biscoito", "10.00", "10"), "2", "10");
        carrinho.aplicarDescontoItem(1L, new BigDecimal("5.00"));

        assertEquals(0, new BigDecimal("5.00").compareTo(carrinho.getDescontoTotal()));
        assertEquals(0, new BigDecimal("15.00").compareTo(carrinho.getTotal()));
        assertEquals(0, new BigDecimal("20.00").compareTo(carrinho.getSubtotal()));
    }

    @Test
    @DisplayName("Desconto não pode ser negativo nem ultrapassar o item")
    void rejeitaDescontoInvalido() {
        CarrinhoVenda carrinho = carrinhoCom(produto(1L, "Queijo", "20.00", "10"), "1", "10");
        assertThrows(VendaInvalidaException.class,
                () -> carrinho.aplicarDescontoItem(1L, new BigDecimal("-1")));
        assertThrows(VendaInvalidaException.class,
                () -> carrinho.aplicarDescontoItem(1L, new BigDecimal("21")));
    }

    @Test
    @DisplayName("Limpar zera os itens e os totais")
    void limparCarrinho() {
        CarrinhoVenda carrinho = carrinhoCom(produto(1L, "Molho", "6.00", "10"), "3", "10");
        carrinho.limpar();

        assertTrue(carrinho.isVazio());
        assertEquals(0, BigDecimal.ZERO.compareTo(carrinho.getSubtotal()));
        assertEquals(0, BigDecimal.ZERO.compareTo(carrinho.getTotal()));
        assertFalse(carrinho.isFinalizada());
    }

    @Test
    @DisplayName("Finalização é protegida contra chamadas duplicadas")
    void bloqueiaFinalizacaoDuplicada() {
        CarrinhoVenda carrinho = carrinhoCom(produto(1L, "Suco", "6.00", "10"), "1", "10");

        assertTrue(carrinho.iniciarFinalizacao());
        assertFalse(carrinho.iniciarFinalizacao(), "segunda chamada deve ser bloqueada");

        carrinho.abortarFinalizacao();
        assertTrue(carrinho.iniciarFinalizacao(), "após abortar deve permitir nova tentativa");

        carrinho.concluirFinalizacao();
        assertTrue(carrinho.isFinalizada());
        assertFalse(carrinho.iniciarFinalizacao());
    }
}
