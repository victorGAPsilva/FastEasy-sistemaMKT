package com.fasteasy.service;

import com.fasteasy.domain.Category;
import com.fasteasy.domain.Product;
import com.fasteasy.domain.Supplier;
import com.fasteasy.domain.UnidadeMedida;
import com.fasteasy.repository.CategoryRepository;
import com.fasteasy.repository.ProductRepository;
import com.fasteasy.repository.SupplierRepository;
import com.fasteasy.validator.BarcodeValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Serviço de aplicação para operações com Product.
 * Contém todas as regras de negócio do cadastro de produtos.
 */
@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          SupplierRepository supplierRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
    }

    /**
     * Cadastra um novo produto aplicando todas as regras de negócio.
     */
    public Product cadastrar(Product product) {
        validarRegrasBasicas(product);
        validarUnicidade(product, null);
        resolverRelacionamentos(product);

        product.setId(null);
        product.setNome(product.getNome().trim());
        if (product.getStatus() == null) {
            product.setStatus("ATIVO");
        }
        if (product.getUnidadeMedida() == null) {
            product.setUnidadeMedida(UnidadeMedida.UN);
        }
        // O estoque inicial é sempre zero: entradas são registradas pelo StockService.
        product.setEstoqueAtual(BigDecimal.ZERO);
        product.setDataCriacao(LocalDateTime.now());
        product.setDataAtualizacao(null);

        return productRepository.save(product);
    }

    /**
     * Edita um produto existente aplicando todas as regras de negócio.
     */
    public Product editar(Long id, Product dados) {
        Product produto = buscarOuFalhar(id);

        validarRegrasBasicas(dados);
        validarUnicidade(dados, id);
        resolverRelacionamentos(dados);

        produto.setNome(dados.getNome().trim());
        produto.setDescricao(dados.getDescricao());
        produto.setCodigoBarras(dados.getCodigoBarras());
        produto.setSku(dados.getSku());
        produto.setCategoria(dados.getCategoria());
        produto.setMarca(dados.getMarca());
        produto.setUnidadeMedida(dados.getUnidadeMedida());
        produto.setCustoAquisicao(dados.getCustoAquisicao());
        produto.setPrecoVenda(dados.getPrecoVenda());
        produto.setEstoqueMinimo(dados.getEstoqueMinimo());
        produto.setEstoqueMaximo(dados.getEstoqueMaximo());
        produto.setFornecedorPrincipal(dados.getFornecedorPrincipal());
        if (dados.getStatus() != null) {
            produto.setStatus(dados.getStatus());
        }
        produto.setDataAtualizacao(LocalDateTime.now());

        return productRepository.save(produto);
    }

    /**
     * Desativa um produto (exclusão lógica).
     * Produtos já utilizados não são removidos fisicamente; apenas inativados.
     */
    public void desativar(Long id) {
        Product produto = buscarOuFalhar(id);
        if ("INATIVO".equals(produto.getStatus())) {
            throw new IllegalArgumentException("Produto já está inativo");
        }
        produto.setStatus("INATIVO");
        produto.setDataAtualizacao(LocalDateTime.now());
        productRepository.save(produto);
    }

    /**
     * Reativa um produto.
     */
    public void ativar(Long id) {
        Product produto = buscarOuFalhar(id);
        produto.setStatus("ATIVO");
        produto.setDataAtualizacao(LocalDateTime.now());
        productRepository.save(produto);
    }

    @Transactional(readOnly = true)
    public Optional<Product> buscarPorId(Long id) {
        return productRepository.findById(id);
    }

    /**
     * Busca um produto pelo código de barras ou, não encontrando, pelo SKU.
     * Usado pela leitura do código de barras no PDV.
     */
    @Transactional(readOnly = true)
    public Optional<Product> buscarPorCodigo(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            return Optional.empty();
        }
        String c = codigo.trim();
        Optional<Product> produto = productRepository.findByCodigoBarras(c);
        if (produto.isEmpty()) {
            produto = productRepository.findBySku(c);
        }
        return produto;
    }

    @Transactional(readOnly = true)
    public List<Product> listarTodos() {
        return productRepository.findAllByOrderByNomeAsc();
    }

    /**
     * Lista somente produtos ativos, opção para operações de venda.
     */
    @Transactional(readOnly = true)
    public List<Product> listarAtivosParaVenda() {
        return productRepository.listarAtivosParaVenda();
    }

    @Transactional(readOnly = true)
    public List<Product> pesquisar(String termo, String status, Long categoriaId) {
        String t = (termo == null || termo.trim().isEmpty()) ? null : termo.trim();
        String s = (status == null || status.trim().isEmpty() || "TODOS".equals(status)) ? null : status.trim();
        return productRepository.buscar(t, s, categoriaId);
    }

    @Transactional(readOnly = true)
    public long contarInativos() {
        return productRepository.countByStatus("INATIVO");
    }

    /**
     * Calcula a margem bruta percentual de forma segura.
     * Retorna null quando o custo não é maior que zero.
     */
    public BigDecimal calcularMargemBruta(BigDecimal precoVenda, BigDecimal custo) {
        if (precoVenda == null || custo == null
                || custo.compareTo(BigDecimal.ZERO) <= 0
                || precoVenda.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return precoVenda.subtract(custo)
                .divide(precoVenda, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Product buscarOuFalhar(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado com ID: " + id));
    }

    private void resolverRelacionamentos(Product product) {
        if (product.getCategoria() == null || product.getCategoria().getId() == null) {
            throw new IllegalArgumentException("A categoria é obrigatória");
        }
        Category categoria = categoryRepository.findById(product.getCategoria().getId())
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada"));
        if (!"ATIVO".equals(categoria.getStatus())) {
            throw new IllegalArgumentException("A categoria selecionada está inativa");
        }
        product.setCategoria(categoria);

        if (product.getFornecedorPrincipal() != null && product.getFornecedorPrincipal().getId() != null) {
            Supplier fornecedor = supplierRepository.findById(product.getFornecedorPrincipal().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Fornecedor não encontrado"));
            if (!"ATIVO".equals(fornecedor.getStatus())) {
                throw new IllegalArgumentException("O fornecedor selecionado está inativo");
            }
            product.setFornecedorPrincipal(fornecedor);
        } else {
            product.setFornecedorPrincipal(null);
        }
    }

    private void validarRegrasBasicas(Product p) {
        if (p == null) {
            throw new IllegalArgumentException("O produto é obrigatório");
        }
        // Regra 1
        if (p.getNome() == null || p.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do produto é obrigatório");
        }
        // Regra 2
        if (p.getPrecoVenda() == null || p.getPrecoVenda().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O preço de venda deve ser maior que zero");
        }
        // Regra 3
        if (p.getCustoAquisicao() != null && p.getCustoAquisicao().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("O custo não pode ser negativo");
        }
        // Regra 4
        if (p.getEstoqueMinimo() != null && p.getEstoqueMinimo() < 0) {
            throw new IllegalArgumentException("O estoque mínimo não pode ser negativo");
        }
        if (p.getEstoqueMinimo() == null) {
            p.setEstoqueMinimo(0);
        }
        // Regra 5
        if (p.getEstoqueMaximo() != null
                && p.getEstoqueMaximo().compareTo(BigDecimal.valueOf(p.getEstoqueMinimo())) < 0) {
            throw new IllegalArgumentException("O estoque máximo não pode ser menor que o mínimo");
        }
        // Regra 7 - código de barras (EAN com DV ou código interno válido)
        if (p.getCodigoBarras() != null && !p.getCodigoBarras().trim().isEmpty()) {
            if (!BarcodeValidator.isCodigoValido(p.getCodigoBarras())) {
                throw new IllegalArgumentException("Código de barras inválido");
            }
        }
    }

    private void validarUnicidade(Product p, Long idAtual) {
        // Regra 6 - SKU único quando informado
        if (p.getSku() != null && !p.getSku().trim().isEmpty()) {
            productRepository.findBySku(p.getSku().trim()).ifPresent(outro -> {
                if (idAtual == null || !outro.getId().equals(idAtual)) {
                    throw new IllegalArgumentException("SKU já cadastrado para outro produto: " + p.getSku());
                }
            });
        }
        // Regra 7 - código de barras único quando informado
        if (p.getCodigoBarras() != null && !p.getCodigoBarras().trim().isEmpty()) {
            productRepository.findByCodigoBarras(p.getCodigoBarras().trim()).ifPresent(outro -> {
                if (idAtual == null || !outro.getId().equals(idAtual)) {
                    throw new IllegalArgumentException("Código de barras já cadastrado para outro produto: " + p.getCodigoBarras());
                }
            });
        }
    }
}
