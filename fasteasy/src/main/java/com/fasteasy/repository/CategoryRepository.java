package com.fasteasy.repository;

import com.fasteasy.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para operações com Category (categorias).
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByNomeIgnoreCase(String nome);

    List<Category> findByStatusOrderByNomeAsc(String status);

    List<Category> findAllByOrderByNomeAsc();

    /**
     * Pesquisa categorias por nome (termo parcial).
     */
    @Query("SELECT c FROM Category c WHERE LOWER(c.nome) LIKE LOWER(CONCAT('%', :termo, '%')) " +
           "ORDER BY c.nome ASC")
    List<Category> pesquisarPorNome(@Param("termo") String termo);
}
