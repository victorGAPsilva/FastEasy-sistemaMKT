package com.fasteasy.presentation.view;

import com.fasteasy.domain.ExpenseCategory;
import com.fasteasy.presentation.util.TarefaAssincrona;
import com.fasteasy.service.ExpenseCategoryService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Diálogo de cadastro de categorias de despesa operacional.
 */
public final class CadastroCategoriaDespesaDialog {

    private static final Logger log = LoggerFactory.getLogger(CadastroCategoriaDespesaDialog.class);

    private CadastroCategoriaDespesaDialog() {}

    public static void mostrar(Window owner, ExpenseCategoryService service) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Categorias de Despesa");
        dialog.setHeaderText("Cadastro de categorias para despesas operacionais");
        if (owner != null) dialog.initOwner(owner);
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Fechar", ButtonBar.ButtonData.CANCEL_CLOSE));

        ObservableList<ExpenseCategory> dados = FXCollections.observableArrayList();
        TableView<ExpenseCategory> tabela = new TableView<>(dados);
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabela.setPlaceholder(new Label("Nenhuma categoria cadastrada"));
        tabela.setPrefHeight(240);

        TableColumn<ExpenseCategory, String> colNome = new TableColumn<>("Nome");
        colNome.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNome()));
        colNome.setMinWidth(180);

        TableColumn<ExpenseCategory, String> colDesc = new TableColumn<>("Descrição");
        colDesc.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDescricao() == null ? "" : c.getValue().getDescricao()));
        colDesc.setMinWidth(300);

        TableColumn<ExpenseCategory, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        colStatus.setMinWidth(90);

        tabela.getColumns().addAll(colNome, colDesc, colStatus);

        TextField txtNome = new TextField();
        txtNome.setPromptText("Nome da categoria (ex.: ALUGUEL, ENERGIA)");
        txtNome.setPrefWidth(200);

        TextField txtDesc = new TextField();
        txtDesc.setPromptText("Descrição (opcional)");
        txtDesc.setPrefWidth(250);

        Button btnAdicionar = new Button("Adicionar");
        btnAdicionar.getStyleClass().add("button-primary");

        Button btnAlternar = new Button("Ativar/Desativar");
        btnAlternar.getStyleClass().add("button-secondary");

        Label lblMsg = new Label();

        btnAdicionar.setOnAction(e -> {
            String nome = txtNome.getText();
            if (nome == null || nome.isBlank()) {
                aviso(dialog, "Informe o nome da categoria.");
                return;
            }
            ExpenseCategory nova = new ExpenseCategory(nome.trim());
            nova.setDescricao(txtDesc.getText() == null || txtDesc.getText().isBlank() ? null : txtDesc.getText().trim());

            TarefaAssincrona.executar(
                    () -> service.cadastrar(nova),
                    salvo -> {
                        txtNome.clear();
                        txtDesc.clear();
                        lblMsg.setText("Categoria \"" + salvo.getNome() + "\" cadastrada.");
                        recarregar(service, dados, lblMsg);
                    },
                    erro -> aviso(dialog, mensagemErro(erro))
            );
        });

        btnAlternar.setOnAction(e -> {
            ExpenseCategory sel = tabela.getSelectionModel().getSelectedItem();
            if (sel == null) {
                aviso(dialog, "Selecione uma categoria na lista.");
                return;
            }
            boolean desativar = sel.isAtivo();
            TarefaAssincrona.executar(
                    () -> {
                        if (desativar) service.desativar(sel.getId());
                        else service.ativar(sel.getId());
                        return null;
                    },
                    ignorado -> recarregar(service, dados, lblMsg),
                    erro -> aviso(dialog, mensagemErro(erro))
            );
        });

        HBox form = new HBox(10, new Label("Nome:"), txtNome, txtDesc, btnAdicionar);
        form.setAlignment(Pos.CENTER_LEFT);

        HBox acoes = new HBox(10, btnAlternar, lblMsg);
        acoes.setAlignment(Pos.CENTER_LEFT);

        VBox conteudo = new VBox(12, form, tabela, acoes);
        conteudo.setPadding(new Insets(12));
        conteudo.setPrefWidth(620);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        dialog.getDialogPane().setContent(conteudo);

        recarregar(service, dados, lblMsg);
        dialog.showAndWait();
    }

    private static void recarregar(ExpenseCategoryService service, ObservableList<ExpenseCategory> dados, Label lblMsg) {
        TarefaAssincrona.executar(
                service::listarTodos,
                lista -> {
                    dados.setAll(lista);
                    if (lblMsg.getText() == null || lblMsg.getText().isBlank()) {
                        lblMsg.setText(lista.size() + " categoria(s) cadastrada(s).");
                    }
                },
                e -> log.error("Erro ao carregar categorias: {}", e.getMessage(), e)
        );
    }

    private static String mensagemErro(Throwable erro) {
        if (erro instanceof IllegalArgumentException) return erro.getMessage();
        log.error("Erro no cadastro: {}", erro.getMessage(), erro);
        return "Ocorreu um erro inesperado. Consulte os logs.";
    }

    private static void aviso(Dialog<?> dialog, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Atenção");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        if (dialog.getDialogPane().getScene() != null) alert.initOwner(dialog.getDialogPane().getScene().getWindow());
        alert.showAndWait();
    }
}