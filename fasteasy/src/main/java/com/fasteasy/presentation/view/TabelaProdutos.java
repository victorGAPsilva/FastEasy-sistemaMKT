package com.fasteasy.presentation.view;

import com.fasteasy.domain.Category;
import com.fasteasy.domain.Product;
import com.fasteasy.presentation.util.TarefaAssincrona;
import com.fasteasy.service.CategoryService;
import com.fasteasy.service.ProductService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Tabela de listagem de produtos com pesquisa, filtros e ações.
 * As consultas são feitas exclusivamente pelos serviços de aplicação,
 * sempre fora da thread da interface (ver {@link TarefaAssincrona}).
 */
@Component
public class TabelaProdutos extends VBox {

    private static final Logger log = LoggerFactory.getLogger(TabelaProdutos.class);


    private final ProductService productService;
    private final CategoryService categoryService;

    private final TableView<Product> tabela = new TableView<>();
    private final TextField txtPesquisa = new TextField();
    private final ComboBox<String> cmbFiltroStatus = new ComboBox<>();
    private final ComboBox<Category> cmbFiltroCategoria = new ComboBox<>();

    private final ObservableList<Product> dados = FXCollections.observableArrayList();
    private Consumer<Product> onEditar;
    private Consumer<Product> onAtivarDesativar;

    @Autowired
    public TabelaProdutos(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
        inicializarComponentes();
        configurarColunas();
        carregarFiltroCategorias();
    }

    private void inicializarComponentes() {
        setSpacing(10);
        setPadding(new Insets(10, 0, 0, 0));

        Label titulo = new Label("Lista de Produtos");
        titulo.getStyleClass().add("section-title");

        HBox filtros = new HBox(10);
        filtros.getStyleClass().add("card");
        filtros.setPadding(new Insets(10));

        txtPesquisa.setPromptText("Pesquisar por nome, SKU ou código de barras");
        txtPesquisa.setPrefWidth(280);
        txtPesquisa.textProperty().addListener((obs, old, novo) -> filtrarTabela());

        cmbFiltroStatus.setPromptText("Status");
        cmbFiltroStatus.getItems().addAll("TODOS", "ATIVO", "INATIVO");
        cmbFiltroStatus.getSelectionModel().selectFirst();
        cmbFiltroStatus.setOnAction(e -> filtrarTabela());

        cmbFiltroCategoria.setPromptText("Categoria");
        cmbFiltroCategoria.setOnAction(e -> filtrarTabela());
        cmbFiltroCategoria.setPrefWidth(200);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        filtros.getChildren().addAll(
                new Label("Pesquisa:"), txtPesquisa,
                new Label("Status:"), cmbFiltroStatus,
                new Label("Categoria:"), cmbFiltroCategoria,
                spacer
        );

        tabela.setItems(dados);
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabela.setPrefHeight(400);
        VBox.setVgrow(tabela, Priority.ALWAYS);

        getChildren().addAll(titulo, filtros, tabela);
    }

