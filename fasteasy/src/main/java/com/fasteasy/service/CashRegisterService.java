package com.fasteasy.service;

import com.fasteasy.domain.CashRegister;
import com.fasteasy.repository.CashRegisterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Serviço de aplicação para o cadastro de caixas (CashRegister).
 *
 * <p>Não se confunde com uma sessão de caixa: aqui apenas se mantém o cadastro
 * dos pontos de atendimento (ex.: "Caixa 1"). Nenhuma operação financeira é
 * executada neste serviço — isso é responsabilidade do
 * {@link CashSessionService}.</p>
 */
@Service
@Transactional
public class CashRegisterService {

    private final CashRegisterRepository cashRegisterRepository;

    public CashRegisterService(CashRegisterRepository cashRegisterRepository) {
        this.cashRegisterRepository = cashRegisterRepository;
    }

    /** Cadastra um caixa, impedindo nomes duplicados. */
    public CashRegister cadastrar(CashRegister caixa) {
        validarNome(caixa);
        String nome = caixa.getNome().trim();
        cashRegisterRepository.findByNomeIgnoreCase(nome).ifPresent(c -> {
            throw new IllegalArgumentException("Já existe um caixa com o nome: " + nome);
        });
        caixa.setId(null);
        caixa.setNome(nome);
        if (caixa.getStatus() == null) {
            caixa.setStatus("ATIVO");
        }
        caixa.setDataCriacao(LocalDateTime.now());
        caixa.setDataAtualizacao(null);
        return cashRegisterRepository.save(caixa);
    }

    /** Edita um caixa existente. */
    public CashRegister editar(Long id, CashRegister dados) {
        CashRegister caixa = buscarOuFalhar(id);
        validarNome(dados);
        String nome = dados.getNome().trim();
        cashRegisterRepository.findByNomeIgnoreCase(nome).ifPresent(outro -> {
            if (!outro.getId().equals(id)) {
                throw new IllegalArgumentException("Já existe um caixa com o nome: " + nome);
            }
        });
        caixa.setNome(nome);
        caixa.setDescricao(dados.getDescricao());
        if (dados.getStatus() != null) {
            caixa.setStatus(dados.getStatus());
        }
        caixa.setDataAtualizacao(LocalDateTime.now());
        return cashRegisterRepository.save(caixa);
    }

    /** Desativa um caixa (exclusão lógica). */
    public void desativar(Long id) {
        CashRegister caixa = buscarOuFalhar(id);
        if (!caixa.isAtivo()) {
            throw new IllegalArgumentException("Caixa já está inativo");
        }
        caixa.setStatus("INATIVO");
        caixa.setDataAtualizacao(LocalDateTime.now());
        cashRegisterRepository.save(caixa);
    }

    /** Reativa um caixa. */
    public void ativar(Long id) {
        CashRegister caixa = buscarOuFalhar(id);
        caixa.setStatus("ATIVO");
        caixa.setDataAtualizacao(LocalDateTime.now());
        cashRegisterRepository.save(caixa);
    }

    @Transactional(readOnly = true)
    public Optional<CashRegister> buscarPorId(Long id) {
        return cashRegisterRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<CashRegister> listarTodos() {
        return cashRegisterRepository.findAllByOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public List<CashRegister> listarAtivos() {
        return cashRegisterRepository.findByStatusOrderByNomeAsc("ATIVO");
    }

    private CashRegister buscarOuFalhar(Long id) {
        return cashRegisterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Caixa não encontrado com ID: " + id));
    }

    private void validarNome(CashRegister caixa) {
        if (caixa == null) {
            throw new IllegalArgumentException("O caixa é obrigatório");
        }
        if (caixa.getNome() == null || caixa.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do caixa é obrigatório");
        }
    }
}
