package com.fasteasy.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade Sale - representa uma venda concluída no PDV.
 *
 * <p>Os valores monetários usam {@link BigDecimal}. Os itens e pagamentos
 * preservam os dados históricos do momento da venda (nome, código e preço),
 * de modo que alterações futuras no cadastro dos produtos não modifiquem
 * vendas já registradas.</p>
 */
@Entity
@Table(name = "sales")
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Número da venda (identificador de negócio, ex.: 000123). */
    @Column(name = "numero", length = 20, unique = true)
    private String numero;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 20)
    private StatusVenda status;

    /**
     * Sessão de caixa à qual a venda pertence.
     *
     * <p>Pode ser nula para vendas registradas sem fluxo de caixa (compatível
     * com vendas anteriores à introdução do módulo). Quando a venda é feita
     * pelo PDV com uma sessão aberta, o vínculo é obrigatório e validado dentro
     * da mesma transação de finalização.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_session_id")
    private CashSession cashSession;

    @Column(name = "subtotal", precision = 19, scale = 4, nullable = false)
    private BigDecimal subtotal;

    @Column(name = "desconto_total", precision = 19, scale = 4, nullable = false)
    private BigDecimal descontoTotal;

    @Column(name = "valor_total", precision = 19, scale = 4, nullable = false)
    private BigDecimal valorTotal;

    @Column(name = "valor_recebido", precision = 19, scale = 4, nullable = false)
    private BigDecimal valorRecebido;

    @Column(name = "troco", precision = 19, scale = 4, nullable = false)
    private BigDecimal troco;

    @Column(name = "operador", length = 100)
    private String operador;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<SaleItem> itens = new ArrayList<>();

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<SalePayment> pagamentos = new ArrayList<>();

    public Sale() {
        this.status = StatusVenda.COMPLETED;
        this.dataHora = LocalDateTime.now();
        this.dataCriacao = LocalDateTime.now();
        this.descontoTotal = BigDecimal.ZERO;
        this.valorRecebido = BigDecimal.ZERO;
        this.troco = BigDecimal.ZERO;
    }

    /** Adiciona um item à venda, mantendo o vínculo bidirecional. */
    public void addItem(SaleItem item) {
        item.setVenda(this);
        this.itens.add(item);
    }

    /** Adiciona um pagamento à venda, mantendo o vínculo bidirecional. */
    public void addPagamento(SalePayment pagamento) {
        pagamento.setVenda(this);
        this.pagamentos.add(pagamento);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public StatusVenda getStatus() {
        return status;
    }

    public void setStatus(StatusVenda status) {
        this.status = status;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDescontoTotal() {
        return descontoTotal;
    }

    public void setDescontoTotal(BigDecimal descontoTotal) {
        this.descontoTotal = descontoTotal;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public BigDecimal getValorRecebido() {
        return valorRecebido;
    }

    public void setValorRecebido(BigDecimal valorRecebido) {
        this.valorRecebido = valorRecebido;
    }

    public BigDecimal getTroco() {
        return troco;
    }

    public void setTroco(BigDecimal troco) {
        this.troco = troco;
    }

    public String getOperador() {
        return operador;
    }

    public void setOperador(String operador) {
        this.operador = operador;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public List<SaleItem> getItens() {
        return itens;
    }

    public void setItens(List<SaleItem> itens) {
        this.itens = itens;
    }

    public CashSession getCashSession() {
        return cashSession;
    }

    public void setCashSession(CashSession cashSession) {
        this.cashSession = cashSession;
    }

    public List<SalePayment> getPagamentos() {
        return pagamentos;
    }

    public void setPagamentos(List<SalePayment> pagamentos) {
        this.pagamentos = pagamentos;
    }
}
