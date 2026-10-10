package com.fasteasy.presentation.controller;

import com.fasteasy.domain.Category;
import com.fasteasy.domain.Product;
import com.fasteasy.domain.Supplier;
import com.fasteasy.presentation.util.TarefaAssincrona;
import com.fasteasy.presentation.view.*;
import com.fasteasy.service.CashRegisterService;
import com.fasteasy.service.CashSessionService;
import com.fasteasy.service.CategoryService;
import com.fasteasy.service.ExpenseCategoryService;
import com.fasteasy.service.FinancialService;
import com.fasteasy.service.PayableAccountService;
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
    private final TelaPdv telaPdv;
    private final TelaHistoricoVendas telaHistoricoVendas;
    private final TelaCaixa telaCaixa;
    private final TelaHistoricoCaixas telaHistoricoCaixas;
    private final TelaContasPagar telaContasPagar;
    private final TelaFluxoCaixa telaFluxoCaixa;

    @FXML private VBox conteudoPrincipal;
    @FXML private Label tituloAreaPrincipal;
    @FXML private Label statusBar;
    @FXML private Button btnPdv;
    @FXML private Button btnProdutos;
    @FXML private Button btnCategorias;
    @FXML private Button btnFornecedores;
    @FXML private Button btnVendas;
    @FXML private Button btnCaixa;
    @FXML private Button btnHistoricoCaixas;
    @FXML private Button btnContasPagar;
    @FXML private Button btnFluxoCaixa;
    @FXML private Button btnSair;

    private VBox telaProdutos;
    private VBox telaCategorias;
    private VBox telaFornecedores;

    public MainController(ProductService productService,
                          CategoryService categoryService,
                          SupplierService supplierService,
                          ExpenseCategoryService expenseCategoryService,
                          FinancialService financialService,
                          PayableAccountService payableAccountService,
                          FormProduto formProduto,
                          TabelaProdutos tabelaProdutos,
                          FormCategoria formCategoria,
                          TabelaCategorias tabelaCategorias,
                          FormFornecedor formFornecedor,
                          TabelaFornecedores tabelaFornecedores,
                          TelaPdv telaPdv,
                          TelaHistoricoVendas telaHistoricoVendas,
                          TelaCaixa telaCaixa,
                          TelaHistoricoCaixas telaHistoricoCaixas,
                          TelaContasPagar telaContasPagar,
                          TelaFluxoCaixa telaFluxoCaixa) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.supplierService = supplierService;
        this.formProduto = formProduto;
        this.tabelaProdutos = tabelaProdutos;
        this.formCategoria = formCategoria;
        this.tabelaCategorias = tabelaCategorias;
        this.formFornecedor = formFornecedor;
        this.tabelaFornecedores = tabelaFornecedores;
        this.telaPdv = telaPdv;
        this.telaHistoricoVendas = telaHistoricoVendas;
        this.telaCaixa = telaCaixa;
        this.telaHistoricoCaixas = telaHistoricoCaixas;
        this.telaContasPagar = telaContasPagar;
        this.telaFluxoCaixa = telaFluxoCaixa;
    }

    @FXML
    public void initialize() {
        montarTelas();
        configurarAtalhos();
        handleMenuPdv(null);
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
    private void handleMenuPdv(ActionEvent event) {
        tituloAreaPrincipal.setText("PDV — Frente de Caixa");
        definirStatus("Registre produtos pelo código de barras e finalize a venda (F4)");
        conteudoPrincipal.getChildren().setAll(telaPdv);
        telaPdv.aoExibir();
        marcarAtivo(btnPdv);
    }

    @FXML
    private void handleMenuVendas(ActionEvent event) {
        tituloAreaPrincipal.setText("Histórico de Vendas");
        definirStatus("Consulta de vendas realizadas");
        conteudoPrincipal.getChildren().setAll(telaHistoricoVendas);
        telaHistoricoVendas.atualizarLista();
        marcarAtivo(btnVendas);
    }

    @FXML
    private void handleMenuCaixa(ActionEvent event) {
        tituloAreaPrincipal.setText("Gestão de Caixa");
        definirStatus("Abertura, movimentações e fechamento de caixa");
        conteudoPrincipal.getChildren().setAll(telaCaixa);
        telaCaixa.aoExibir();
        marcarAtivo(btnCaixa);
    }

    @FXML
    private void handleMenuHistoricoCaixas(ActionEvent event) {
        tituloAreaPrincipal.setText("Histórico de Caixas");
        definirStatus("Consulta de sessões e fechamentos anteriores");
        conteudoPrincipal.getChildren().setAll(telaHistoricoCaixas);
        telaHistoricoCaixas.atualizarLista();
        marcarAtivo(btnHistoricoCaixas);
    }

    @FXML
    private void handleMenuContasPagar(ActionEvent event) {
        tituloAreaPrincipal.setText("Contas a Pagar");
        definirStatus("Gestão de obrigações financeiras e despesas");
        conteudoPrincipal.getChildren().setAll(telaContasPagar);
        telaContasPagar.aoExibir();
        marcarAtivo(btnContasPagar);
    }

    @FXML
    private void handleMenuFluxoCaixa(ActionEvent event) {
        tituloAreaPrincipal.setText("Fluxo de Caixa");
        definirStatus("Visualização de fluxo financeiro previsto e realizado");
        conteudoPrincipal.getChildren().setAll(telaFluxoCaixa);
        telaFluxoCaixa.aoExibir();
        marcarAtivo(btnFluxoCaixa);
    }

    private void configurarAtalhos() {
        conteudoPrincipal.sceneProperty().addListener((observable, anterior, scene) -> {
            if (scene != null) {
                telaPdv.registrarAtalhos(scene);
            }
        });
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
        for (Button b : new Button[]{btnPdv, btnProdutos, btnCategorias, btnFornecedores, btnVendas, btnCaixa, btnHistoricoCaixas, btnContasPagar, btnFluxoCaixa}) {
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
