package com.fasteasy.service;

import com.fasteasy.domain.ExpenseCategory;
import com.fasteasy.repository.ExpenseCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Serviço de aplicação para cadastro de categorias de despesa.
 */
@Service
@Transactional
public class ExpenseCategoryService {

    private final ExpenseCategoryRepository expenseCategoryRepository;

    public ExpenseCategoryService(ExpenseCategoryRepository expenseCategoryRepository) {
        this.expenseCategoryRepository = expenseCategoryRepository;
    }

    public ExpenseCategory cadastrar(ExpenseCategory categoria) {
        validar(categoria);

        String nome = categoria.getNome().trim();
        expenseCategoryRepository.findByNomeIgnoreCase(nome).ifPresent(c -> {
            throw new IllegalArgumentException("Já existe uma categoria de despesa com o nome: " + nome);
        });

        categoria.setId(null);
        categoria.setNome(nome);
        if (categoria.getStatus() == null) {
            categoria.setStatus("ATIVO");
        }
        categoria.setDataCriacao(LocalDateTime.now());
        categoria.setDataAtualizacao(null);
        return expenseCategoryRepository.save(categoria);
    }

    public ExpenseCategory editar(Long id, ExpenseCategory dados) {
        ExpenseCategory categoria = buscarOuFalhar(id);
        validar(dados);

        String nome = dados.getNome().trim();
        expenseCategoryRepository.findByNomeIgnoreCase(nome).ifPresent(outro -> {
            if (!outro.getId().equals(id)) {
                throw new IllegalArgumentException("Já existe uma categoria de despesa com o nome: " + nome);
            }
        });

        categoria.setNome(nome);
        categoria.setDescricao(dados.getDescricao());
        if (dados.getStatus() != null) {
            categoria.setStatus(dados.getStatus());
        }
        categoria.setDataAtualizacao(LocalDateTime.now());
        return expenseCategoryRepository.save(categoria);
    }

    public void desativar(Long id) {
        ExpenseCategory categoria = buscarOuFalhar(id);
        if (!categoria.isAtivo()) {
            throw new IllegalArgumentException("Categoria já está inativa");
        }
        categoria.setStatus("INATIVO");
        categoria.setDataAtualizacao(LocalDateTime.now());
        expenseCategoryRepository.save(categoria);
    }

    public void ativar(Long id) {
        ExpenseCategory categoria = buscarOuFalhar(id);
        categoria.setStatus("ATIVO");
        categoria.setDataAtualizacao(LocalDateTime.now());
        expenseCategoryRepository.save(categoria);
    }

    @Transactional(readOnly = true)
    public Optional<ExpenseCategory> buscarPorId(Long id) {
        return expenseCategoryRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<ExpenseCategory> listarTodos() {
        return expenseCategoryRepository.findAllByOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public List<ExpenseCategory> listarAtivas() {
        return expenseCategoryRepository.findByStatusOrderByNomeAsc("ATIVO");
    }

    @Transactional(readOnly = true)
    public List<ExpenseCategory> pesquisar(String nome, String status) {
        return expenseCategoryRepository.pesquisar(nome, status);
    }

    private ExpenseCategory buscarOuFalhar(Long id) {
        return expenseCategoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria de despesa não encontrada com ID: " + id));
    }

    private void validar(ExpenseCategory categoria) {
        if (categoria == null) {
            throw new IllegalArgumentException("A categoria é obrigatória");
        }
        if (categoria.getNome() == null || categoria.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("O nome da categoria é obrigatório");
        }
    }
}