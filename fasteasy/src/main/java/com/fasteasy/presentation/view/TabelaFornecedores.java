package com.fasteasy.presentation.view;

import com.fasteasy.domain.Supplier;
import com.fasteasy.presentation.util.TarefaAssincrona;
import com.fasteasy.service.SupplierService;
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
 * Tabela de listagem de fornecedores com pesquisa e ações.
 */
@Component
public class TabelaFornecedores extends VBox {

    private static final Logger log = LoggerFactory.getLogger(TabelaFornecedores.class);

    private final SupplierService supplierService;

    private final TableView<Supplier> tabela = new TableView<>();
    private final TextField txtPesquisa = new TextField();
    private final ComboBox<String> cmbFiltroStatus = new ComboBox<>();

    private final ObservableList<Supplier> dados = FXCollections.observableArrayList();
    private Consumer<Supplier> onEditar;
    private Consumer<Supplier> onAtivarDesativar;

    @Autowired
    public TabelaFornecedores(SupplierService supplierService) {
        this.supplierService = supplierService;
        inicializarComponentes();
        configurarColunas();
    }

    private void inicializarComponentes() {
        setSpacing(10);
        setPadding(new Insets(10, 0, 0, 0));

        Label titulo = new Label("Lista de Fornecedores");
        titulo.getStyleClass().add("section-title");

        HBox filtros = new HBox(10);
        filtros.getStyleClass().add("card");
        filtros.setPadding(new Insets(10));

        txtPesquisa.setPromptText("Pesquisar por nome, fantasia ou documento");
        txtPesquisa.setPrefWidth(300);
        txtPesquisa.textProperty().addListener((obs, old, novo) -> filtrarTabela());

        cmbFiltroStatus.setPromptText("Status");
        cmbFiltroStatus.getItems().addAll("TODOS", "ATIVO", "INATIVO");
        cmbFiltroStatus.getSelectionModel().selectFirst();
        cmbFiltroStatus.setOnAction(e -> filtrarTabela());

        filtros.getChildren().addAll(
                new Label("Pesquisa:"), txtPesquisa,
                new Label("Status:"), cmbFiltroStatus
        );

        tabela.setItems(dados);
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabela.setPrefHeight(400);

        getChildren().addAll(titulo, filtros, tabela);
    }

    private void configurarColunas() {
        TableColumn<Supplier, String> colNome = new TableColumn<>("Razão Social");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nomeRazaoSocial"));
        colNome.setMinWidth(220);

        TableColumn<Supplier, String> colFantasia = new TableColumn<>("Nome Fantasia");
        colFantasia.setCellValueFactory(new PropertyValueFactory<>("nomeFantasia"));
        colFantasia.setMinWidth(180);

        TableColumn<Supplier, String> colDocumento = new TableColumn<>("CPF/CNPJ");
        colDocumento.setCellValueFactory(new PropertyValueFactory<>("cpfCnpj"));
        colDocumento.setMinWidth(140);

        TableColumn<Supplier, String> colTelefone = new TableColumn<>("Telefone");
        colTelefone.setCellValueFactory(new PropertyValueFactory<>("telefone"));
        colTelefone.setMinWidth(130);

        TableColumn<Supplier, String> colStatus = new TableColumn<>("Status");
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

        TableColumn<Supplier, Void> colAcoes = new TableColumn<>("Ações");
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
                    Supplier s = getTableView().getItems().get(getIndex());
                    if (onEditar != null) onEditar.accept(s);
                });
                btnAtivarDesativar.setOnAction(e -> {
                    Supplier s = getTableView().getItems().get(getIndex());
                    if (onAtivarDesativar != null) onAtivarDesativar.accept(s);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Supplier s = getTableView().getItems().get(getIndex());
                    btnAtivarDesativar.setText("ATIVO".equals(s.getStatus()) ? "Desativar" : "Ativar");
                    setGraphic(pane);
                }
            }
        });
        colAcoes.setMinWidth(170);

        tabela.getColumns().addAll(colNome, colFantasia, colDocumento, colTelefone, colStatus, colAcoes);
    }

    private void filtrarTabela() {
        String termo = txtPesquisa.getText();
        String status = cmbFiltroStatus.getValue();
        TarefaAssincrona.executar(
                () -> supplierService.pesquisarPorNome(termo).stream()
                        .filter(s -> "TODOS".equals(status) || status == null || status.equals(s.getStatus()))
                        .toList(),
                dados::setAll,
                e -> log.error("Erro ao filtrar fornecedores: {}", e.getMessage(), e));
    }

    public void atualizarLista() {
        filtrarTabela();
    }

    public void setOnEditar(Consumer<Supplier> onEditar) {
        this.onEditar = onEditar;
    }

    public void setOnAtivarDesativar(Consumer<Supplier> onAtivarDesativar) {
        this.onAtivarDesativar = onAtivarDesativar;
    }
}
