package com.fasteasy.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utilitário de valores monetários do FastEasy.
 *
 * <p>Centraliza o arredondamento monetário (2 casas, HALF_UP) e a
 * formatação em Real (pt-BR), garantindo consistência em todo o sistema.
 * Nunca utilize {@code float}/{@code double} para dinheiro.</p>
 */
public final class Dinheiro {

    /** Casas decimais usadas em valores monetários. */
    public static final int ESCALA = 2;

    /** Modo de arredondamento monetário. */
    public static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_UP;

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private Dinheiro() {
    }

    /** Arredonda para 2 casas decimais (HALF_UP). Retorna {@code null} se o valor for nulo. */
    public static BigDecimal arredondar(BigDecimal valor) {
        return valor == null ? null : valor.setScale(ESCALA, ARREDONDAMENTO);
    }

    /** Retorna {@link BigDecimal#ZERO} quando o valor é nulo. */
    public static BigDecimal ouZero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    /** Formata um valor como moeda brasileira, ex.: {@code R$ 1.234,56}. */
    public static String formatar(BigDecimal valor) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(PT_BR);
        return nf.format(arredondar(ouZero(valor)));
    }

    /** Formata uma quantidade (até 3 casas, sem zeros à direita), ex.: {@code 1,5}. */
    public static String formatarQuantidade(BigDecimal valor) {
        if (valor == null) {
            return "0";
        }
        NumberFormat nf = NumberFormat.getNumberInstance(PT_BR);
        nf.setMinimumFractionDigits(0);
        nf.setMaximumFractionDigits(3);
        return nf.format(valor);
    }
}
