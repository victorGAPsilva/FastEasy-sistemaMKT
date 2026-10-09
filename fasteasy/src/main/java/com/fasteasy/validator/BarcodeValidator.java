package com.fasteasy.validator;

/**
 * Validação de códigos de barras.
 *
 * Aceita:
 * <ul>
 *     <li>EAN-8 (8 dígitos, com dígito verificador válido);</li>
 *     <li>EAN-13 (13 dígitos, com dígito verificador válido);</li>
 *     <li>Códigos internos (qualquer outro formato não vazio).</li>
 * </ul>
 *
 * Códigos internos válidos NÃO são rejeitados.
 */
public final class BarcodeValidator {

    private BarcodeValidator() {
    }

    /**
     * Verifica se o código informado é válido para uso no FastEasy.
     * Um EAN de 8 ou 13 dígitos precisa ter dígito verificador correto;
     * qualquer outro código não vazio é tratado como código interno.
     */
    public static boolean isCodigoValido(String codigo) {
        if (codigo == null) {
            return false;
        }
        String c = codigo.trim();
        if (c.isEmpty()) {
            return false;
        }
        if (somenteDigitos(c)) {
            if (c.length() == 8) {
                return isEan8(c);
            }
            if (c.length() == 13) {
                return isEan13(c);
            }
        }
        // Código interno
        return true;
    }

    public static boolean isEan8(String codigo) {
        String c = BarcodeValidator.apenasDigitos(codigo);
        return c.length() == 8 && digitoValido(c, 8);
    }

    public static boolean isEan13(String codigo) {
        String c = apenasDigitos(codigo);
        return c.length() == 13 && digitoValido(c, 13);
    }

    /**
     * Calcula o dígito verificador EAN para a base informada
     * (base = código sem o último dígito).
     */
    public static int calcularDigitoVerificador(String base) {
        String b = apenasDigitos(base);
        int soma = 0;
        // Pesos 3 e 1 alternados, da direita para a esquerda.
        boolean peso3 = true;
        for (int i = b.length() - 1; i >= 0; i--) {
            int digito = b.charAt(i) - '0';
            soma += peso3 ? digito * 3 : digito;
            peso3 = !peso3;
        }
        int resto = soma % 10;
        return resto == 0 ? 0 : 10 - resto;
    }

    private static boolean digitoValido(String codigo, int tamanho) {
        String base = codigo.substring(0, tamanho - 1);
        int informado = codigo.charAt(tamanho - 1) - '0';
        return calcularDigitoVerificador(base) == informado;
    }

    private static boolean somenteDigitos(String valor) {
        return valor.matches("\\d+");
    }

    private static String apenasDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }
}
