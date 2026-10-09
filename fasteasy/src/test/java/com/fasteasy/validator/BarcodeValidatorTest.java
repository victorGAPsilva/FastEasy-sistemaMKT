package com.fasteasy.validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Validação de códigos de barras (EAN-8, EAN-13 e interno)")
class BarcodeValidatorTest {

    @Test
    @DisplayName("EAN-13 válido deve ser aceito")
    void deveAceitarEan13Valido() {
        assertTrue(BarcodeValidator.isEan13("5901234123457"));
        assertTrue(BarcodeValidator.isCodigoValido("5901234123457"));
    }

    @Test
    @DisplayName("EAN-13 com dígito verificador errado deve ser rejeitado")
    void deveRejeitarEan13Invalido() {
        assertFalse(BarcodeValidator.isEan13("5901234123456"));
        assertFalse(BarcodeValidator.isCodigoValido("5901234123456"));
    }

    @Test
    @DisplayName("EAN-8 válido deve ser aceito")
    void deveAceitarEan8Valido() {
        assertTrue(BarcodeValidator.isEan8("96385074"));
        assertTrue(BarcodeValidator.isCodigoValido("96385074"));
    }

    @Test
    @DisplayName("EAN-8 com dígito verificador errado deve ser rejeitado")
    void deveRejeitarEan8Invalido() {
        assertFalse(BarcodeValidator.isEan8("96385070"));
        assertFalse(BarcodeValidator.isCodigoValido("96385070"));
    }

    @Test
    @DisplayName("Código interno válido não deve ser rejeitado")
    void deveAceitarCodigoInterno() {
        assertTrue(BarcodeValidator.isCodigoValido("PROD-0001"));
        assertTrue(BarcodeValidator.isCodigoValido("123"));
    }

    @Test
    @DisplayName("Código vazio ou nulo deve ser rejeitado")
    void deveRejeitarVazio() {
        assertFalse(BarcodeValidator.isCodigoValido(null));
        assertFalse(BarcodeValidator.isCodigoValido(""));
        assertFalse(BarcodeValidator.isCodigoValido("   "));
    }

    @Test
    @DisplayName("Dígito verificador EAN deve ser calculado corretamente")
    void deveCalcularDigitoVerificador() {
        assertEquals(7, BarcodeValidator.calcularDigitoVerificador("590123412345"));
        assertEquals(4, BarcodeValidator.calcularDigitoVerificador("9638507"));
    }
}
