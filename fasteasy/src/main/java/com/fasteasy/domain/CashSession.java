package com.fasteasy.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade CashSession - sessão de trabalho de um caixa.
 *
 * <p>Representa o período entre a abertura (com um fundo de troco) e o
 * fechamento de um caixa por um operador. Os valores consolidados do
 * fechamento ({@link #saldoEsperado}, {@link #totalContado} e
 * {@link #diferenca}) são gravados apenas no momento do fechamento, evitando
 * totais financeiros redundantes enquanto a sessão está aberta. Os totais
 * parciais são calculados a partir das vendas/pagamentos e das movimentações.</p>
 */
@Entity
@Table(name = "cash_sessions")
public class CashSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caixa_id", nullable = false)
    private CashRegister caixa;

    /** Operador responsável pela sessão (texto livre enquanto não há autenticação). */
    @Column(name = "operador", length = 100)
    private String operador;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 20)
    private StatusSessaoCaixa status;

    @Column(name = "data_abertura", nullable = false)
    private LocalDateTime dataAbertura;

    @Column(name = "data_fechamento")
    private LocalDateTime dataFechamento;

    /** Fundo de troco informado na abertura. */
    @Column(name = "saldo_inicial", precision = 19, scale = 4, nullable = false)
    private BigDecimal saldoInicial;

    /** Saldo esperado em dinheiro apurado no fechamento. */
    @Column(name = "saldo_esperado", precision = 19, scale = 4)
    private BigDecimal saldoEsperado;

    /** Total de dinheiro efetivamente contado no fechamento. */
    @Column(name = "total_contado", precision = 19, scale = 4)
    private BigDecimal totalContado;

    /** Diferença de caixa (contado − esperado). Positivo = sobra; negativo = falta. */
    @Column(name = "diferenca", precision = 19, scale = 4)
    private BigDecimal diferenca;

    @Column(name = "observacoes_abertura", length = 255)
    private String observacoesAbertura;

    @Column(name = "observacoes_fechamento", length = 500)
    private String observacoesFechamento;

    @Column(name = "operador_fechamento", length = 100)
    private String operadorFechamento;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @OneToMany(mappedBy = "sessao", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<CashMovement> movimentos = new ArrayList<>();

    public CashSession() {
        this.status = StatusSessaoCaixa.OPEN;
        this.dataAbertura = LocalDateTime.now();
        this.dataCriacao = LocalDateTime.now();
        this.saldoInicial = BigDecimal.ZERO;
    }

    /** Adiciona uma movimentação à sessão, mantendo o vínculo bidirecional. */
    public void addMovimento(CashMovement movimento) {
        movimento.setSessao(this);
        this.movimentos.add(movimento);
    }

    public boolean isAberta() {
        return status == StatusSessaoCaixa.OPEN;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CashRegister getCaixa() {
        return caixa;
    }

    public void setCaixa(CashRegister caixa) {
        this.caixa = caixa;
    }

    public String getOperador() {
        return operador;
    }

    public void setOperador(String operador) {
        this.operador = operador;
    }

    public StatusSessaoCaixa getStatus() {
        return status;
    }

    public void setStatus(StatusSessaoCaixa status) {
        this.status = status;
    }

    public LocalDateTime getDataAbertura() {
        return dataAbertura;
    }

    public void setDataAbertura(LocalDateTime dataAbertura) {
        this.dataAbertura = dataAbertura;
    }

    public LocalDateTime getDataFechamento() {
        return dataFechamento;
    }

    public void setDataFechamento(LocalDateTime dataFechamento) {
        this.dataFechamento = dataFechamento;
    }

    public BigDecimal getSaldoInicial() {
        return saldoInicial;
    }

    public void setSaldoInicial(BigDecimal saldoInicial) {
        this.saldoInicial = saldoInicial;
    }

    public BigDecimal getSaldoEsperado() {
        return saldoEsperado;
    }

    public void setSaldoEsperado(BigDecimal saldoEsperado) {
        this.saldoEsperado = saldoEsperado;
    }

    public BigDecimal getTotalContado() {
        return totalContado;
    }

    public void setTotalContado(BigDecimal totalContado) {
        this.totalContado = totalContado;
    }

    public BigDecimal getDiferenca() {
        return diferenca;
    }

    public void setDiferenca(BigDecimal diferenca) {
        this.diferenca = diferenca;
    }

    public String getObservacoesAbertura() {
        return observacoesAbertura;
    }

    public void setObservacoesAbertura(String observacoesAbertura) {
        this.observacoesAbertura = observacoesAbertura;
    }

    public String getObservacoesFechamento() {
        return observacoesFechamento;
    }

    public void setObservacoesFechamento(String observacoesFechamento) {
        this.observacoesFechamento = observacoesFechamento;
    }

    public String getOperadorFechamento() {
        return operadorFechamento;
    }

    public void setOperadorFechamento(String operadorFechamento) {
        this.operadorFechamento = operadorFechamento;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public List<CashMovement> getMovimentos() {
        return movimentos;
    }

    public void setMovimentos(List<CashMovement> movimentos) {
        this.movimentos = movimentos;
    }
}
