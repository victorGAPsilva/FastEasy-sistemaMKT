package com.fasteasy.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Carrinho de venda do PDV, mantido em memória e independente da interface.
 *
 * <p>Concentra todas as regras de negócio do carrinho: validação de produto
 * ativo, preço, quantidade, estoque disponível, descontos e o cálculo de
 * subtotal, desconto total e total. A finalização (persistência atômica) é
 * responsabilidade do {@code SaleService}.</p>
 *
 * <p>Nenhum carrinho incompleto é persistido: a venda só é gravada na
 * finalização.</p>
 */
public class CarrinhoVenda {

    private final List<ItemCarrinho> itens = new ArrayList<>();
    private final AtomicBoolean finalizando = new AtomicBoolean(false);
    private boolean finalizada = false;

    /**
     * Adiciona um produto ao carrinho. Se o produto já existir, incrementa a
     * quantidade (respeitando o estoque disponível).
     */
    public ItemCarrinho adicionar(Product produto, BigDecimal quantidade, BigDecimal estoqueDisponivel) {
        validarProduto(produto);
        validarQuantidade(quantidade);

        BigDecimal disponivel = Dinheiro.ouZero(estoqueDisponivel);
        Optional<ItemCarrinho> existente = buscarItem(produto.getId());

        BigDecimal novaQuantidade = existente
                .map(i -> i.getQuantidade().add(quantidade))
                .orElse(quantidade);

        if (novaQuantidade.compareTo(disponivel) > 0) {
            throw new EstoqueInsuficienteException(String.format(
                    "Estoque insuficiente para \"%s\". Disponível: %s, solicitado: %s",
                    produto.getNome(),
                    Dinheiro.formatarQuantidade(disponivel),
                    Dinheiro.formatarQuantidade(novaQuantidade)));
        }

        if (existente.isPresent()) {
            ItemCarrinho item = existente.get();
            item.setQuantidade(novaQuantidade);
            // Atualiza o preço unitário para o valor atual do cadastro.
            item.setPrecoUnitario(produto.getPrecoVenda());
            return item;
        }

        ItemCarrinho item = new ItemCarrinho(
                produto.getId(),
                produto.getNome(),
                primeiroNaoVazio(produto.getCodigoBarras(), produto.getSku()),
                produto.getUnidadeMedida(),
                produto.getPrecoVenda(),
                quantidade);
        itens.add(item);
        return item;
    }

    /** Define a quantidade de um item existente, validando contra o estoque. */
    public void alterarQuantidade(Long produtoId, BigDecimal novaQuantidade, BigDecimal estoqueDisponivel) {
        ItemCarrinho item = buscarItemOuFalhar(produtoId);
        validarQuantidade(novaQuantidade);
        BigDecimal disponivel = Dinheiro.ouZero(estoqueDisponivel);
        if (novaQuantidade.compareTo(disponivel) > 0) {
            throw new EstoqueInsuficienteException(String.format(
                    "Estoque insuficiente para \"%s\". Disponível: %s, solicitado: %s",
                    item.getNome(),
                    Dinheiro.formatarQuantidade(disponivel),
                    Dinheiro.formatarQuantidade(novaQuantidade)));
        }
        item.setQuantidade(novaQuantidade);
    }

    /** Aumenta a quantidade de um item. */
    public void aumentar(Long produtoId, BigDecimal passo, BigDecimal estoqueDisponivel) {
        ItemCarrinho item = buscarItemOuFalhar(produtoId);
        BigDecimal passoValido = (passo == null || passo.signum() <= 0) ? BigDecimal.ONE : passo;
        alterarQuantidade(produtoId, item.getQuantidade().add(passoValido), estoqueDisponivel);
    }

    /** Diminui a quantidade de um item, impedindo zero ou valores negativos. */
    public void diminuir(Long produtoId, BigDecimal passo) {
        ItemCarrinho item = buscarItemOuFalhar(produtoId);
        BigDecimal passoValido = (passo == null || passo.signum() <= 0) ? BigDecimal.ONE : passo;
        BigDecimal nova = item.getQuantidade().subtract(passoValido);
        if (nova.signum() <= 0) {
            throw new VendaInvalidaException(
                    "A quantidade não pode ser zero ou negativa. Use \"Remover\" para excluir o item.");
        }
        item.setQuantidade(nova);
    }

