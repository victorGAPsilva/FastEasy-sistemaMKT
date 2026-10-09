package com.fasteasy.service;

import com.fasteasy.domain.Category;
import com.fasteasy.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Serviço de aplicação para operações com Category.
 * Contém as regras de negócio relacionadas a categorias.
 */
@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Cadastra uma nova categoria, impedindo nomes duplicados.
     */
    public Category cadastrar(Category category) {
        validarNome(category);
        categoryRepository.findByNomeIgnoreCase(category.getNome().trim()).ifPresent(c -> {
            throw new IllegalArgumentException("Já existe uma categoria com o nome: " + category.getNome());
        });
        category.setId(null);
        category.setNome(category.getNome().trim());
        if (category.getStatus() == null) {
            category.setStatus("ATIVO");
        }
        category.setDataCriacao(LocalDateTime.now());
        category.setDataAtualizacao(null);
        return categoryRepository.save(category);
    }

    /**
     * Edita uma categoria existente.
     */
    public Category editar(Long id, Category dados) {
        Category categoria = buscarOuFalhar(id);
        validarNome(dados);

        categoryRepository.findByNomeIgnoreCase(dados.getNome().trim()).ifPresent(outra -> {
            if (!outra.getId().equals(id)) {
                throw new IllegalArgumentException("Já existe uma categoria com o nome: " + dados.getNome());
            }
        });

        categoria.setNome(dados.getNome().trim());
        categoria.setDescricao(dados.getDescricao());
        if (dados.getStatus() != null) {
            categoria.setStatus(dados.getStatus());
        }
        categoria.setDataAtualizacao(LocalDateTime.now());
        return categoryRepository.save(categoria);
    }

    /**
     * Desativa uma categoria (exclusão lógica, preserva histórico).
     */
    public void desativar(Long id) {
        Category categoria = buscarOuFalhar(id);
        if ("INATIVO".equals(categoria.getStatus())) {
            throw new IllegalArgumentException("Categoria já está inativa");
        }
        categoria.setStatus("INATIVO");
        categoria.setDataAtualizacao(LocalDateTime.now());
        categoryRepository.save(categoria);
    }

    /**
     * Reativa uma categoria.
     */
    public void ativar(Long id) {
        Category categoria = buscarOuFalhar(id);
        categoria.setStatus("ATIVO");
        categoria.setDataAtualizacao(LocalDateTime.now());
        categoryRepository.save(categoria);
    }

    @Transactional(readOnly = true)
    public Optional<Category> buscarPorId(Long id) {
        return categoryRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Category> listarTodos() {
        return categoryRepository.findAllByOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public List<Category> listarAtivos() {
        return categoryRepository.findByStatusOrderByNomeAsc("ATIVO");
    }

    @Transactional(readOnly = true)
    public List<Category> pesquisarPorNome(String termo) {
        if (termo == null || termo.trim().isEmpty()) {
            return listarTodos();
        }
        return categoryRepository.pesquisarPorNome(termo.trim());
    }

    private Category buscarOuFalhar(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada com ID: " + id));
    }

    private void validarNome(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("A categoria é obrigatória");
        }
        if (category.getNome() == null || category.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("O nome da categoria é obrigatório");
        }
    }
}
