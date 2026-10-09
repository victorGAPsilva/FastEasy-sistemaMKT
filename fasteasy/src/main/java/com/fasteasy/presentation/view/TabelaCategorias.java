package com.fasteasy.presentation.view;

import com.fasteasy.domain.Category;
import com.fasteasy.presentation.util.TarefaAssincrona;
import com.fasteasy.service.CategoryService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Tabela de listagem de categorias com pesquisa e ações.
 */
@Component
public class TabelaCategorias extends VBox {

    private static final Logger log = LoggerFactory.getLogger(TabelaCategorias.class);

    private final CategoryService categoryService;

    private final TableView<Category> tabela = new TableView<>();
    private final TextField txtPesquisa = new TextField();

    private final ObservableList<Category> dados = FXCollections.observableArrayList();
    private Consumer<Category> onEditar;
    private Consumer<Category> onAtivarDesativar;

    @Autowired
    public TabelaCategorias(CategoryService categoryService) {
        this.categoryService = categoryService;
        inicializarComponentes();
        configurarColunas();
    }

    private void inicializarComponentes() {
        setSpacing(10);
        setPadding(new Insets(10, 0, 0, 0));

        Label titulo = new Label("Lista de Categorias");
        titulo.getStyleClass().add("section-title");

        HBox filtros = new HBox(10);
        filtros.getStyleClass().add("card");
        filtros.setPadding(new Insets(10));

        txtPesquisa.setPromptText("Pesquisar por nome");
        txtPesquisa.setPrefWidth(300);
        txtPesquisa.textProperty().addListener((obs, old, novo) -> filtrarTabela());

        filtros.getChildren().addAll(new Label("Pesquisa:"), txtPesquisa);

        tabela.setItems(dados);
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabela.setPrefHeight(400);

        getChildren().addAll(titulo, filtros, tabela);
    }

    private void configurarColunas() {
        TableColumn<Category, String> colNome = new TableColumn<>("Nome");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colNome.setMinWidth(220);

        TableColumn<Category, String> colDescricao = new TableColumn<>("Descrição");
        colDescricao.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colDescricao.setMinWidth(400);

        TableColumn<Category, String> colStatus = new TableColumn<>("Status");
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
        colStatus.setMinWidth(100);

        TableColumn<Category, Void> colAcoes = new TableColumn<>("Ações");
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
                    Category c = getTableView().getItems().get(getIndex());
                    if (onEditar != null) onEditar.accept(c);
                });
                btnAtivarDesativar.setOnAction(e -> {
                    Category c = getTableView().getItems().get(getIndex());
                    if (onAtivarDesativar != null) onAtivarDesativar.accept(c);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Category c = getTableView().getItems().get(getIndex());
                    btnAtivarDesativar.setText("ATIVO".equals(c.getStatus()) ? "Desativar" : "Ativar");
                    setGraphic(pane);
                }
            }
        });
        colAcoes.setMinWidth(170);

        tabela.getColumns().addAll(colNome, colDescricao, colStatus, colAcoes);
    }

    private void filtrarTabela() {
        String termo = txtPesquisa.getText();
        TarefaAssincrona.executar(
                () -> categoryService.pesquisarPorNome(termo),
                dados::setAll,
                e -> log.error("Erro ao filtrar categorias: {}", e.getMessage(), e));
    }

    public void atualizarLista() {
        filtrarTabela();
    }

    public void setOnEditar(Consumer<Category> onEditar) {
        this.onEditar = onEditar;
    }

    public void setOnAtivarDesativar(Consumer<Category> onAtivarDesativar) {
        this.onAtivarDesativar = onAtivarDesativar;
    }
}
