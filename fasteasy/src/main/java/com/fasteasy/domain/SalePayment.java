package com.fasteasy.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade SalePayment - pagamento de uma venda.
 *
 * <p>{@link #valor} é o valor efetivamente aplicado à compra (soma dos
 * pagamentos deve ser igual ao total). {@link #valorRecebido} é o valor
 * entregue pelo cliente — para dinheiro pode ser maior que {@link #valor},
 * gerando troco. Não há confirmação externa de transação.</p>
 */
@Entity
@Table(name = "sale_payments")
public class SalePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venda_id", nullable = false)
    private Sale venda;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "forma_pagamento", nullable = false, length = 20)
    private FormaPagamento formaPagamento;

    /** Valor aplicado à venda. */
    @Column(name = "valor", precision = 19, scale = 4, nullable = false)
    private BigDecimal valor;

    /** Valor entregue pelo cliente (igual a {@link #valor} exceto em dinheiro). */
    @Column(name = "valor_recebido", precision = 19, scale = 4, nullable = false)
    private BigDecimal valorRecebido;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    public SalePayment() {
        this.dataHora = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Sale getVenda() {
        return venda;
    }

    public void setVenda(Sale venda) {
        this.venda = venda;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(FormaPagamento formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public BigDecimal getValorRecebido() {
        return valorRecebido;
    }

    public void setValorRecebido(BigDecimal valorRecebido) {
        this.valorRecebido = valorRecebido;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }
}
