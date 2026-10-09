package com.fasteasy.repository;

import com.fasteasy.domain.StockMovement;
import com.fasteasy.domain.TipoMovimentacaoEstoque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório para operações com StockMovement (movimentações de estoque).
 */
@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByProdutoIdOrderByDataMovimentacaoDesc(Long produtoId);

    List<StockMovement> findByVendaId(Long vendaId);

    long countByVendaIdAndTipo(Long vendaId, TipoMovimentacaoEstoque tipo);
}
