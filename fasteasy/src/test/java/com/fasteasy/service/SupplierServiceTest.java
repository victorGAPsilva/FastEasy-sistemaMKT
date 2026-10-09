package com.fasteasy.service;

import com.fasteasy.domain.Supplier;
import com.fasteasy.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Serviço de Fornecedores")
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private SupplierService supplierService;

    private Supplier fornecedor;

    @BeforeEach
    void setUp() {
        fornecedor = new Supplier("Distribuidora Alfa LTDA");
        fornecedor.setCpfCnpj("11.222.333/0001-81");
        fornecedor.setTelefone("(11) 98765-4321");
        fornecedor.setEmail("contato@alfa.com");
    }

    @Test
    @DisplayName("Deve cadastrar fornecedor válido")
    void deveCadastrarFornecedor() {
        when(supplierRepository.findByNomeIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(supplierRepository.pesquisarPorDocumento(anyString())).thenReturn(Optional.empty());
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(inv -> {
            Supplier s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        Supplier salvo = supplierService.cadastrar(fornecedor);

        assertNotNull(salvo.getId());
        assertEquals("ATIVO", salvo.getStatus());
        assertNotNull(salvo.getDataCadastro());
    }

    @Test
    @DisplayName("Deve rejeitar CNPJ com dígito verificador inválido")
    void deveRejeitarCnpjInvalido() {
        fornecedor.setCpfCnpj("11.222.333/0001-80");
        assertThrows(IllegalArgumentException.class, () -> supplierService.cadastrar(fornecedor));
        verify(supplierRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar CPF com dígito verificador inválido")
    void deveRejeitarCpfInvalido() {
        fornecedor.setCpfCnpj("529.982.247-24");
        assertThrows(IllegalArgumentException.class, () -> supplierService.cadastrar(fornecedor));
        verify(supplierRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve aceitar fornecedor sem documento (campo opcional)")
    void deveAceitarSemDocumento() {
        fornecedor.setCpfCnpj(null);
        when(supplierRepository.findByNomeIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(inv -> {
            Supplier s = inv.getArgument(0);
            s.setId(2L);
            return s;
        });

        Supplier salvo = supplierService.cadastrar(fornecedor);
        assertNotNull(salvo.getId());
    }

    @Test
    @DisplayName("Deve rejeitar e-mail inválido")
    void deveRejeitarEmailInvalido() {
        fornecedor.setEmail("email-invalido");
        assertThrows(IllegalArgumentException.class, () -> supplierService.cadastrar(fornecedor));
        verify(supplierRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve desativar fornecedor")
    void deveDesativarFornecedor() {
        Supplier existente = new Supplier("Fornecedor X");
        existente.setId(9L);
        when(supplierRepository.findById(9L)).thenReturn(Optional.of(existente));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(inv -> inv.getArgument(0));

        supplierService.desativar(9L);

        assertEquals("INATIVO", existente.getStatus());
        verify(supplierRepository).save(existente);
    }

    @Test
    @DisplayName("Não deve permitir fornecedor com razão social duplicada")
    void naoDevePermitirDuplicidade() {
        when(supplierRepository.findByNomeIgnoreCase(anyString()))
                .thenReturn(Optional.of(new Supplier("Distribuidora Alfa LTDA")));
        assertThrows(IllegalArgumentException.class, () -> supplierService.cadastrar(fornecedor));
        verify(supplierRepository, never()).save(any());
    }
}
