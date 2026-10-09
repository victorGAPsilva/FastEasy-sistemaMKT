package com.fasteasy.repository;

import com.fasteasy.domain.CashRegister;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para operações com CashRegister (cadastro de caixas).
 */
@Repository
public interface CashRegisterRepository extends JpaRepository<CashRegister, Long> {

    Optional<CashRegister> findByNomeIgnoreCase(String nome);

    List<CashRegister> findByStatusOrderByNomeAsc(String status);

    List<CashRegister> findAllByOrderByNomeAsc();

    /**
     * Carrega o caixa com bloqueio pessimista de escrita.
     *
     * <p>Usado na abertura de sessão para serializar tentativas concorrentes
     * no mesmo caixa e evitar duas sessões abertas simultâneas.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CashRegister c WHERE c.id = :id")
    Optional<CashRegister> buscarParaAtualizacao(@Param("id") Long id);
}
