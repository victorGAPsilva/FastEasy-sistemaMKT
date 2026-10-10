package com.fasteasy.repository;

import com.fasteasy.domain.PayableAccount;
import com.fasteasy.domain.PayableInstallment;
import com.fasteasy.domain.PayableInstallmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositório para operações com PayableInstallment.
 */
@Repository
public interface PayableInstallmentRepository extends JpaRepository<PayableInstallment, Long> {

    List<PayableInstallment> findByContaIdOrderByNumeroParcelaAsc(Long contaId);

    List<PayableInstallment> findByContaIdAndStatus(Long contaId, PayableInstallmentStatus status);

    /**
     * Carrega a parcela com bloqueio pessimista de escrita.
     * Usado no registro de pagamento para serializar operações concorrentes.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PayableInstallment p WHERE p.id = :id")
    Optional<PayableInstallment> buscarParaAtualizacao(@Param("id") Long id);

    /**
     * Busca parcelas vencidas ou a vencer.
     */
    @Query("SELECT p FROM PayableInstallment p " +
           "WHERE p.status IN (:statuses) " +
           "AND p.dataVencimento <= :dataLimite " +
           "ORDER BY p.dataVencimento ASC")
    List<PayableInstallment> buscarVencidasAte(@Param("statuses") List<PayableInstallmentStatus> statuses,
                                               @Param("dataLimite") LocalDate dataLimite);

    /**
     * Busca parcelas a vencer em um período.
     */
    @Query("SELECT p FROM PayableInstallment p " +
           "WHERE p.status IN (:statuses) " +
           "AND p.dataVencimento BETWEEN :inicio AND :fim " +
           "ORDER BY p.dataVencimento ASC")
    List<PayableInstallment> buscarPorPeriodo(@Param("statuses") List<PayableInstallmentStatus> statuses,
                                               @Param("inicio") LocalDate inicio,
                                               @Param("fim") LocalDate fim);

    /**
     * Soma dos saldos pendentes das parcelas de uma conta.
     */
    @Query("SELECT COALESCE(SUM(p.saldoPendente), 0) FROM PayableInstallment p WHERE p.conta.id = :contaId")
    BigDecimal somaSaldosPendentesPorConta(@Param("contaId") Long contaId);
}