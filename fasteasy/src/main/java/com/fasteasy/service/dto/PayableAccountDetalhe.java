package com.fasteasy.service.dto;

import com.fasteasy.domain.ExpenseCategory;
import com.fasteasy.domain.PayableAccountStatus;
import com.fasteasy.domain.PayableInstallmentStatus;
import com.fasteasy.domain.Supplier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detalhe completo de uma conta a pagar: resumo + parcelas + pagamentos.
 */
public record PayableAccountDetalhe(
        PayableAccountResumo resumo,
        List<PayableInstallmentResumo> parcelas,
        List<FinancialPaymentResumo> pagamentos
) {
}