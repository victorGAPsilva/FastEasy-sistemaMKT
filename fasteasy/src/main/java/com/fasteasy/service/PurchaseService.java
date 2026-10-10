package com.fasteasy.service;

import com.fasteasy.domain.Purchase;
import com.fasteasy.domain.PurchaseItem;
import com.fasteasy.domain.Supplier;
import com.fasteasy.domain.Product;
import com.fasteasy.repository.PurchaseRepository;
import com.fasteasy.repository.SupplierRepository;
import com.fasteasy.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Serviço mínimo para compras (Parte 6 - base).
 *
 * <p>Fornece operações básicas para que o módulo financeiro possa
 * referenciar compras. O módulo completo de compras expandirá este serviço.</p>
 */
@Service
@Transactional
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;

    public PurchaseService(PurchaseRepository purchaseRepository,
                           SupplierRepository supplierRepository,
                           ProductRepository productRepository) {
        this.purchaseRepository = purchaseRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
    }

    public Purchase cadastrar(Purchase compra) {
        if (compra.getFornecedor() == null || compra.getFornecedor().getId() == null) {
            throw new IllegalArgumentException("Fornecedor é obrigatório");
        }
        Supplier fornecedor = supplierRepository.findById(compra.getFornecedor().getId())
                .orElseThrow(() -> new IllegalArgumentException("Fornecedor não encontrado: " + compra.getFornecedor().getId()));
        compra.setFornecedor(fornecedor);

        if (compra.getDataEmissao() == null) {
            compra.setDataEmissao(LocalDateTime.now());
        }
        if (compra.getStatus() == null) {
            compra.setStatus("PENDING");
        }
        compra.setValorTotal(compra.getValorTotal() != null ? compra.getValorTotal() : BigDecimal.ZERO);
        compra.setDataCriacao(LocalDateTime.now());

        return purchaseRepository.save(compra);
    }

    public Purchase adicionarItem(Long compraId, Long produtoId, BigDecimal quantidade, BigDecimal precoUnitario) {
        Purchase compra = purchaseRepository.findById(compraId)
                .orElseThrow(() -> new IllegalArgumentException("Compra não encontrada: " + compraId));

        Product produto = productRepository.findById(produtoId)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + produtoId));

        PurchaseItem item = new PurchaseItem();
        item.setCompra(compra);
        item.setProduto(produto);
        item.setQuantidade(quantidade);
        item.setPrecoUnitario(precoUnitario);
        item.setTotal(precoUnitario.multiply(quantidade));

        compra.getItens().add(item);
        compra.setValorTotal(compra.getValorTotal().add(item.getTotal()));
        compra.setDataAtualizacao(LocalDateTime.now());

        return purchaseRepository.save(compra);
    }

    public void confirmarRecebimento(Long compraId) {
        Purchase compra = purchaseRepository.findById(compraId)
                .orElseThrow(() -> new IllegalArgumentException("Compra não encontrada: " + compraId));
        compra.setStatus("RECEIVED");
        compra.setDataAtualizacao(LocalDateTime.now());
        purchaseRepository.save(compra);
    }

    @Transactional(readOnly = true)
    public Optional<Purchase> buscarPorId(Long id) {
        return purchaseRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Purchase> pesquisar(Long fornecedorId, String status, LocalDateTime inicio, LocalDateTime fim) {
        return purchaseRepository.pesquisar(fornecedorId, status, inicio, fim);
    }

    @Transactional(readOnly = true)
    public boolean existsPayableAccountByCompraId(Long compraId) {
        return purchaseRepository.existsPayableAccountByCompraId(compraId);
    }
}