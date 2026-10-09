package com.fasteasy.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidade Product - representa produtos do supermercado.
 * Utiliza BigDecimal para valores monetários.
 * Suporta ativação/desativação e unidades de medida padronizadas.
 */
@Entity
@Table(name = "products")
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome do produto é obrigatório")
    @Size(max = 150, message = "O nome do produto deve ter no máximo 150 caracteres")
    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "descricao", length = 500)
    private String descricao;

    @Column(name = "codigo_barras", length = 50, unique = true)
    private String codigoBarras;

    @Column(name = "sku", length = 50, unique = true)
    private String sku;

    @NotNull(message = "A categoria é obrigatória")
    @ManyToOne
    @JoinColumn(name = "categoria_id", nullable = false)
    private Category categoria;

    @Column(name = "marca", length = 100)
    private String marca;

    @NotNull(message = "A unidade de medida é obrigatória")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "unidade_medida", nullable = false, length = 10)
    private UnidadeMedida unidadeMedida;

    @DecimalMin(value = "0.0", message = "O custo não pode ser negativo")
    @Column(name = "custo_aquisicao", precision = 19, scale = 4)
    private BigDecimal custoAquisicao;

    @NotNull(message = "O preço de venda é obrigatório")
    @DecimalMin(value = "0.0", inclusive = false, message = "O preço de venda deve ser maior que zero")
    @Column(name = "preco_venda", precision = 19, scale = 4, nullable = false)
    private BigDecimal precoVenda;

    @PositiveOrZero(message = "O estoque mínimo não pode ser negativo")
    @Column(name = "estoque_minimo", nullable = false)
    private Integer estoqueMinimo;

    @Column(name = "estoque_maximo", precision = 19, scale = 4)
    private BigDecimal estoqueMaximo;

    @ManyToOne
    @JoinColumn(name = "fornecedor_principal_id")
    private Supplier fornecedorPrincipal;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    // Construtores
    public Product() {
        this.status = "ATIVO";
        this.unidadeMedida = UnidadeMedida.UN;
        this.estoqueMinimo = 0;
        this.dataCriacao = LocalDateTime.now();
    }

    public Product(String nome) {
        this();
        this.nome = nome;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Category getCategoria() {
        return categoria;
    }

    public void setCategoria(Category categoria) {
        this.categoria = categoria;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public UnidadeMedida getUnidadeMedida() {
        return unidadeMedida;
    }

    public void setUnidadeMedida(UnidadeMedida unidadeMedida) {
        this.unidadeMedida = unidadeMedida;
    }

    public BigDecimal getCustoAquisicao() {
        return custoAquisicao;
    }

    public void setCustoAquisicao(BigDecimal custoAquisicao) {
        this.custoAquisicao = custoAquisicao;
    }

    public BigDecimal getPrecoVenda() {
        return precoVenda;
    }

    public void setPrecoVenda(BigDecimal precoVenda) {
        this.precoVenda = precoVenda;
    }

    public Integer getEstoqueMinimo() {
        return estoqueMinimo;
    }

    public void setEstoqueMinimo(Integer estoqueMinimo) {
        this.estoqueMinimo = estoqueMinimo;
    }

    public BigDecimal getEstoqueMaximo() {
        return estoqueMaximo;
    }

    public void setEstoqueMaximo(BigDecimal estoqueMaximo) {
        this.estoqueMaximo = estoqueMaximo;
    }

    public Supplier getFornecedorPrincipal() {
        return fornecedorPrincipal;
    }

    public void setFornecedorPrincipal(Supplier fornecedorPrincipal) {
        this.fornecedorPrincipal = fornecedorPrincipal;
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

    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }

    public void setDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }

    @PreUpdate
    protected void onUpdate() {
        this.dataAtualizacao = LocalDateTime.now();
    }

    public boolean isAtivo() {
        return "ATIVO".equals(status);
    }

    public void desativar() {
        this.status = "INATIVO";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", sku='" + sku + '\'' +
                ", codigoBarras='" + codigoBarras + '\'' +
                ", categoria=" + categoria +
                ", marca='" + marca + '\'' +
                ", unidadeMedida=" + unidadeMedida +
                ", custoAquisicao=" + custoAquisicao +
                ", precoVenda=" + precoVenda +
                ", estoqueMinimo=" + estoqueMinimo +
                ", status='" + status + '\'' +
                '}';
    }
}