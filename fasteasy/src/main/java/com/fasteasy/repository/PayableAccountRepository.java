package com.fasteasy.repository;

import com.fasteasy.domain.PayableAccount;
import com.fasteasy.domain.PayableAccountStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositório para operações com PayableAccount.
 */
@Repository
public interface PayableAccountRepository extends JpaRepository<PayableAccount, Long> {

    Optional<PayableAccount> findByCompraOrigemId(Long compraId);

    List<PayableAccount> findByFornecedorId(Long fornecedorId);

    List<PayableAccount> findByCategoriaId(Long categoriaId);

    List<PayableAccount> findByStatus(PayableAccountStatus status);

    /**
     * Carrega a conta com bloqueio pessimista de escrita.
     * Usado no registro de pagamento para serializar operações concorrentes.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM PayableAccount a WHERE a.id = :id")
    Optional<PayableAccount> buscarParaAtualizacao(@Param("id") Long id);

    /**
     * Busca contas com vencimento próximo ou vencidas.
     */
    @Query("SELECT DISTINCT a FROM PayableAccount a " +
           "JOIN a.parcelas p " +
           "WHERE a.status IN (:statuses) " +
           "AND p.dataVencimento <= :dataLimite " +
           "ORDER BY p.dataVencimento ASC")
    List<PayableAccount> buscarComVencimentoAte(@Param("statuses") List<PayableAccountStatus> statuses,
                                                 @Param("dataLimite") LocalDate dataLimite);

    /**
     * Pesquisa avançada com filtros.
     */
    @Query("SELECT a FROM PayableAccount a " +
           "WHERE (:fornecedorId IS NULL OR a.fornecedor.id = :fornecedorId) " +
           "AND (:categoriaId IS NULL OR a.categoria.id = :categoriaId) " +
           "AND (:status IS NULL OR a.status = :status) " +
           "AND (:dataInicio IS NULL OR a.dataEmissao >= :dataInicio) " +
           "AND (:dataFim IS NULL OR a.dataEmissao <= :dataFim) " +
           "ORDER BY a.dataEmissao DESC, a.id DESC")
    List<PayableAccount> pesquisar(@Param("fornecedorId") Long fornecedorId,
                                    @Param("categoriaId") Long categoriaId,
                                    @Param("status") PayableAccountStatus status,
                                    @Param("dataInicio") LocalDate dataInicio,
                                    @Param("dataFim") LocalDate dataFim);

    /**
     * Conta total de contas em aberto.
     */
    long countByStatusIn(List<PayableAccountStatus> statuses);

    /**
     * Soma dos saldos pendentes por status.
     */
    @Query("SELECT COALESCE(SUM(a.saldoPendente), 0) FROM PayableAccount a WHERE a.status IN (:statuses)")
    BigDecimal somaSaldosPendentes(@Param("statuses") List<PayableAccountStatus> statuses);

    /**
     * Soma dos saldos pendentes por fornecedor.
     */
    @Query("SELECT a.fornecedor.id, a.fornecedor.nomeRazaoSocial, COALESCE(SUM(a.saldoPendente), 0) " +
           "FROM PayableAccount a " +
           "WHERE a.fornecedor IS NOT NULL " +
           "AND a.status IN (:statuses) " +
           "GROUP BY a.fornecedor.id, a.fornecedor.nomeRazaoSocial " +
           "ORDER BY COALESCE(SUM(a.saldoPendente), 0) DESC")
    List<Object[]> saldosPorFornecedorRaw(@Param("statuses") List<PayableAccountStatus> statuses);

    /**
     * Soma dos saldos pendentes por categoria.
     */
    @Query("SELECT a.categoria.id, a.categoria.nome, COALESCE(SUM(a.saldoPendente), 0) " +
           "FROM PayableAccount a " +
           "WHERE a.status IN (:statuses) " +
           "GROUP BY a.categoria.id, a.categoria.nome " +
           "ORDER BY COALESCE(SUM(a.saldoPendente), 0) DESC")
    List<Object[]> saldosPorCategoriaRaw(@Param("statuses") List<PayableAccountStatus> statuses);
}