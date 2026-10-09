package com.fasteasy.validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Validação de CPF e CNPJ")
class DocumentoValidatorTest {

    @Test
    @DisplayName("CPF válido deve ser aceito (com e sem máscara)")
    void deveAceitarCpfValido() {
        assertTrue(DocumentoValidator.isCpfValido("52998224725"));
        assertTrue(DocumentoValidator.isCpfValido("529.982.247-25"));
    }

    @Test
    @DisplayName("CPF inválido deve ser rejeitado")
    void deveRejeitarCpfInvalido() {
        assertFalse(DocumentoValidator.isCpfValido("52998224724"));
        assertFalse(DocumentoValidator.isCpfValido("11111111111"));
        assertFalse(DocumentoValidator.isCpfValido("123"));
    }

    @Test
    @DisplayName("CNPJ válido deve ser aceito (com e sem máscara)")
    void deveAceitarCnpjValido() {
        assertTrue(DocumentoValidator.isCnpjValido("11222333000181"));
        assertTrue(DocumentoValidator.isCnpjValido("11.222.333/0001-81"));
    }

    @Test
    @DisplayName("CNPJ inválido deve ser rejeitado")
    void deveRejeitarCnpjInvalido() {
        assertFalse(DocumentoValidator.isCnpjValido("11222333000180"));
        assertFalse(DocumentoValidator.isCnpjValido("00000000000000"));
    }

    @Test
    @DisplayName("isDocumentoValido reconhece CPF e CNPJ")
    void deveReconhecerDocumentos() {
        assertTrue(DocumentoValidator.isDocumentoValido("52998224725"));
        assertTrue(DocumentoValidator.isDocumentoValido("11222333000181"));
        assertFalse(DocumentoValidator.isDocumentoValido("123456"));
    }

    @Test
    @DisplayName("Somente dígitos remove máscaras")
    void deveRemoverMascara() {
        assertEquals("52998224725", DocumentoValidator.somenteDigitos("529.982.247-25"));
        assertEquals("11222333000181", DocumentoValidator.somenteDigitos("11.222.333/0001-81"));
    }
}
