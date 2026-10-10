package com.fasteasy.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pagamento financeiro registrado (contas a pagar, despesas).
 *
 * <p>Diferente de {@link SalePayment} (pagamento de venda no PDV), este
 * representa saídas de caixa para quitar obrigações com fornecedores
 * ou despesas operacionais.</p>
 */
@Entity
@Table(name = "financial_payments")
public class FinancialPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_id")
    private PayableAccount conta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parcela_id")
    private PayableInstallment parcela;

    @Column(name = "valor", precision = 19, scale = 4, nullable = false)
    private BigDecimal valor;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pagamento", nullable = false, length = 20)
    private FinancialPaymentMethod metodoPagamento;

    @Column(name = "referencia", length = 100)
    private String referencia;

    @Column(name = "observacoes", length = 500)
    private String observacoes;

    @Column(name = "operador", length = 100)
    private String operador;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    public FinancialPayment() {
        this.status = "CONFIRMED";
        this.dataCriacao = LocalDateTime.now();
        this.dataHora = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PayableAccount getConta() {
        return conta;
    }

    public void setConta(PayableAccount conta) {
        this.conta = conta;
    }

    public PayableInstallment getParcela() {
        return parcela;
    }

    public void setParcela(PayableInstallment parcela) {
        this.parcela = parcela;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public FinancialPaymentMethod getMetodoPagamento() {
        return metodoPagamento;
    }

    public void setMetodoPagamento(FinancialPaymentMethod metodoPagamento) {
        this.metodoPagamento = metodoPagamento;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public String getOperador() {
        return operador;
    }

    public void setOperador(String operador) {
        this.operador = operador;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    @Override
    public String toString() {
        return "FinancialPayment{" +
                "id=" + id +
                ", valor=" + valor +
                ", dataHora=" + dataHora +
                ", metodoPagamento=" + metodoPagamento +
                ", status='" + status + '\'' +
                '}';
    }
}