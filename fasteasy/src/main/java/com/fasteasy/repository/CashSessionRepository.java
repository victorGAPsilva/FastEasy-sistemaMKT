package com.fasteasy.repository;

import com.fasteasy.domain.CashSession;
import com.fasteasy.domain.StatusSessaoCaixa;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositório para operações com CashSession (sessões de caixa).
 *
 * <p>As consultas com bloqueio pessimista são usadas nos fluxos de venda,
 * sangria e fechamento para serializar operações concorrentes na mesma
 * sessão.</p>
 */
@Repository
public interface CashSessionRepository extends JpaRepository<CashSession, Long> {

    Optional<CashSession> findFirstByCaixaIdAndStatusOrderByDataAberturaDesc(Long caixaId, StatusSessaoCaixa status);

    boolean existsByCaixaIdAndStatus(Long caixaId, StatusSessaoCaixa status);

    List<CashSession> findByCaixaIdOrderByDataAberturaDesc(Long caixaId);

    /**
     * Carrega a sessão com bloqueio pessimista de escrita.
     * Usado no fechamento, nas movimentações e na finalização de vendas.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM CashSession s WHERE s.id = :id")
    Optional<CashSession> buscarParaAtualizacao(@Param("id") Long id);

    /**
     * Carrega a sessão aberta de um caixa com bloqueio pessimista de escrita,
     * se existir. Serializa sangrias, suprimentos e fechamento concorrentes.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM CashSession s WHERE s.caixa.id = :caixaId AND s.status = :status")
    Optional<CashSession> buscarAbertaParaAtualizacao(@Param("caixaId") Long caixaId,
                                                       @Param("status") StatusSessaoCaixa status);

    /**
     * Pesquisa sessões por caixa, período de abertura e status.
     * Parâmetros nulos são ignorados.
     */
    @Query("SELECT s FROM CashSession s WHERE " +
           "(:caixaId IS NULL OR s.caixa.id = :caixaId) " +
           "AND (:inicio IS NULL OR s.dataAbertura >= :inicio) " +
           "AND (:fim IS NULL OR s.dataAbertura < :fim) " +
           "AND (:status IS NULL OR s.status = :status) " +
           "ORDER BY s.dataAbertura DESC")
    List<CashSession> buscarHistorico(@Param("caixaId") Long caixaId,
                                      @Param("inicio") LocalDateTime inicio,
                                      @Param("fim") LocalDateTime fim,
                                      @Param("status") StatusSessaoCaixa status);
}
