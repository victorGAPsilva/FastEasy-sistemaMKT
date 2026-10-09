package com.fasteasy.repository;

import com.fasteasy.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para operações com Product (produtos).
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByNomeIgnoreCase(String nome);

    Optional<Product> findBySku(String sku);

    Optional<Product> findByCodigoBarras(String codigoBarras);

    List<Product> findByStatusOrderByNomeAsc(String status);

    List<Product> findByCategoriaId(Long categoriaId);

    List<Product> findAllByOrderByNomeAsc();

    long countByStatus(String status);

    /**
     * Pesquisa produtos combinando termo (nome/SKU/código de barras), status e categoria.
     * Parâmetros nulos são ignorados.
     */
    @Query("SELECT p FROM Product p WHERE " +
           "(:termo IS NULL OR LOWER(p.nome) LIKE LOWER(CONCAT('%', :termo, '%')) " +
           "   OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :termo, '%')) " +
           "   OR p.codigoBarras LIKE CONCAT('%', :termo, '%')) " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:categoriaId IS NULL OR p.categoria.id = :categoriaId) " +
           "ORDER BY p.nome ASC")
    List<Product> buscar(@Param("termo") String termo,
                         @Param("status") String status,
                         @Param("categoriaId") Long categoriaId);

    /**
     * Lista apenas produtos ativos, usados como opção em operações de venda.
     */
    @Query("SELECT p FROM Product p WHERE p.status = 'ATIVO' ORDER BY p.nome ASC")
    List<Product> listarAtivosParaVenda();
}
