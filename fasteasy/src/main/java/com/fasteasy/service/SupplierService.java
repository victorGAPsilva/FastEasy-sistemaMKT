package com.fasteasy.service;

import com.fasteasy.domain.Supplier;
import com.fasteasy.repository.SupplierRepository;
import com.fasteasy.validator.DocumentoValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Serviço de aplicação para operações com Supplier.
 * Contém as regras de negócio relacionadas a fornecedores.
 */
@Service
@Transactional
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    /**
     * Cadastra um novo fornecedor.
     */
    public Supplier cadastrar(Supplier supplier) {
        validar(supplier);

        supplierRepository.findByNomeIgnoreCase(supplier.getNomeRazaoSocial().trim()).ifPresent(s -> {
            throw new IllegalArgumentException("Já existe um fornecedor com a razão social: " + supplier.getNomeRazaoSocial());
        });

        validarDocumentoUnico(supplier.getCpfCnpj(), null);

        supplier.setId(null);
        supplier.setNomeRazaoSocial(supplier.getNomeRazaoSocial().trim());
        if (supplier.getStatus() == null) {
            supplier.setStatus("ATIVO");
        }
        supplier.setDataCadastro(LocalDateTime.now());
        supplier.setDataAtualizacao(null);

        return supplierRepository.save(supplier);
    }

    /**
     * Edita um fornecedor existente.
     */
    public Supplier editar(Long id, Supplier dados) {
        Supplier fornecedor = buscarOuFalhar(id);
        validar(dados);

        supplierRepository.findByNomeIgnoreCase(dados.getNomeRazaoSocial().trim()).ifPresent(outro -> {
            if (!outro.getId().equals(id)) {
                throw new IllegalArgumentException("Já existe um fornecedor com a razão social: " + dados.getNomeRazaoSocial());
            }
        });

        validarDocumentoUnico(dados.getCpfCnpj(), id);

        fornecedor.setNomeRazaoSocial(dados.getNomeRazaoSocial().trim());
        fornecedor.setNomeFantasia(dados.getNomeFantasia());
        fornecedor.setCpfCnpj(dados.getCpfCnpj());
        fornecedor.setTelefone(dados.getTelefone());
        fornecedor.setEmail(dados.getEmail());
        fornecedor.setEndereco(dados.getEndereco());
        fornecedor.setObservacoes(dados.getObservacoes());
        if (dados.getStatus() != null) {
            fornecedor.setStatus(dados.getStatus());
        }
        fornecedor.setDataAtualizacao(LocalDateTime.now());

        return supplierRepository.save(fornecedor);
    }

    /**
     * Desativa um fornecedor (exclusão lógica).
     */
    public void desativar(Long id) {
        Supplier fornecedor = buscarOuFalhar(id);
        if ("INATIVO".equals(fornecedor.getStatus())) {
            throw new IllegalArgumentException("Fornecedor já está inativo");
        }
        fornecedor.setStatus("INATIVO");
        fornecedor.setDataAtualizacao(LocalDateTime.now());
        supplierRepository.save(fornecedor);
    }

    /**
     * Reativa um fornecedor.
     */
    public void ativar(Long id) {
        Supplier fornecedor = buscarOuFalhar(id);
        fornecedor.setStatus("ATIVO");
        fornecedor.setDataAtualizacao(LocalDateTime.now());
        supplierRepository.save(fornecedor);
    }

    @Transactional(readOnly = true)
    public Optional<Supplier> buscarPorId(Long id) {
        return supplierRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Supplier> listarTodos() {
        return supplierRepository.findAllByOrderByNomeRazaoSocialAsc();
    }

    @Transactional(readOnly = true)
    public List<Supplier> listarAtivos() {
        return supplierRepository.findByStatusOrderByNomeRazaoSocialAsc("ATIVO");
    }

    @Transactional(readOnly = true)
    public List<Supplier> pesquisarPorNome(String termo) {
        if (termo == null || termo.trim().isEmpty()) {
            return listarTodos();
        }
        return supplierRepository.pesquisarPorNome(termo.trim());
    }

    @Transactional(readOnly = true)
    public Optional<Supplier> pesquisarPorDocumento(String documento) {
        return supplierRepository.pesquisarPorDocumento(DocumentoValidator.somenteDigitos(documento));
    }

    private Supplier buscarOuFalhar(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fornecedor não encontrado com ID: " + id));
    }

    private void validar(Supplier supplier) {
        if (supplier == null) {
            throw new IllegalArgumentException("O fornecedor é obrigatório");
        }
        if (supplier.getNomeRazaoSocial() == null || supplier.getNomeRazaoSocial().trim().isEmpty()) {
            throw new IllegalArgumentException("O nome ou razão social é obrigatório");
        }
        if (supplier.getCpfCnpj() != null && !supplier.getCpfCnpj().trim().isEmpty()) {
            if (!DocumentoValidator.isDocumentoValido(supplier.getCpfCnpj())) {
                throw new IllegalArgumentException("CPF/CNPJ inválido");
            }
        }
        if (supplier.getEmail() != null && !supplier.getEmail().trim().isEmpty()
                && !supplier.getEmail().matches("^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$")) {
            throw new IllegalArgumentException("E-mail inválido");
        }
    }

    private void validarDocumentoUnico(String documento, Long idAtual) {
        if (documento == null || documento.trim().isEmpty()) {
            return;
        }
        String digitos = DocumentoValidator.somenteDigitos(documento);
        supplierRepository.pesquisarPorDocumento(digitos).ifPresent(existente -> {
            if (idAtual == null || !existente.getId().equals(idAtual)) {
                throw new IllegalArgumentException("Já existe um fornecedor com este CPF/CNPJ");
            }
        });
    }
}
