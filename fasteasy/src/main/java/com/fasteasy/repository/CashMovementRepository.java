package com.fasteasy.repository;

import com.fasteasy.domain.CashMovement;
import com.fasteasy.domain.TipoMovimentacaoCaixa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório para operações com CashMovement (movimentações de caixa).
 *
 * <p>Não há métodos de atualização/remoção: as movimentações são imutáveis
 * após o registro — correções devem ser feitas por movimentação compensatória.</p>
 */
@Repository
public interface CashMovementRepository extends JpaRepository<CashMovement, Long> {

    List<CashMovement> findBySessaoIdOrderByDataHoraAsc(Long sessaoId);

    List<CashMovement> findBySessaoIdAndTipoOrderByDataHoraAsc(Long sessaoId, TipoMovimentacaoCaixa tipo);

    long countBySessaoId(Long sessaoId);
}
