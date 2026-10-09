package com.fasteasy.repository;

import com.fasteasy.domain.Sale;
import com.fasteasy.domain.StatusVenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositório para operações com Sale (vendas).
 */
@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    Optional<Sale> findByNumero(String numero);

    boolean existsByNumero(String numero);

    /**
     * Pesquisa vendas por número, intervalo de datas e status.
     * Parâmetros nulos são ignorados.
     */
    @Query("SELECT s FROM Sale s WHERE " +
           "(:numero IS NULL OR s.numero = :numero) " +
           "AND (:inicio IS NULL OR s.dataHora >= :inicio) " +
           "AND (:fim IS NULL OR s.dataHora < :fim) " +
           "AND (:status IS NULL OR s.status = :status) " +
           "ORDER BY s.dataHora DESC")
    List<Sale> buscarHistorico(@Param("numero") String numero,
                               @Param("inicio") LocalDateTime inicio,
                               @Param("fim") LocalDateTime fim,
                               @Param("status") StatusVenda status);
}
