package com.fasteasy.repository;

import com.fasteasy.domain.FinancialPayment;
import com.fasteasy.domain.FinancialPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositório para operações com FinancialPayment.
 */
@Repository
public interface FinancialPaymentRepository extends JpaRepository<FinancialPayment, Long> {

    List<FinancialPayment> findByContaIdOrderByDataHoraAsc(Long contaId);

    List<FinancialPayment> findByParcelaIdOrderByDataHoraAsc(Long parcelaId);

    List<FinancialPayment> findByMetodoPagamento(FinancialPaymentMethod metodo);

    @Query("SELECT p FROM FinancialPayment p " +
           "WHERE (:contaId IS NULL OR p.conta.id = :contaId) " +
           "AND (:parcelaId IS NULL OR p.parcela.id = :parcelaId) " +
           "AND (:metodo IS NULL OR p.metodoPagamento = :metodo) " +
           "AND (:inicio IS NULL OR p.dataHora >= :inicio) " +
           "AND (:fim IS NULL OR p.dataHora < :fim) " +
           "ORDER BY p.dataHora DESC")
    List<FinancialPayment> pesquisar(@Param("contaId") Long contaId,
                                      @Param("parcelaId") Long parcelaId,
                                      @Param("metodo") FinancialPaymentMethod metodo,
                                      @Param("inicio") LocalDateTime inicio,
                                      @Param("fim") LocalDateTime fim);

    /**
     * Soma dos pagamentos por método em um período.
     */
    @Query("SELECT p.metodoPagamento, COALESCE(SUM(p.valor), 0) " +
           "FROM FinancialPayment p " +
           "WHERE p.status = 'CONFIRMED' " +
           "AND (:inicio IS NULL OR p.dataHora >= :inicio) " +
           "AND (:fim IS NULL OR p.dataHora < :fim) " +
           "GROUP BY p.metodoPagamento " +
           "ORDER BY COALESCE(SUM(p.valor), 0) DESC")
    List<Object[]> totaisPorMetodoRaw(@Param("inicio") LocalDateTime inicio,
                                       @Param("fim") LocalDateTime fim);

    /**
     * Soma dos pagamentos por data (para fluxo de caixa realizado).
     */
    @Query("SELECT DATE(p.dataHora), COALESCE(SUM(p.valor), 0) " +
           "FROM FinancialPayment p " +
           "WHERE p.status = 'CONFIRMED' " +
           "AND (:inicio IS NULL OR p.dataHora >= :inicio) " +
           "AND (:fim IS NULL OR p.dataHora < :fim) " +
           "GROUP BY DATE(p.dataHora) " +
           "ORDER BY DATE(p.dataHora) ASC")
    List<Object[]> fluxoRealizadoPorDiaRaw(@Param("inicio") LocalDateTime inicio,
                                            @Param("fim") LocalDateTime fim);
}