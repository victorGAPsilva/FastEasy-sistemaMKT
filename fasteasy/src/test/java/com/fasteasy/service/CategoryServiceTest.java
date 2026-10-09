package com.fasteasy.service;

import com.fasteasy.domain.Category;
import com.fasteasy.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Serviço de Categorias")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category categoria;

    @BeforeEach
    void setUp() {
        categoria = new Category("Bebidas", "Bebidas em geral");
    }

    @Test
    @DisplayName("Deve cadastrar categoria válida")
    void deveCadastrarCategoria() {
        when(categoryRepository.findByNomeIgnoreCase("Bebidas")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        Category salva = categoryService.cadastrar(categoria);

        assertNotNull(salva.getId());
        assertEquals("Bebidas", salva.getNome());
        assertEquals("ATIVO", salva.getStatus());
        assertNotNull(salva.getDataCriacao());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("Não deve permitir categoria duplicada")
    void naoDevePermitirDuplicidade() {
        when(categoryRepository.findByNomeIgnoreCase("Bebidas"))
                .thenReturn(Optional.of(new Category("Bebidas")));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> categoryService.cadastrar(categoria));
        assertTrue(ex.getMessage().toLowerCase().contains("já existe"));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Não deve cadastrar categoria sem nome")
    void naoDeveCadastrarSemNome() {
        Category semNome = new Category();
        assertThrows(IllegalArgumentException.class, () -> categoryService.cadastrar(semNome));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve desativar categoria (exclusão lógica)")
    void deveDesativarCategoria() {
        Category existente = new Category("Bebidas");
        existente.setId(10L);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(existente));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        categoryService.desativar(10L);

        assertEquals("INATIVO", existente.getStatus());
        assertNotNull(existente.getDataAtualizacao());
        verify(categoryRepository).save(existente);
    }

    @Test
    @DisplayName("Deve editar categoria existente")
    void deveEditarCategoria() {
        Category existente = new Category("Bebidas");
        existente.setId(5L);
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(categoryRepository.findByNomeIgnoreCase("Bebidas Geladas")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        Category dados = new Category("Bebidas Geladas", "Refrigeradas");
        Category atualizada = categoryService.editar(5L, dados);

        assertEquals("Bebidas Geladas", atualizada.getNome());
        assertEquals("Refrigeradas", atualizada.getDescricao());
    }

    @Test
    @DisplayName("Deve falhar ao desativar categoria já inativa")
    void naoDeveDesativarDuasVezes() {
        Category existente = new Category("Bebidas");
        existente.setId(7L);
        existente.setStatus("INATIVO");
        when(categoryRepository.findById(7L)).thenReturn(Optional.of(existente));

        assertThrows(IllegalArgumentException.class, () -> categoryService.desativar(7L));
    }
}
