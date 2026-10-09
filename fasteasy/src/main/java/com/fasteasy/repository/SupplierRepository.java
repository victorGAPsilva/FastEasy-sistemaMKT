package com.fasteasy.repository;

import com.fasteasy.domain.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para operações com Supplier (fornecedores).
 */
@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    /**
     * Busca fornecedor pela razão social (ignorando maiúsculas/minúsculas).
     */
    @Query("SELECT s FROM Supplier s WHERE LOWER(s.nomeRazaoSocial) = LOWER(:nome)")
    Optional<Supplier> findByNomeIgnoreCase(@Param("nome") String nome);

    /**
     * Busca fornecedor por CPF/CNPJ.
     */
    Optional<Supplier> findByCpfCnpj(String cpfCnpj);

    /**
     * Lista fornecedores ativos.
     */
    List<Supplier> findByStatusOrderByNomeRazaoSocialAsc(String status);

    /**
     * Lista todos os fornecedores ordenados por razão social.
     */
    List<Supplier> findAllByOrderByNomeRazaoSocialAsc();

    /**
     * Pesquisa fornecedores por nome/fantasia (termo parcial).
     */
    @Query("SELECT s FROM Supplier s WHERE LOWER(s.nomeRazaoSocial) LIKE LOWER(CONCAT('%', :termo, '%')) " +
           "OR LOWER(s.nomeFantasia) LIKE LOWER(CONCAT('%', :termo, '%')) " +
           "ORDER BY s.nomeRazaoSocial ASC")
    List<Supplier> pesquisarPorNome(@Param("termo") String termo);

    /**
     * Pesquisa por documento exato.
     */
    @Query("SELECT s FROM Supplier s WHERE s.cpfCnpj = :documento")
    Optional<Supplier> pesquisarPorDocumento(@Param("documento") String documento);
}
