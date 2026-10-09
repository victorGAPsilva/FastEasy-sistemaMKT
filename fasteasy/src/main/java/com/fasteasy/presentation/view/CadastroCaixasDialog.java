package com.fasteasy.presentation.view;

import com.fasteasy.domain.CashRegister;
import com.fasteasy.presentation.util.TarefaAssincrona;
import com.fasteasy.service.CashRegisterService;
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
 * Diálogo de cadastro de caixas (pontos de atendimento).
 *
 * <p>Permite cadastrar, ativar e desativar caixas. Não executa nenhuma
 * operação financeira — apenas mantém o cadastro.</p>
 */
public final class CadastroCaixasDialog {

    private static final Logger log = LoggerFactory.getLogger(CadastroCaixasDialog.class);

    private CadastroCaixasDialog() {
    }

    public static void mostrar(Window owner, CashRegisterService service) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Cadastro de caixas");
        dialog.setHeaderText("Caixas do estabelecimento");
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Fechar", ButtonBar.ButtonData.CANCEL_CLOSE));

        ObservableList<CashRegister> dados = FXCollections.observableArrayList();
        TableView<CashRegister> tabela = new TableView<>(dados);
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabela.setPlaceholder(new Label("Nenhum caixa cadastrado"));
        tabela.setPrefHeight(240);

        TableColumn<CashRegister, String> colNome = new TableColumn<>("Nome");
        colNome.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNome()));
        colNome.setMinWidth(140);

        TableColumn<CashRegister, String> colDescricao = new TableColumn<>("Descrição");
        colDescricao.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDescricao() == null ? "" : c.getValue().getDescricao()));
        colDescricao.setMinWidth(200);

        TableColumn<CashRegister, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().isAtivo() ? "Ativo" : "Inativo"));
        colStatus.setMinWidth(90);
        tabela.getColumns().addAll(colNome, colDescricao, colStatus);

        TextField txtNome = new TextField();
        txtNome.setPromptText("Nome do caixa (ex.: Caixa 1)");
        txtNome.setPrefWidth(200);
        TextField txtDescricao = new TextField();
        txtDescricao.setPromptText("Descrição (opcional)");
        txtDescricao.setPrefWidth(220);

        Button btnAdicionar = new Button("Adicionar");
        btnAdicionar.getStyleClass().add("button-primary");
        Button btnAlternar = new Button("Ativar/Desativar");
        btnAlternar.getStyleClass().add("button-secondary");

        Label lblMensagem = new Label();

        btnAdicionar.setOnAction(e -> {
            String nome = txtNome.getText();
            if (nome == null || nome.isBlank()) {
                aviso(dialog, "Informe o nome do caixa.");
                return;
            }
            CashRegister novo = new CashRegister(nome.trim());
            novo.setDescricao(txtDescricao.getText() == null || txtDescricao.getText().isBlank()
                    ? null : txtDescricao.getText().trim());
            TarefaAssincrona.executar(
                    () -> service.cadastrar(novo),
                    salvo -> {
                        txtNome.clear();
                        txtDescricao.clear();
                        lblMensagem.setText("Caixa \"" + salvo.getNome() + "\" cadastrado.");
                        recarregar(service, dados, lblMensagem);
                    },
                    erro -> aviso(dialog, mensagemErro(erro)));
        });

        btnAlternar.setOnAction(e -> {
            CashRegister selecionado = tabela.getSelectionModel().getSelectedItem();
            if (selecionado == null) {
                aviso(dialog, "Selecione um caixa na lista.");
                return;
            }
            boolean desativar = selecionado.isAtivo();
            TarefaAssincrona.executar(
                    () -> {
                        if (desativar) {
                            service.desativar(selecionado.getId());
                        } else {
                            service.ativar(selecionado.getId());
                        }
                        return null;
                    },
                    ignorado -> recarregar(service, dados, lblMensagem),
                    erro -> aviso(dialog, mensagemErro(erro)));
        });

        HBox form = new HBox(10, new Label("Nome:"), txtNome, txtDescricao, btnAdicionar);
        form.setAlignment(Pos.CENTER_LEFT);

        HBox acoes = new HBox(10, btnAlternar, lblMensagem);
        acoes.setAlignment(Pos.CENTER_LEFT);

        VBox conteudo = new VBox(12, form, tabela, acoes);
        conteudo.setPadding(new Insets(12));
        conteudo.setPrefWidth(620);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        dialog.getDialogPane().setContent(conteudo);

        recarregar(service, dados, lblMensagem);
        dialog.showAndWait();
    }

    private static void recarregar(CashRegisterService service,
                                   ObservableList<CashRegister> dados,
                                   Label lblMensagem) {
        TarefaAssincrona.executar(
                service::listarTodos,
                lista -> {
                    dados.setAll(lista);
                    if (lblMensagem.getText() == null || lblMensagem.getText().isBlank()) {
                        lblMensagem.setText(lista.size() + " caixa(s) cadastrado(s).");
                    }
                },
                e -> log.error("Erro ao carregar caixas: {}", e.getMessage(), e));
    }

    private static String mensagemErro(Throwable erro) {
        if (erro instanceof IllegalArgumentException) {
            return erro.getMessage();
        }
        log.error("Erro no cadastro de caixas: {}", erro.getMessage(), erro);
        return "Ocorreu um erro inesperado. Consulte os logs.";
    }

    private static void aviso(Dialog<?> dialog, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Atenção");
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        if (dialog.getDialogPane().getScene() != null) {
            alert.initOwner(dialog.getDialogPane().getScene().getWindow());
        }
        alert.showAndWait();
    }
}
