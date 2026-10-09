package com.fasteasy.service;

import com.fasteasy.domain.CarrinhoVenda;
import com.fasteasy.domain.Dinheiro;
import com.fasteasy.domain.FormaPagamento;
import com.fasteasy.domain.ItemCarrinho;
import com.fasteasy.domain.Product;
import com.fasteasy.domain.Sale;
import com.fasteasy.domain.SaleItem;
import com.fasteasy.domain.SalePayment;
import com.fasteasy.domain.StatusVenda;
import com.fasteasy.domain.VendaInvalidaException;
import com.fasteasy.repository.ProductRepository;
import com.fasteasy.repository.SaleRepository;
import com.fasteasy.service.dto.ItemVendaDTO;
import com.fasteasy.service.dto.PagamentoDTO;
import com.fasteasy.service.dto.PagamentoVendaDTO;
import com.fasteasy.service.dto.VendaDetalhe;
import com.fasteasy.service.dto.VendaResumo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço de aplicação responsável pelo PDV e pelas vendas.
 *
 * <p>A finalização é totalmente transacional e atômica: valida o carrinho e os
 * pagamentos, revalida os produtos (existência, status, preço e estoque),
 * grava a venda, os itens e os pagamentos e dá baixa no estoque por meio do
 * {@link StockService}. Qualquer falha provoca rollback completo.</p>
 */
@Service
@Transactional
public class SaleService {

    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final StockService stockService;

    public SaleService(ProductRepository productRepository,
                       SaleRepository saleRepository,
                       StockService stockService) {
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.stockService = stockService;
    }

    /**
     * Finaliza uma venda de forma atômica.
     *
     * @param carrinho   carrinho em memória
     * @param pagamentos pagamentos informados
     * @param operador   identificação do operador (opcional)
     * @return detalhe completo da venda gravada
     */
    public VendaDetalhe finalizarVenda(CarrinhoVenda carrinho, List<PagamentoDTO> pagamentos, String operador) {
        if (carrinho == null || carrinho.isVazio()) {
            throw new VendaInvalidaException("O carrinho está vazio");
        }
        if (carrinho.isFinalizada()) {
            throw new VendaInvalidaException("Esta venda já foi finalizada");
        }
        if (!carrinho.iniciarFinalizacao()) {
            throw new VendaInvalidaException("A venda já está sendo finalizada");
        }

        try {
            BigDecimal subtotal = carrinho.getSubtotal();
            BigDecimal desconto = carrinho.getDescontoTotal();
            BigDecimal total = carrinho.getTotal();
            List<PagamentoDTO> pagamentosValidados = validarPagamentos(pagamentos, total);

            LocalDateTime agora = LocalDateTime.now();
            Sale sale = new Sale();
            sale.setDataHora(agora);
            sale.setDataCriacao(agora);
            sale.setStatus(StatusVenda.COMPLETED);
            sale.setSubtotal(subtotal);
            sale.setDescontoTotal(desconto);
            sale.setValorTotal(total);
            sale.setOperador(normalizarOperador(operador));
            sale.setValorRecebido(BigDecimal.ZERO);
            sale.setTroco(BigDecimal.ZERO);

            // Primeira gravação: obtém o ID para compor o número da venda.
            sale = saleRepository.saveAndFlush(sale);
            sale.setNumero(gerarNumero(sale.getId()));

            // Itens: revalida produto (compromisso/bloqueio), preço e estoque.
            for (ItemCarrinho item : carrinho.getItens()) {
                Product produto = productRepository.buscarParaAtualizacao(item.getProdutoId())
                        .orElseThrow(() -> new VendaInvalidaException(
                                "Produto não encontrado: " + item.getNome()));

                if (!produto.isAtivo()) {
                    throw new VendaInvalidaException("O produto \"" + produto.getNome() + "\" está inativo");
                }
                if (produto.getPrecoVenda() == null
                        || produto.getPrecoVenda().compareTo(item.getPrecoUnitario()) != 0) {
                    throw new VendaInvalidaException("O preço do produto \"" + produto.getNome()
                            + "\" foi alterado. Remova e adicione o item novamente.");
                }

                SaleItem saleItem = new SaleItem();
                saleItem.setProduto(produto);
                saleItem.setNomeProduto(produto.getNome());
                saleItem.setCodigoProduto(primeiroNaoVazio(produto.getCodigoBarras(), produto.getSku()));
                saleItem.setQuantidade(item.getQuantidade());
                saleItem.setPrecoUnitario(item.getPrecoUnitario());
                saleItem.setDesconto(item.getDesconto());
                saleItem.setTotal(item.getTotal());
                sale.addItem(saleItem);

                stockService.registrarSaidaVenda(
                        produto, item.getQuantidade(), "Venda " + sale.getNumero(), sale.getId());
            }

            BigDecimal recebido = BigDecimal.ZERO;
            BigDecimal troco = BigDecimal.ZERO;
            for (PagamentoDTO pagamento : pagamentosValidados) {
                SalePayment salePayment = new SalePayment();
                salePayment.setFormaPagamento(pagamento.formaPagamento());
                salePayment.setValor(pagamento.valor());
                salePayment.setValorRecebido(pagamento.valorRecebido());
                salePayment.setDataHora(agora);
                sale.addPagamento(salePayment);

                recebido = recebido.add(pagamento.valorRecebido());
                if (pagamento.formaPagamento() == FormaPagamento.DINHEIRO) {
                    troco = troco.add(pagamento.valorRecebido().subtract(pagamento.valor()));
                }
            }
            sale.setValorRecebido(Dinheiro.arredondar(recebido));
            sale.setTroco(Dinheiro.arredondar(troco));

            sale = saleRepository.saveAndFlush(sale);
            carrinho.concluirFinalizacao();
            return montarDetalhe(sale);
        } catch (RuntimeException e) {
            carrinho.abortarFinalizacao();
            throw e;
        }
    }

