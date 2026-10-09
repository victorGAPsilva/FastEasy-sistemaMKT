package com.fasteasy.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade CashMovement - movimentação financeira manual de uma sessão de caixa.
 *
 * <p>Representa suprimentos, sangrias e outras entradas/saídas de dinheiro.
 * É imutável após o registro: não há método de edição; correções devem ser
 * feitas por uma movimentação compensatória auditável.</p>
 */
@Entity
@Table(name = "cash_movements")
public class CashMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sessao_id", nullable = false)
    private CashSession sessao;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoMovimentacaoCaixa tipo;

    /** Valor movimentado (sempre positivo). */
    @Column(name = "valor", precision = 19, scale = 4, nullable = false)
    private BigDecimal valor;

    @Column(name = "motivo", nullable = false, length = 255)
    private String motivo;

    @Column(name = "operador", length = 100)
    private String operador;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    public CashMovement() {
        this.dataHora = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CashSession getSessao() {
        return sessao;
    }

    public void setSessao(CashSession sessao) {
        this.sessao = sessao;
    }

    public TipoMovimentacaoCaixa getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimentacaoCaixa tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getOperador() {
        return operador;
    }

    public void setOperador(String operador) {
        this.operador = operador;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }
}
