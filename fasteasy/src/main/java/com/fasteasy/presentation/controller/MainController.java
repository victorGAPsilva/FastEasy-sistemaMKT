package com.fasteasy.presentation.controller;

import com.fasteasy.domain.Category;
import com.fasteasy.domain.Product;
import com.fasteasy.domain.Supplier;
import com.fasteasy.presentation.util.TarefaAssincrona;
import com.fasteasy.presentation.view.*;
import com.fasteasy.service.CategoryService;
import com.fasteasy.service.ProductService;
import com.fasteasy.service.SupplierService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

/**
 * Controlador principal da aplicação FastEasy.
 * Responsável pela navegação entre as telas e pelo layout base.
 * Todas as regras de negócio são delegadas aos serviços de aplicação.
 */
@Component
public class MainController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final SupplierService supplierService;

    private final FormProduto formProduto;
    private final TabelaProdutos tabelaProdutos;
    private final FormCategoria formCategoria;
    private final TabelaCategorias tabelaCategorias;
    private final FormFornecedor formFornecedor;
    private final TabelaFornecedores tabelaFornecedores;

    @FXML private VBox conteudoPrincipal;
    @FXML private Label tituloAreaPrincipal;
    @FXML private Label statusBar;
    @FXML private Button btnProdutos;
    @FXML private Button btnCategorias;
    @FXML private Button btnFornecedores;
    @FXML private Button btnSair;

    private VBox telaProdutos;
    private VBox telaCategorias;
    private VBox telaFornecedores;

    public MainController(ProductService productService,
                          CategoryService categoryService,
                          SupplierService supplierService,
                          FormProduto formProduto,
                          TabelaProdutos tabelaProdutos,
                          FormCategoria formCategoria,
                          TabelaCategorias tabelaCategorias,
                          FormFornecedor formFornecedor,
                          TabelaFornecedores tabelaFornecedores) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.supplierService = supplierService;
        this.formProduto = formProduto;
        this.tabelaProdutos = tabelaProdutos;
        this.formCategoria = formCategoria;
        this.tabelaCategorias = tabelaCategorias;
        this.formFornecedor = formFornecedor;
        this.tabelaFornecedores = tabelaFornecedores;
    }

    @FXML
    public void initialize() {
        montarTelas();
        handleMenuProdutos(null);
    }

    private void montarTelas() {
        telaProdutos = new VBox(16, formProduto, tabelaProdutos);
        tabelaProdutos.setOnEditar(formProduto::editar);
        tabelaProdutos.setOnAtivarDesativar(this::alternarStatusProduto);

        telaCategorias = new VBox(16, formCategoria, tabelaCategorias);
        tabelaCategorias.setOnEditar(formCategoria::editar);
        tabelaCategorias.setOnAtivarDesativar(this::alternarStatusCategoria);

        telaFornecedores = new VBox(16, formFornecedor, tabelaFornecedores);
        tabelaFornecedores.setOnEditar(formFornecedor::editar);
        tabelaFornecedores.setOnAtivarDesativar(this::alternarStatusFornecedor);
    }

    @FXML
    private void handleMenuProdutos(ActionEvent event) {
        tituloAreaPrincipal.setText("Produtos");
        definirStatus("Gerenciamento de produtos");
        conteudoPrincipal.getChildren().setAll(telaProdutos);
        formProduto.recarregarDados();
        tabelaProdutos.atualizarLista();
        marcarAtivo(btnProdutos);
    }

    @FXML
    private void handleMenuCategorias(ActionEvent event) {
        tituloAreaPrincipal.setText("Categorias");
        definirStatus("Gerenciamento de categorias");
        conteudoPrincipal.getChildren().setAll(telaCategorias);
        tabelaCategorias.atualizarLista();
        formProduto.recarregarDados();
        marcarAtivo(btnCategorias);
    }

    @FXML
    private void handleMenuFornecedores(ActionEvent event) {
        tituloAreaPrincipal.setText("Fornecedores");
        definirStatus("Gerenciamento de fornecedores");
        conteudoPrincipal.getChildren().setAll(telaFornecedores);
        tabelaFornecedores.atualizarLista();
        formProduto.recarregarDados();
        marcarAtivo(btnFornecedores);
    }

    @FXML
    private void handleSair(ActionEvent event) {
        if (btnSair != null && btnSair.getScene() != null) {
            ((Stage) btnSair.getScene().getWindow()).close();
        }
    }

    private void alternarStatusProduto(Product produto) {
        boolean desativar = produto.isAtivo();
        if (desativar && !confirmar("Desativar produto",
                "Deseja desativar o produto \"" + produto.getNome() + "\"?")) {
            return;
        }

        final Long id = produto.getId();
        final String nome = produto.getNome();
        TarefaAssincrona.executar(
                () -> {
                    if (desativar) {
                        productService.desativar(id);
                    } else {
                        Product atualizado = copiarProduto(produto);
                        atualizado.setStatus("ATIVO");
                        productService.editar(id, atualizado);
                    }
                    return null;
                },
                ignorado -> {
                    tabelaProdutos.atualizarLista();
                    definirStatus((desativar ? "Produto desativado: " : "Produto reativado: ") + nome);
                },
                e -> erro("Não foi possível alterar o status", e.getMessage()));
    }

    private Product copiarProduto(Product p) {
        Product c = new Product();
        c.setNome(p.getNome());
        c.setDescricao(p.getDescricao());
        c.setCodigoBarras(p.getCodigoBarras());
        c.setSku(p.getSku());
        c.setCategoria(p.getCategoria());
        c.setMarca(p.getMarca());
        c.setUnidadeMedida(p.getUnidadeMedida());
        c.setCustoAquisicao(p.getCustoAquisicao());
        c.setPrecoVenda(p.getPrecoVenda());
        c.setEstoqueMinimo(p.getEstoqueMinimo());
        c.setEstoqueMaximo(p.getEstoqueMaximo());
        c.setFornecedorPrincipal(p.getFornecedorPrincipal());
        c.setStatus(p.getStatus());
        return c;
    }

    private void alternarStatusCategoria(Category categoria) {
        boolean desativar = "ATIVO".equals(categoria.getStatus());
        if (desativar && !confirmar("Desativar categoria",
                "Deseja desativar a categoria \"" + categoria.getNome() + "\"?")) {
            return;
        }

        final Long id = categoria.getId();
        final String nome = categoria.getNome();
        final String descricao = categoria.getDescricao();
        TarefaAssincrona.executar(
                () -> {
                    if (desativar) {
                        categoryService.desativar(id);
                    } else {
                        Category atualizada = new Category();
                        atualizada.setNome(nome);
                        atualizada.setDescricao(descricao);
                        atualizada.setStatus("ATIVO");
                        categoryService.editar(id, atualizada);
                    }
                    return null;
                },
                ignorado -> {
                    tabelaCategorias.atualizarLista();
                    definirStatus((desativar ? "Categoria desativada: " : "Categoria reativada: ") + nome);
                },
                e -> erro("Não foi possível alterar o status", e.getMessage()));
    }

    private void alternarStatusFornecedor(Supplier fornecedor) {
        boolean desativar = "ATIVO".equals(fornecedor.getStatus());
        if (desativar && !confirmar("Desativar fornecedor",
                "Deseja desativar o fornecedor \"" + fornecedor.getNomeRazaoSocial() + "\"?")) {
            return;
        }

        final Long id = fornecedor.getId();
        final String nome = fornecedor.getNomeRazaoSocial();
        final Supplier copia = copiarFornecedor(fornecedor);
        TarefaAssincrona.executar(
                () -> {
                    if (desativar) {
                        supplierService.desativar(id);
                    } else {
                        copia.setStatus("ATIVO");
                        supplierService.editar(id, copia);
                    }
                    return null;
                },
                ignorado -> {
                    tabelaFornecedores.atualizarLista();
                    definirStatus((desativar ? "Fornecedor desativado: " : "Fornecedor reativado: ") + nome);
                },
                e -> erro("Não foi possível alterar o status", e.getMessage()));
    }

    private Supplier copiarFornecedor(Supplier f) {
        Supplier s = new Supplier();
        s.setNomeRazaoSocial(f.getNomeRazaoSocial());
        s.setNomeFantasia(f.getNomeFantasia());
        s.setCpfCnpj(f.getCpfCnpj());
        s.setTelefone(f.getTelefone());
        s.setEmail(f.getEmail());
        s.setEndereco(f.getEndereco());
        s.setObservacoes(f.getObservacoes());
        s.setStatus(f.getStatus());
        return s;
    }

    private void marcarAtivo(Button ativo) {
        for (Button b : new Button[]{btnProdutos, btnCategorias, btnFornecedores}) {
            if (b != null) {
                b.getStyleClass().remove("active");
            }
        }
        if (ativo != null && !ativo.getStyleClass().contains("active")) {
            ativo.getStyleClass().add("active");
        }
    }

    private void definirStatus(String texto) {
        if (statusBar != null) {
            statusBar.setText(texto);
        }
    }

    private boolean confirmar(String titulo, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        return alert.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    private void erro(String titulo, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}
