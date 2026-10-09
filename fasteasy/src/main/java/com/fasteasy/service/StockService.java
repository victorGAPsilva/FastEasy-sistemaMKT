package com.fasteasy.service;

import com.fasteasy.domain.Dinheiro;
import com.fasteasy.domain.EstoqueInsuficienteException;
import com.fasteasy.domain.Product;
import com.fasteasy.domain.StockMovement;
import com.fasteasy.domain.TipoMovimentacaoEstoque;
import com.fasteasy.domain.VendaInvalidaException;
import com.fasteasy.repository.ProductRepository;
import com.fasteasy.repository.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Serviço de aplicação responsável pelo estoque.
 *
 * <p>Toda alteração de quantidade passa por aqui: a interface e o PDV nunca
 * atualizam o estoque diretamente. Cada operação gera uma
 * {@link StockMovement} auditável.</p>
 *
 * <p>Para evitar que duas vendas consumam o mesmo item simultaneamente, o
 * produto é carregado com bloqueio pessimista de escrita
 * ({@code PESSIMISTIC_WRITE}) antes de qualquer alteração. O estoque nunca
 * fica negativo.</p>
 */
@Service
@Transactional
public class StockService {

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;

    public StockService(ProductRepository productRepository,
                        StockMovementRepository stockMovementRepository) {
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    /** Registra uma entrada de estoque (ex.: recebimento de compra). */
    public StockMovement registrarEntrada(Long produtoId, BigDecimal quantidade, String motivo) {
        Product produto = buscarParaAtualizacao(produtoId);
        return movimentar(produto, quantidade, TipoMovimentacaoEstoque.ENTRADA, motivo, null);
    }

    /** Registra uma saída de estoque avulsa (não vinculada a venda). */
    public StockMovement registrarSaida(Long produtoId, BigDecimal quantidade, String motivo) {
        Product produto = buscarParaAtualizacao(produtoId);
        return movimentar(produto, quantidade, TipoMovimentacaoEstoque.SAIDA, motivo, null);
    }

    /**
     * Registra a saída de estoque de uma venda.
     *
     * <p>Recebe o produto já carregado (e bloqueado) pelo serviço de venda,
     * garantindo que o bloqueio ocorra uma única vez, na mesma transação.</p>
     */
    public StockMovement registrarSaidaVenda(Product produto, BigDecimal quantidade, String motivo, Long vendaId) {
        if (produto == null) {
            throw new VendaInvalidaException("O produto é obrigatório para a saída de estoque");
        }
        return movimentar(produto, quantidade, TipoMovimentacaoEstoque.SALE_EXIT, motivo, vendaId);
    }

    /** Ajusta o estoque para uma quantidade absoluta informada. */
    public StockMovement ajustarEstoque(Long produtoId, BigDecimal novaQuantidade, String motivo) {
        if (novaQuantidade == null || novaQuantidade.signum() < 0) {
            throw new VendaInvalidaException("A quantidade de ajuste não pode ser negativa");
        }
        Product produto = buscarParaAtualizacao(produtoId);
        BigDecimal anterior = valorAtual(produto);
        BigDecimal delta = novaQuantidade.subtract(anterior).abs();
        produto.setEstoqueAtual(novaQuantidade);
        produto.setDataAtualizacao(LocalDateTime.now());
        productRepository.save(produto);
        return salvarMovimento(produto, TipoMovimentacaoEstoque.AJUSTE, delta, anterior, novaQuantidade, motivo, null);
    }

    @Transactional(readOnly = true)
    public BigDecimal consultarEstoque(Long produtoId) {
        Product produto = productRepository.findById(produtoId)
                .orElseThrow(() -> new VendaInvalidaException("Produto não encontrado: " + produtoId));
        return valorAtual(produto);
    }

    @Transactional(readOnly = true)
    public List<StockMovement> listarMovimentacoes(Long produtoId) {
        return stockMovementRepository.findByProdutoIdOrderByDataMovimentacaoDesc(produtoId);
    }

    /**
     * Carrega o produto com bloqueio pessimista de escrita (para uso dentro de
     * transações maiores, como a finalização de uma venda).
     */
    public Product buscarParaAtualizacao(Long produtoId) {
        if (produtoId == null) {
            throw new VendaInvalidaException("O produto é obrigatório");
        }
        return productRepository.buscarParaAtualizacao(produtoId)
                .orElseThrow(() -> new VendaInvalidaException("Produto não encontrado: " + produtoId));
    }

    private StockMovement movimentar(Product produto,
                                     BigDecimal quantidade,
                                     TipoMovimentacaoEstoque tipo,
                                     String motivo,
                                     Long vendaId) {
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new VendaInvalidaException("A quantidade deve ser maior que zero");
        }

        BigDecimal anterior = valorAtual(produto);
        BigDecimal posterior;

        if (tipo == TipoMovimentacaoEstoque.ENTRADA) {
            posterior = anterior.add(quantidade);
        } else {
            posterior = anterior.subtract(quantidade);
            if (posterior.signum() < 0) {
                throw new EstoqueInsuficienteException(String.format(
                        "Estoque insuficiente para \"%s\". Disponível: %s, solicitado: %s",
                        produto.getNome(),
                        Dinheiro.formatarQuantidade(anterior),
                        Dinheiro.formatarQuantidade(quantidade)));
            }
        }

        produto.setEstoqueAtual(posterior);
        produto.setDataAtualizacao(LocalDateTime.now());
        productRepository.save(produto);

        return salvarMovimento(produto, tipo, quantidade, anterior, posterior, motivo, vendaId);
    }

    private StockMovement salvarMovimento(Product produto,
                                          TipoMovimentacaoEstoque tipo,
                                          BigDecimal quantidade,
                                          BigDecimal anterior,
                                          BigDecimal posterior,
                                          String motivo,
                                          Long vendaId) {
        StockMovement movimentacao = new StockMovement();
        movimentacao.setProduto(produto);
        movimentacao.setTipo(tipo);
        movimentacao.setQuantidade(quantidade);
        movimentacao.setQuantidadeAnterior(anterior);
        movimentacao.setQuantidadePosterior(posterior);
        movimentacao.setMotivo(motivo);
        movimentacao.setVendaId(vendaId);
        movimentacao.setDataMovimentacao(LocalDateTime.now());
        return stockMovementRepository.save(movimentacao);
    }

    private BigDecimal valorAtual(Product produto) {
        return produto.getEstoqueAtual() != null ? produto.getEstoqueAtual() : BigDecimal.ZERO;
    }
}
