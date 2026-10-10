package com.fasteasy.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Parcela de uma conta a pagar.
 *
 * <p>Uma conta pode ter uma ou várias parcelas. A soma dos valores das parcelas
 * deve corresponder ao valor total da obrigação.</p>
 */
@Entity
@Table(name = "payable_installments")
public class PayableInstallment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conta_id", nullable = false)
    private PayableAccount conta;

    @Column(name = "numero_parcela", nullable = false)
    private Integer numeroParcela;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Column(name = "valor_original", precision = 19, scale = 4, nullable = false)
    private BigDecimal valorOriginal;

    @Column(name = "valor_pago", precision = 19, scale = 4, nullable = false)
    private BigDecimal valorPago;

    @Column(name = "saldo_pendente", precision = 19, scale = 4, nullable = false)
    private BigDecimal saldoPendente;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PayableInstallmentStatus status;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    public PayableInstallment() {
        this.status = PayableInstallmentStatus.OPEN;
        this.dataCriacao = LocalDateTime.now();
        this.valorPago = BigDecimal.ZERO;
        this.saldoPendente = BigDecimal.ZERO;
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

    public Integer getNumeroParcela() {
        return numeroParcela;
    }

    public void setNumeroParcela(Integer numeroParcela) {
        this.numeroParcela = numeroParcela;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public void setDataVencimento(LocalDate dataVencimento) {
        this.dataVencimento = dataVencimento;
    }

    public BigDecimal getValorOriginal() {
        return valorOriginal;
    }

    public void setValorOriginal(BigDecimal valorOriginal) {
        this.valorOriginal = valorOriginal;
    }

    public BigDecimal getValorPago() {
        return valorPago;
    }

    public void setValorPago(BigDecimal valorPago) {
        this.valorPago = valorPago;
    }

    public BigDecimal getSaldoPendente() {
        return saldoPendente;
    }

    public void setSaldoPendente(BigDecimal saldoPendente) {
        this.saldoPendente = saldoPendente;
    }

    public PayableInstallmentStatus getStatus() {
        return status;
    }

    public void setStatus(PayableInstallmentStatus status) {
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
        return "PayableInstallment{" +
                "id=" + id +
                ", numeroParcela=" + numeroParcela +
                ", dataVencimento=" + dataVencimento +
                ", valorOriginal=" + valorOriginal +
                ", saldoPendente=" + saldoPendente +
                ", status=" + status +
                '}';
    }
}