    /** Consulta o histórico de vendas com filtros opcionais. */
    @Transactional(readOnly = true)
    public List<VendaResumo> listarHistorico(String numero,
                                             LocalDateTime inicio,
                                             LocalDateTime fim,
                                             StatusVenda status) {
        String n = (numero == null || numero.trim().isEmpty()) ? null : numero.trim();
        return saleRepository.buscarHistorico(n, inicio, fim, status).stream()
                .map(this::montarResumo)
                .collect(Collectors.toList());
    }

    /** Busca o detalhe completo de uma venda (para comprovante/reimpressão). */
    @Transactional(readOnly = true)
    public VendaDetalhe buscarDetalhe(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new VendaInvalidaException("Venda não encontrada: " + id));
        return montarDetalhe(sale);
    }

    @Transactional(readOnly = true)
    public List<VendaResumo> listarTodas() {
        return saleRepository.findAll().stream()
                .sorted((a, b) -> b.getDataHora().compareTo(a.getDataHora()))
                .map(this::montarResumo)
                .collect(Collectors.toList());
    }

    /**
     * Valida os pagamentos e normaliza os valores.
     *
     * <p>A soma dos valores aplicados deve ser exatamente igual ao total.
     * Em dinheiro, o valor recebido não pode ser menor que o aplicado.</p>
     */
    private List<PagamentoDTO> validarPagamentos(List<PagamentoDTO> pagamentos, BigDecimal total) {
        if (pagamentos == null || pagamentos.isEmpty()) {
            throw new VendaInvalidaException("Informe ao menos uma forma de pagamento");
        }
        if (total == null || total.signum() < 0) {
            throw new VendaInvalidaException("O total da venda é inválido");
        }

        List<PagamentoDTO> resultado = new ArrayList<>();
        BigDecimal soma = BigDecimal.ZERO;

        for (PagamentoDTO pagamento : pagamentos) {
            if (pagamento == null || pagamento.formaPagamento() == null) {
                throw new VendaInvalidaException("Forma de pagamento inválida");
            }
            BigDecimal valor = Dinheiro.arredondar(pagamento.valor());
            if (valor == null || valor.signum() <= 0) {
                throw new VendaInvalidaException("O valor do pagamento deve ser maior que zero");
            }

            BigDecimal recebido;
            if (pagamento.formaPagamento() == FormaPagamento.DINHEIRO) {
                recebido = Dinheiro.arredondar(pagamento.valorRecebido());
                if (recebido == null || recebido.compareTo(valor) < 0) {
                    throw new VendaInvalidaException(
                            "O valor recebido em dinheiro não pode ser menor que o valor aplicado");
                }
            } else {
                recebido = valor;
            }

            soma = soma.add(valor);
            resultado.add(new PagamentoDTO(pagamento.formaPagamento(), valor, recebido));
        }

        soma = Dinheiro.arredondar(soma);
        int comparacao = soma.compareTo(total);
        if (comparacao < 0) {
            throw new VendaInvalidaException(
                    "Valor insuficiente. Faltam " + Dinheiro.formatar(total.subtract(soma)));
        }
        if (comparacao > 0) {
            throw new VendaInvalidaException(
                    "O valor dos pagamentos excede o total da venda em " + Dinheiro.formatar(soma.subtract(total)));
        }
        return resultado;
    }

    private VendaDetalhe montarDetalhe(Sale sale) {
        List<ItemVendaDTO> itens = sale.getItens().stream()
                .map(i -> new ItemVendaDTO(
                        i.getNomeProduto(),
                        i.getCodigoProduto(),
                        i.getQuantidade(),
                        i.getPrecoUnitario(),
                        i.getDesconto(),
                        i.getTotal()))
                .collect(Collectors.toList());

        List<PagamentoVendaDTO> pagamentos = sale.getPagamentos().stream()
                .map(p -> {
                    BigDecimal troco = p.getFormaPagamento() == FormaPagamento.DINHEIRO
                            ? Dinheiro.arredondar(Dinheiro.ouZero(p.getValorRecebido()).subtract(Dinheiro.ouZero(p.getValor())))
                            : BigDecimal.ZERO;
                    return new PagamentoVendaDTO(p.getFormaPagamento(), p.getValor(), p.getValorRecebido(), troco);
                })
                .collect(Collectors.toList());

        return new VendaDetalhe(
                sale.getId(),
                sale.getNumero(),
                sale.getDataHora(),
                sale.getStatus(),
                sale.getSubtotal(),
                sale.getDescontoTotal(),
                sale.getValorTotal(),
                sale.getValorRecebido(),
                sale.getTroco(),
                sale.getOperador(),
                itens,
                pagamentos);
    }

    private VendaResumo montarResumo(Sale sale) {
        String formas = sale.getPagamentos().stream()
                .map(p -> p.getFormaPagamento().getDescricao())
                .distinct()
                .collect(Collectors.joining(", "));
        return new VendaResumo(
                sale.getId(),
                sale.getNumero(),
                sale.getDataHora(),
                sale.getValorTotal(),
                formas,
                sale.getStatus(),
                sale.getOperador());
    }

    private String gerarNumero(Long id) {
        return id == null ? null : String.format("%06d", id);
    }

    private String normalizarOperador(String operador) {
        if (operador == null) {
            return null;
        }
        String o = operador.trim();
        return o.isEmpty() ? null : o;
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