    /** Remove um item do carrinho. */
    public void remover(Long produtoId) {
        itens.removeIf(i -> i.getProdutoId().equals(produtoId));
    }

    /**
     * Aplica um desconto a um item, validando os limites.
     *
     * <p>No estado atual do sistema não existe módulo de permissões, portanto
     * a interface do PDV mantém a aplicação de desconto desabilitada. Este
     * método existe para a regra de negócio e testes.</p>
     */
    public void aplicarDescontoItem(Long produtoId, BigDecimal desconto) {
        ItemCarrinho item = buscarItemOuFalhar(produtoId);
        if (desconto == null || desconto.signum() < 0) {
            throw new VendaInvalidaException("O desconto não pode ser negativo");
        }
        if (desconto.compareTo(item.getTotalBruto()) > 0) {
            throw new VendaInvalidaException("O desconto não pode ultrapassar o total do item");
        }
        item.setDesconto(Dinheiro.arredondar(desconto));
    }

    /** Esvazia o carrinho e reinicia o estado de finalização. */
    public void limpar() {
        itens.clear();
        finalizada = false;
        finalizando.set(false);
    }

    public Optional<ItemCarrinho> buscarItem(Long produtoId) {
        if (produtoId == null) {
            return Optional.empty();
        }
        return itens.stream().filter(i -> produtoId.equals(i.getProdutoId())).findFirst();
    }

    public ItemCarrinho buscarItemOuFalhar(Long produtoId) {
        return buscarItem(produtoId)
                .orElseThrow(() -> new VendaInvalidaException("Item não encontrado no carrinho"));
    }

    /** Lista imutável dos itens do carrinho. */
    public List<ItemCarrinho> getItens() {
        return Collections.unmodifiableList(itens);
    }

    public boolean isVazio() {
        return itens.isEmpty();
    }

    /** Quantidade total de itens (soma das quantidades). */
    public BigDecimal getQuantidadeTotalItens() {
        return itens.stream()
                .map(ItemCarrinho::getQuantidade)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Soma dos totais brutos dos itens (preço × quantidade), arredondada. */
    public BigDecimal getSubtotal() {
        return Dinheiro.arredondar(itens.stream()
                .map(ItemCarrinho::getTotalBruto)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Soma dos descontos dos itens, arredondada. */
    public BigDecimal getDescontoTotal() {
        return Dinheiro.arredondar(itens.stream()
                .map(ItemCarrinho::getDesconto)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Total final (subtotal − descontos), nunca negativo, arredondado. */
    public BigDecimal getTotal() {
        BigDecimal total = getSubtotal().subtract(getDescontoTotal());
        if (total.signum() < 0) {
            total = BigDecimal.ZERO;
        }
        return Dinheiro.arredondar(total);
    }

    public boolean isFinalizada() {
        return finalizada;
    }

    /**
     * Tenta iniciar a finalização do carrinho de forma atômica,
     * impedindo finalização duplicada ou simultânea.
     *
     * @return {@code true} se esta chamada obteve o direito de finalizar
     */
    public boolean iniciarFinalizacao() {
        if (finalizada) {
            return false;
        }
        return finalizando.compareAndSet(false, true);
    }

    /** Marca a finalização como concluída com sucesso. */
    public void concluirFinalizacao() {
        this.finalizada = true;
    }

    /** Libera a trava de finalização para permitir nova tentativa após falha. */
    public void abortarFinalizacao() {
        this.finalizando.set(false);
    }

    private void validarProduto(Product produto) {
        if (produto == null || produto.getId() == null) {
            throw new VendaInvalidaException("Produto inválido");
        }
        if (!produto.isAtivo()) {
            throw new VendaInvalidaException("O produto \"" + produto.getNome() + "\" está inativo");
        }
        if (produto.getPrecoVenda() == null || produto.getPrecoVenda().signum() < 0) {
            throw new VendaInvalidaException("O preço do produto \"" + produto.getNome() + "\" é inválido");
        }
    }

    private void validarQuantidade(BigDecimal quantidade) {
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new VendaInvalidaException("A quantidade deve ser maior que zero");
        }
    }

    private static String primeiroNaoVazio(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }
}
