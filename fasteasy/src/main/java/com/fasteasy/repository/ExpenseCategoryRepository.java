package com.fasteasy.repository;

import com.fasteasy.domain.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para operações com ExpenseCategory.
 */
@Repository
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {

    Optional<ExpenseCategory> findByNomeIgnoreCase(String nome);

    List<ExpenseCategory> findByStatusOrderByNomeAsc(String status);

    List<ExpenseCategory> findAllByOrderByNomeAsc();

    @Query("SELECT c FROM ExpenseCategory c WHERE " +
           "(:nome IS NULL OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :nome, '%'))) " +
           "AND (:status IS NULL OR c.status = :status) " +
           "ORDER BY c.nome ASC")
    List<ExpenseCategory> pesquisar(@Param("nome") String nome,
                                    @Param("status") String status);
}