package com.fasteasy.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasteasy.domain.Purchase;

/**
 * Entidade PayableAccount - representa uma obrigação financeira (conta a pagar).
 *
 * <p>Pode ser originada de uma compra, uma despesa operacional ou registrada manualmente.
 * Mantém o valor original, valor pago acumulado e saldo pendente.</p>
 */
@Entity
@Table(name = "payable_accounts")
public class PayableAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "descricao", nullable = false, length = 255)
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fornecedor_id")
    private Supplier fornecedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id")
    private Purchase compraOrigem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private ExpenseCategory categoria;

    @Column(name = "data_emissao", nullable = false)
    private LocalDate dataEmissao;

    @Column(name = "data_competencia")
    private LocalDate dataCompetencia;

    @Column(name = "valor_original", precision = 19, scale = 4, nullable = false)
    private BigDecimal valorOriginal;

    @Column(name = "valor_pago", precision = 19, scale = 4, nullable = false)
    private BigDecimal valorPago;

    @Column(name = "saldo_pendente", precision = 19, scale = 4, nullable = false)
    private BigDecimal saldoPendente;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PayableAccountStatus status;

    @Column(name = "observacoes", length = 500)
    private String observacoes;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    @OneToMany(mappedBy = "conta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("numeroParcela ASC")
    private List<PayableInstallment> parcelas = new ArrayList<>();

    public PayableAccount() {
        this.status = PayableAccountStatus.OPEN;
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

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Supplier getFornecedor() {
        return fornecedor;
    }

    public void setFornecedor(Supplier fornecedor) {
        this.fornecedor = fornecedor;
    }

    public Purchase getCompraOrigem() {
        return compraOrigem;
    }

    public void setCompraOrigem(Purchase compraOrigem) {
        this.compraOrigem = compraOrigem;
    }

    public ExpenseCategory getCategoria() {
        return categoria;
    }

    public void setCategoria(ExpenseCategory categoria) {
        this.categoria = categoria;
    }

    public LocalDate getDataEmissao() {
        return dataEmissao;
    }

    public void setDataEmissao(LocalDate dataEmissao) {
        this.dataEmissao = dataEmissao;
    }

    public LocalDate getDataCompetencia() {
        return dataCompetencia;
    }

    public void setDataCompetencia(LocalDate dataCompetencia) {
        this.dataCompetencia = dataCompetencia;
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

    public PayableAccountStatus getStatus() {
        return status;
    }

    public void setStatus(PayableAccountStatus status) {
        this.status = status;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }

    public void setDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }

    public List<PayableInstallment> getParcelas() {
        return parcelas;
    }

    public void setParcelas(List<PayableInstallment> parcelas) {
        this.parcelas = parcelas;
    }

    /** Adiciona uma parcela mantendo o vínculo bidirecional. */
    public void addParcela(PayableInstallment parcela) {
        parcela.setConta(this);
        this.parcelas.add(parcela);
    }

    @PreUpdate
    protected void onUpdate() {
        this.dataAtualizacao = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "PayableAccount{" +
                "id=" + id +
                ", descricao='" + descricao + '\'' +
                ", valorOriginal=" + valorOriginal +
                ", saldoPendente=" + saldoPendente +
                ", status=" + status +
                '}';
    }
}