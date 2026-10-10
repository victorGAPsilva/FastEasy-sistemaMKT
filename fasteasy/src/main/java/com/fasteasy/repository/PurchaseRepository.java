package com.fasteasy.repository;

import com.fasteasy.domain.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para operações com Purchase.
 */
@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    Optional<Purchase> findByNumero(String numero);

    List<Purchase> findByFornecedorId(Long fornecedorId);

    List<Purchase> findByStatus(String status);

    @Query("SELECT p FROM Purchase p " +
           "WHERE (:fornecedorId IS NULL OR p.fornecedor.id = :fornecedorId) " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:inicio IS NULL OR p.dataEmissao >= :inicio) " +
           "AND (:fim IS NULL OR p.dataEmissao < :fim) " +
           "ORDER BY p.dataEmissao DESC")
    List<Purchase> pesquisar(@Param("fornecedorId") Long fornecedorId,
                             @Param("status") String status,
                             @Param("inicio") java.time.LocalDateTime inicio,
                             @Param("fim") java.time.LocalDateTime fim);

    /**
     * Verifica se já existe conta a pagar vinculada a esta compra.
     * Usado para idempotência na geração de contas a pagar.
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
           "FROM com.fasteasy.domain.PayableAccount a WHERE a.compraOrigem.id = :compraId")
    boolean existsPayableAccountByCompraId(@Param("compraId") Long compraId);
}