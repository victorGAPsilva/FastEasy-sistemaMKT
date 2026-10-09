package com.fasteasy.validator;

/**
 * Validação de documentos brasileiros (CPF e CNPJ).
 * Utilitário puro, sem dependência de framework, reutilizado por
 * validadores Java e serviços de aplicação.
 */
public final class DocumentoValidator {

    private DocumentoValidator() {
    }

    /**
     * Remove máscaras deixando apenas dígitos.
     */
    public static String somenteDigitos(String valor) {
        if (valor == null) {
            return null;
        }
        return valor.replaceAll("\\D", "");
    }

    /**
     * Verifica se o documento informado é um CPF válido (11 dígitos + DV).
     */
    public static boolean isCpfValido(String cpf) {
        String d = somenteDigitos(cpf);
        if (d == null || d.length() != 11 || todosDigitosIguais(d)) {
            return false;
        }
        int digito1 = calcularDigito(d.substring(0, 9), new int[]{10, 9, 8, 7, 6, 5, 4, 3, 2});
        int digito2 = calcularDigito(d.substring(0, 10), new int[]{11, 10, 9, 8, 7, 6, 5, 4, 3, 2});
        return d.charAt(9) - '0' == digito1 && d.charAt(10) - '0' == digito2;
    }

    /**
     * Verifica se o documento informado é um CNPJ válido (14 dígitos + DV).
     */
    public static boolean isCnpjValido(String cnpj) {
        String d = somenteDigitos(cnpj);
        if (d == null || d.length() != 14 || todosDigitosIguais(d)) {
            return false;
        }
        int digito1 = calcularDigito(d.substring(0, 12),
                new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        int digito2 = calcularDigito(d.substring(0, 13),
                new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        return d.charAt(12) - '0' == digito1 && d.charAt(13) - '0' == digito2;
    }

    /**
     * Valida CPF ou CNPJ de acordo com o tamanho.
     */
    public static boolean isDocumentoValido(String documento) {
        String d = somenteDigitos(documento);
        if (d == null || d.isEmpty()) {
            return false;
        }
        if (d.length() == 11) {
            return isCpfValido(d);
        }
        if (d.length() == 14) {
            return isCnpjValido(d);
        }
        return false;
    }

    private static int calcularDigito(String base, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (base.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private static boolean todosDigitosIguais(String valor) {
        for (int i = 1; i < valor.length(); i++) {
            if (valor.charAt(i) != valor.charAt(0)) {
                return false;
            }
        }
        return true;
    }
}