    private void configurarColunas() {
        TableColumn<Product, String> colNome = new TableColumn<>("Nome");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colNome.setMinWidth(200);

        TableColumn<Product, String> colSku = new TableColumn<>("SKU");
        colSku.setCellValueFactory(new PropertyValueFactory<>("sku"));
        colSku.setMinWidth(100);

        TableColumn<Product, String> colCodigoBarras = new TableColumn<>("Código de Barras");
        colCodigoBarras.setCellValueFactory(new PropertyValueFactory<>("codigoBarras"));
        colCodigoBarras.setMinWidth(140);

        TableColumn<Product, String> colCategoria = new TableColumn<>("Categoria");
        colCategoria.setCellValueFactory(cellData -> {
            Product p = cellData.getValue();
            String nome = p.getCategoria() != null ? p.getCategoria().getNome() : "";
            return new javafx.beans.property.SimpleStringProperty(nome);
        });
        colCategoria.setMinWidth(140);

        TableColumn<Product, String> colPreco = new TableColumn<>("Preço de Venda");
        colPreco.setCellValueFactory(cellData -> {
            Product p = cellData.getValue();
            String valor = p.getPrecoVenda() != null
                    ? String.format("R$ %.2f", p.getPrecoVenda())
                    : "R$ 0,00";
            return new javafx.beans.property.SimpleStringProperty(valor);
        });
        colPreco.setMinWidth(110);

        TableColumn<Product, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setStyle("ATIVO".equals(item) ? "-fx-text-fill: #27ae60; -fx-font-weight: bold;"
                            : "-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                }
            }
        });
        colStatus.setMinWidth(90);

        TableColumn<Product, Void> colAcoes = new TableColumn<>("Ações");
        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnEditar = new Button("Editar");
            private final Button btnAtivarDesativar = new Button("Desativar");
            private final HBox pane = new HBox(5, btnEditar, btnAtivarDesativar);

            {
                btnEditar.getStyleClass().add("button-primary");
                btnAtivarDesativar.getStyleClass().add("button-secondary");
                btnEditar.setStyle("-fx-font-size: 11; -fx-padding: 4 10 4 10;");
                btnAtivarDesativar.setStyle("-fx-font-size: 11; -fx-padding: 4 10 4 10;");

                btnEditar.setOnAction(e -> {
                    Product produto = getTableView().getItems().get(getIndex());
                    if (onEditar != null) {
                        onEditar.accept(produto);
                    }
                });
                btnAtivarDesativar.setOnAction(e -> {
                    Product produto = getTableView().getItems().get(getIndex());
                    if (onAtivarDesativar != null) {
                        onAtivarDesativar.accept(produto);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Product produto = getTableView().getItems().get(getIndex());
                    btnAtivarDesativar.setText(produto.isAtivo() ? "Desativar" : "Ativar");
                    setGraphic(pane);
                }
            }
        });
        colAcoes.setMinWidth(170);

        tabela.getColumns().addAll(colNome, colSku, colCodigoBarras, colCategoria, colPreco, colStatus, colAcoes);
    }

    private void carregarFiltroCategorias() {
        Category selecionada = cmbFiltroCategoria.getValue();
        Long selecionadaId = selecionada != null ? selecionada.getId() : null;

        TarefaAssincrona.executar(
                categoryService::listarTodos,
                lista -> {
                    Category todas = new Category();
                    todas.setId(null);
                    todas.setNome("TODAS");
                    ObservableList<Category> categorias = FXCollections.observableArrayList();
                    categorias.add(todas);
                    if (lista != null) {
                        categorias.addAll(lista);
                    }
                    cmbFiltroCategoria.setItems(categorias);
                    if (selecionadaId != null) {
                        categorias.stream()
                                .filter(c -> selecionadaId.equals(c.getId()))
                                .findFirst()
                                .ifPresentOrElse(
                                        cmbFiltroCategoria.getSelectionModel()::select,
                                        () -> cmbFiltroCategoria.getSelectionModel().selectFirst());
                    } else {
                        cmbFiltroCategoria.getSelectionModel().selectFirst();
                    }
                },
                e -> logErro("carregar categorias do filtro", e));
    }

    private void filtrarTabela() {
        String termo = txtPesquisa.getText();
        String status = cmbFiltroStatus.getValue();
        Category categoria = cmbFiltroCategoria.getValue();
        Long categoriaId = (categoria != null && categoria.getId() != null) ? categoria.getId() : null;

        TarefaAssincrona.executar(
                () -> productService.pesquisar(termo, status, categoriaId),
                dados::setAll,
                e -> logErro("filtrar produtos", e));
    }

    private void logErro(String operacao, Throwable e) {
        log.error("Erro ao {}: {}", operacao, e.getMessage(), e);
    }

    public void atualizarLista() {
        carregarFiltroCategorias();
        filtrarTabela();
    }

    public void setOnEditar(Consumer<Product> onEditar) {
        this.onEditar = onEditar;
    }

    public void setOnAtivarDesativar(Consumer<Product> onAtivarDesativar) {
        this.onAtivarDesativar = onAtivarDesativar;
    }
}
