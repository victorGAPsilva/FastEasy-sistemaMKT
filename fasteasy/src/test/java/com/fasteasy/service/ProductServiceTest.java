package com.fasteasy.service;

import com.fasteasy.domain.Category;
import com.fasteasy.domain.Product;
import com.fasteasy.domain.Supplier;
import com.fasteasy.domain.UnidadeMedida;
import com.fasteasy.repository.CategoryRepository;
import com.fasteasy.repository.ProductRepository;
import com.fasteasy.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Serviço de Produtos")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private ProductService productService;

    private Category categoria;
    private Product produtoValido;

    @BeforeEach
    void setUp() {
        categoria = new Category("Alimentos");
        categoria.setId(1L);
        categoria.setStatus("ATIVO");

        produtoValido = new Product("Arroz 5kg");
        produtoValido.setPrecoVenda(new BigDecimal("25.90"));
        produtoValido.setCustoAquisicao(new BigDecimal("18.00"));
        produtoValido.setCategoria(categoria);
        produtoValido.setUnidadeMedida(UnidadeMedida.UN);
        produtoValido.setEstoqueMinimo(10);
    }

    private void configurarSalvamento() {
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(1L);
            }
            return p;
        });
    }

    // ---------- Cadastro válido ----------

    @Test
    @DisplayName("Deve cadastrar produto válido")
    void deveCadastrarProdutoValido() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(productRepository.findBySku(any())).thenReturn(Optional.empty());
        when(productRepository.findByCodigoBarras(any())).thenReturn(Optional.empty());
        configurarSalvamento();

        Product salvo = productService.cadastrar(produtoValido);

        assertNotNull(salvo.getId());
        assertEquals("ATIVO", salvo.getStatus());
        assertEquals("Arroz 5kg", salvo.getNome());
        assertNotNull(salvo.getDataCriacao());
        verify(productRepository).save(any(Product.class));
    }

    // ---------- Regra 1: nome ----------

    @Test
    @DisplayName("Deve rejeitar produto sem nome")
    void deveRejeitarProdutoSemNome() {
        produtoValido.setNome("  ");
        assertThrows(IllegalArgumentException.class, () -> productService.cadastrar(produtoValido));
        verify(productRepository, never()).save(any());
    }

    // ---------- Regra 2: preço ----------

    @Test
    @DisplayName("Deve rejeitar preço de venda zerado")
    void deveRejeitarPrecoZero() {
        produtoValido.setPrecoVenda(BigDecimal.ZERO);
        assertThrows(IllegalArgumentException.class, () -> productService.cadastrar(produtoValido));
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar preço de venda negativo")
    void deveRejeitarPrecoNegativo() {
        produtoValido.setPrecoVenda(new BigDecimal("-1.00"));
        assertThrows(IllegalArgumentException.class, () -> productService.cadastrar(produtoValido));
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar preço de venda nulo")
    void deveRejeitarPrecoNulo() {
        produtoValido.setPrecoVenda(null);
        assertThrows(IllegalArgumentException.class, () -> productService.cadastrar(produtoValido));
        verify(productRepository, never()).save(any());
    }

    // ---------- Regra 3: custo ----------

    @Test
    @DisplayName("Deve rejeitar custo negativo")
    void deveRejeitarCustoNegativo() {
        produtoValido.setCustoAquisicao(new BigDecimal("-5.00"));
        assertThrows(IllegalArgumentException.class, () -> productService.cadastrar(produtoValido));
        verify(productRepository, never()).save(any());
    }

    // ---------- Regra 4/5: estoque ----------

    @Test
    @DisplayName("Deve rejeitar estoque mínimo negativo")
    void deveRejeitarEstoqueMinimoNegativo() {
        produtoValido.setEstoqueMinimo(-1);
        assertThrows(IllegalArgumentException.class, () -> productService.cadastrar(produtoValido));
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar estoque máximo menor que o mínimo")
    void deveRejeitarEstoqueMaximoMenorQueMinimo() {
        produtoValido.setEstoqueMinimo(10);
        produtoValido.setEstoqueMaximo(new BigDecimal("5"));
        assertThrows(IllegalArgumentException.class, () -> productService.cadastrar(produtoValido));
        verify(productRepository, never()).save(any());
    }

    // ---------- Regra 6: SKU único ----------

    @Test
    @DisplayName("Deve rejeitar SKU duplicado")
    void deveRejeitarSkuDuplicado() {
        produtoValido.setSku("SKU-001");
        Product existente = new Product("Outro");
        existente.setId(99L);
        when(productRepository.findBySku("SKU-001")).thenReturn(Optional.of(existente));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productService.cadastrar(produtoValido));
        assertTrue(ex.getMessage().toLowerCase().contains("sku"));
        verify(productRepository, never()).save(any());
    }

    // ---------- Regra 7: código de barras único ----------

    @Test
    @DisplayName("Deve rejeitar código de barras duplicado")
    void deveRejeitarCodigoBarrasDuplicado() {
        produtoValido.setCodigoBarras("7891000315507");
        Product existente = new Product("Outro");
        existente.setId(99L);
        when(productRepository.findBySku(any())).thenReturn(Optional.empty());
        when(productRepository.findByCodigoBarras("7891000315507")).thenReturn(Optional.of(existente));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productService.cadastrar(produtoValido));
        assertTrue(ex.getMessage().toLowerCase().contains("código de barras"));
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar código de barras EAN com DV inválido")
    void deveRejeitarEanInvalido() {
        produtoValido.setCodigoBarras("5901234123456");
        assertThrows(IllegalArgumentException.class, () -> productService.cadastrar(produtoValido));
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve aceitar código de barras EAN-13 válido")
    void deveAceitarEan13Valido() {
        produtoValido.setCodigoBarras("5901234123457");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(productRepository.findBySku(any())).thenReturn(Optional.empty());
        when(productRepository.findByCodigoBarras("5901234123457")).thenReturn(Optional.empty());
        configurarSalvamento();

        Product salvo = productService.cadastrar(produtoValido);
        assertEquals("5901234123457", salvo.getCodigoBarras());
    }

    // ---------- Relacionamentos ----------

    @Test
    @DisplayName("Deve rejeitar produto com categoria inexistente")
    void deveRejeitarCategoriaInexistente() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> productService.cadastrar(produtoValido));
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar produto com categoria inativa")
    void deveRejeitarCategoriaInativa() {
        categoria.setStatus("INATIVO");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(categoria));
        assertThrows(IllegalArgumentException.class, () -> productService.cadastrar(produtoValido));
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve vincular fornecedor principal ativo")
    void deveVincularFornecedorAtivo() {
        Supplier fornecedor = new Supplier("Fornecedor Alfa");
        fornecedor.setId(3L);
        fornecedor.setStatus("ATIVO");
        produtoValido.setFornecedorPrincipal(fornecedor);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(supplierRepository.findById(3L)).thenReturn(Optional.of(fornecedor));
        when(productRepository.findBySku(any())).thenReturn(Optional.empty());
        when(productRepository.findByCodigoBarras(any())).thenReturn(Optional.empty());
        configurarSalvamento();

        Product salvo = productService.cadastrar(produtoValido);
        assertNotNull(salvo.getFornecedorPrincipal());
        assertEquals("Fornecedor Alfa", salvo.getFornecedorPrincipal().getNomeRazaoSocial());
    }

    @Test
    @DisplayName("Deve rejeitar fornecedor principal inativo")
    void deveRejeitarFornecedorInativo() {
        Supplier fornecedor = new Supplier("Fornecedor Inativo");
        fornecedor.setId(3L);
        fornecedor.setStatus("INATIVO");
        produtoValido.setFornecedorPrincipal(fornecedor);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(supplierRepository.findById(3L)).thenReturn(Optional.of(fornecedor));

        assertThrows(IllegalArgumentException.class, () -> productService.cadastrar(produtoValido));
    }

    // ---------- Edição ----------

    @Test
    @DisplayName("Deve editar produto existente")
    void deveEditarProduto() {
        Product existente = new Product("Arroz 5kg");
        existente.setId(50L);
        existente.setStatus("ATIVO");
        existente.setCategoria(categoria);

        when(productRepository.findById(50L)).thenReturn(Optional.of(existente));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(productRepository.findBySku(any())).thenReturn(Optional.empty());
        when(productRepository.findByCodigoBarras(any())).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product dados = new Product("Arroz 5kg Premium");
        dados.setPrecoVenda(new BigDecimal("29.90"));
        dados.setCategoria(categoria);
        dados.setUnidadeMedida(UnidadeMedida.UN);
        dados.setEstoqueMinimo(5);

        Product atualizado = productService.editar(50L, dados);

        assertEquals("Arroz 5kg Premium", atualizado.getNome());
        assertEquals(new BigDecimal("29.90"), atualizado.getPrecoVenda());
        assertNotNull(atualizado.getDataAtualizacao());
    }

    // ---------- Desativação ----------

    @Test
    @DisplayName("Deve desativar produto (exclusão lógica)")
    void deveDesativarProduto() {
        Product existente = new Product("Feijão");
        existente.setId(60L);
        existente.setStatus("ATIVO");
        when(productRepository.findById(60L)).thenReturn(Optional.of(existente));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        productService.desativar(60L);

        assertEquals("INATIVO", existente.getStatus());
        assertNotNull(existente.getDataAtualizacao());
        verify(productRepository).save(existente);
    }

    @Test
    @DisplayName("Não deve desativar produto já inativo")
    void naoDeveDesativarDuasVezes() {
        Product existente = new Product("Feijão");
        existente.setId(60L);
        existente.setStatus("INATIVO");
        when(productRepository.findById(60L)).thenReturn(Optional.of(existente));

        assertThrows(IllegalArgumentException.class, () -> productService.desativar(60L));
    }

    // ---------- Regra 10: margem bruta ----------

    @Test
    @DisplayName("Deve calcular margem bruta quando custo maior que zero")
    void deveCalcularMargemBruta() {
        BigDecimal margem = productService.calcularMargemBruta(
                new BigDecimal("100.00"), new BigDecimal("60.00"));
        assertEquals(0, new BigDecimal("40.00").compareTo(margem));
    }

    @Test
    @DisplayName("Margem deve ser nula quando custo é zero ou nulo")
    void margemNulaSemCusto() {
        assertNull(productService.calcularMargemBruta(new BigDecimal("100.00"), BigDecimal.ZERO));
        assertNull(productService.calcularMargemBruta(new BigDecimal("100.00"), null));
        assertNull(productService.calcularMargemBruta(null, new BigDecimal("10.00")));
    }
}
