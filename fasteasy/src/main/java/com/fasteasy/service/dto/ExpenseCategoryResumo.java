package com.fasteasy.service.dto;

import com.fasteasy.domain.ExpenseCategory;

/**
 * DTO de leitura para categoria de despesa.
 */
public record ExpenseCategoryResumo(
        Long id,
        String nome,
        String descricao,
        String status
) {
    public static ExpenseCategoryResumo from(ExpenseCategory c) {
        return new ExpenseCategoryResumo(c.getId(), c.getNome(), c.getDescricao(), c.getStatus());
    }
}