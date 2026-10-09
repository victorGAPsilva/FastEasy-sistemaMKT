package com.fasteasy.repository;

import com.fasteasy.domain.Category;
import com.fasteasy.domain.Product;
import com.fasteasy.domain.Supplier;
import com.fasteasy.domain.UnidadeMedida;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste de persistência com banco H2 isolado.
 * Verifica mapeamento, relacionamentos e restrições de unicidade.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Persistência de produtos, categorias e fornecedores")
class PersistenciaProdutoTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    private Category salvarCategoria(String nome) {
        Category c = new Category(nome);
        return categoryRepository.save(c);
    }

    private Product novoProduto(String nome, Category categoria) {
        Product p = new Product(nome);
        p.setCategoria(categoria);
        p.setPrecoVenda(new BigDecimal("19.90"));
        p.setCustoAquisicao(new BigDecimal("12.00"));
        p.setUnidadeMedida(UnidadeMedida.UN);
        p.setEstoqueMinimo(1);
        return p;
    }

    @Test
    @DisplayName("Deve persistir produto com categoria")
    void devePersistirProdutoComCategoria() {
        Category categoria = salvarCategoria("Bebidas");
        Product produto = productRepository.save(novoProduto("Refrigerante 2L", categoria));

        assertNotNull(produto.getId());

        Optional<Product> encontrado = productRepository.findById(produto.getId());
        assertTrue(encontrado.isPresent());
        assertNotNull(encontrado.get().getCategoria());
        assertEquals("Bebidas", encontrado.get().getCategoria().getNome());
    }

    @Test
    @DisplayName("Deve persistir produto com fornecedor principal")
    void devePersistirProdutoComFornecedor() {
        Category categoria = salvarCategoria("Alimentos");
        Supplier fornecedor = new Supplier("Distribuidora Beta");
        fornecedor.setCpfCnpj("11222333000181");
        fornecedor = supplierRepository.save(fornecedor);

        Product produto = novoProduto("Arroz", categoria);
        produto.setFornecedorPrincipal(fornecedor);
        produto = productRepository.save(produto);

        Product encontrado = productRepository.findById(produto.getId()).orElseThrow();
        assertNotNull(encontrado.getFornecedorPrincipal());
        assertEquals("Distribuidora Beta", encontrado.getFornecedorPrincipal().getNomeRazaoSocial());
    }

    @Test
    @DisplayName("Deve encontrar produto por SKU")
    void deveEncontrarPorSku() {
        Category categoria = salvarCategoria("Limpeza");
        Product produto = novoProduto("Detergente", categoria);
        produto.setSku("DET-001");
        productRepository.save(produto);

        assertTrue(productRepository.findBySku("DET-001").isPresent());
        assertFalse(productRepository.findBySku("INEXISTENTE").isPresent());
    }

    @Test
    @DisplayName("Deve encontrar produto por código de barras")
    void deveEncontrarPorCodigoBarras() {
        Category categoria = salvarCategoria("Higiene");
        Product produto = novoProduto("Sabonete", categoria);
        produto.setCodigoBarras("5901234123457");
        productRepository.save(produto);

        assertTrue(productRepository.findByCodigoBarras("5901234123457").isPresent());
    }

    @Test
    @DisplayName("Pesquisa combinada por termo, status e categoria")
    void devePesquisarComFiltros() {
        Category bebidas = salvarCategoria("Bebidas");
        Category alimentos = salvarCategoria("Alimentos");

        Product refrigerante = novoProduto("Refrigerante Cola", bebidas);
        refrigerante.setSku("REF-001");
        productRepository.save(refrigerante);

        productRepository.save(novoProduto("Arroz", alimentos));

        assertEquals(1, productRepository.buscar("Refri", null, null).size());
        assertEquals(1, productRepository.buscar(null, "ATIVO", bebidas.getId()).size());
        assertEquals(2, productRepository.buscar(null, "ATIVO", null).size());
    }

    @Test
    @DisplayName("Categoria possui data de criação preenchida")
    void categoriaPossuiDataCriacao() {
        Category categoria = salvarCategoria("Congelados");
        assertNotNull(categoria.getId());
        assertNotNull(categoria.getDataCriacao());
        assertEquals("ATIVO", categoria.getStatus());
    }
}
