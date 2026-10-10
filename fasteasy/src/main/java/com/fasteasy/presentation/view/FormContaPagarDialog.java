package com.fasteasy.presentation.view;

import com.fasteasy.domain.Dinheiro;
import com.fasteasy.domain.ExpenseCategory;
import com.fasteasy.domain.FinanceiroInvalidoException;
import com.fasteasy.domain.PayableAccount;
import com.fasteasy.domain.Supplier;
import com.fasteasy.presentation.util.TarefaAssincrona;
import com.fasteasy.service.ExpenseCategoryService;
import com.fasteasy.service.PayableAccountService;
import com.fasteasy.service.SupplierService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Diálogo para cadastro/edição de conta a pagar ou despesa.
 */
public final class FormContaPagarDialog {

    private static final Logger log = LoggerFactory.getLogger(FormContaPagarDialog.class);
    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private FormContaPagarDialog() {}

    public static Optional<PayableAccount> mostrar(Window owner,
                                                   SupplierService supplierService,
                                                   ExpenseCategoryService expenseCategoryService,
                                                   PayableAccountService payableAccountService,
                                                   PayableAccount contaEdicao) {
        Dialog<PayableAccount> dialog = new Dialog<>();
        dialog.setTitle(contaEdicao == null ? "Nova Conta a Pagar / Despesa" : "Editar Conta a Pagar");
        dialog.setHeaderText(contaEdicao == null ? "Preencha os dados da obrigação financeira" : "Edite os dados da conta");
        if (owner != null) dialog.initOwner(owner);

        ButtonType btnSalvar = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSalvar, btnCancelar);

        // Campos
        TextField txtDescricao = new TextField();
        txtDescricao.setPromptText("Descrição da obrigação (ex.: Compra de mercadorias, Aluguel)");
        txtDescricao.setPrefWidth(400);

        ComboBox<Supplier> cmbFornecedor = new ComboBox<>();
        cmbFornecedor.setPromptText("Fornecedor (opcional para despesas)");
        cmbFornecedor.setPrefWidth(300);
        cmbFornecedor.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Supplier s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? null : s.getNomeRazaoSocial());
            }
        });
        cmbFornecedor.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Supplier s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? null : s.getNomeRazaoSocial());
            }
        });

        ComboBox<ExpenseCategory> cmbCategoria = new ComboBox<>();
        cmbCategoria.setPromptText("Categoria *");
        cmbCategoria.setPrefWidth(250);
        cmbCategoria.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(ExpenseCategory c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? null : c.getNome());
            }
        });
        cmbCategoria.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(ExpenseCategory c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? null : c.getNome());
            }
        });

        DatePicker dpEmissao = new DatePicker(LocalDate.now());
        dpEmissao.setPrefWidth(140);

        DatePicker dpCompetencia = new DatePicker();
        dpCompetencia.setPrefWidth(140);

        TextField txtValor = new TextField();
        txtValor.setPromptText("0,00");
        txtValor.setPrefWidth(140);

        TextArea txtObs = new TextArea();
        txtObs.setPromptText("Observações (opcional)");
        txtObs.setPrefRowCount(3);
        txtObs.setPrefWidth(400);

        // Parcelas
        ObservableList<ParcelaRow> parcelasData = FXCollections.observableArrayList();
        TableView<ParcelaRow> tblParcelas = new TableView<>(parcelasData);
        tblParcelas.setPrefHeight(180);
        tblParcelas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<ParcelaRow, String> colNum = new TableColumn<>("Nº");
        colNum.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().numero)));
        colNum.setMinWidth(50);

        TableColumn<ParcelaRow, String> colVenc = new TableColumn<>("Vencimento");
        colVenc.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().vencimento != null ? c.getValue().vencimento.format(FMT_DATA) : ""));
        colVenc.setMinWidth(110);

        TableColumn<ParcelaRow, String> colValor = new TableColumn<>("Valor");
        colValor.setCellValueFactory(c -> new SimpleStringProperty(
                Dinheiro.formatar(c.getValue().valor)));
        colValor.setMinWidth(120);

        TableColumn<ParcelaRow, String> colAcoes = new TableColumn<>("");
        colAcoes.setCellFactory(col -> new TableCell<>() {
            private final Button btnDel = new Button("🗑");
            {
                btnDel.setOnAction(e -> {
                    ParcelaRow row = getTableView().getItems().get(getIndex());
                    getTableView().getItems().remove(row);
                    reordenarParcelas(parcelasData);
                });
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDel);
            }
        });
        colAcoes.setMinWidth(40);

        tblParcelas.getColumns().addAll(colNum, colVenc, colValor, colAcoes);

        Button btnAddParcela = new Button("Adicionar Parcela");
        btnAddParcela.getStyleClass().add("button-secondary");
        btnAddParcela.setOnAction(e -> adicionarParcela(parcelasData, tblParcelas));

        // Carregar dados
        TarefaAssincrona.executar(supplierService::listarAtivos,
                lista -> cmbFornecedor.setItems(FXCollections.observableArrayList(lista)),
                e -> log.warn("Erro ao carregar fornecedores: {}", e.getMessage()));

        TarefaAssincrona.executar(expenseCategoryService::listarAtivas,
                lista -> cmbCategoria.setItems(FXCollections.observableArrayList(lista)),
                e -> log.warn("Erro ao carregar categorias: {}", e.getMessage()));

        // Se edição, preencher campos
        if (contaEdicao != null) {
            txtDescricao.setText(contaEdicao.getDescricao());
            if (contaEdicao.getFornecedor() != null) {
                cmbFornecedor.getSelectionModel().select(contaEdicao.getFornecedor());
            }
            if (contaEdicao.getCategoria() != null) {
                cmbCategoria.getSelectionModel().select(contaEdicao.getCategoria());
            }
            if (contaEdicao.getDataEmissao() != null) dpEmissao.setValue(contaEdicao.getDataEmissao());
            if (contaEdicao.getDataCompetencia() != null) dpCompetencia.setValue(contaEdicao.getDataCompetencia());
            txtValor.setText(Dinheiro.formatar(contaEdicao.getValorOriginal()));
            txtObs.setText(contaEdicao.getObservacoes());

            // Parcelas existentes
            if (contaEdicao.getParcelas() != null) {
                for (com.fasteasy.domain.PayableInstallment p : contaEdicao.getParcelas()) {
                    ParcelaRow row = new ParcelaRow();
                    row.numero = p.getNumeroParcela();
                    row.vencimento = p.getDataVencimento();
                    row.valor = p.getValorOriginal();
                    parcelasData.add(row);
                }
            }
        } else {
            // Nova: adiciona uma parcela padrão
            ParcelaRow row = new ParcelaRow();
            row.numero = 1;
            row.vencimento = LocalDate.now().plusDays(30);
            row.valor = BigDecimal.ZERO;
            parcelasData.add(row);
        }

        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        int row = 0;
        grid.add(new Label("Descrição *:"), 0, row);
        grid.add(txtDescricao, 1, row++);
        grid.add(new Label("Fornecedor:"), 0, row);
        grid.add(cmbFornecedor, 1, row++);
        grid.add(new Label("Categoria *:"), 0, row);
        grid.add(cmbCategoria, 1, row++);
        grid.add(new Label("Data Emissão *:"), 0, row);
        grid.add(dpEmissao, 1, row++);
        grid.add(new Label("Data Competência:"), 0, row);
        grid.add(dpCompetencia, 1, row++);
        grid.add(new Label("Valor Total *:"), 0, row);
        grid.add(txtValor, 1, row++);
        grid.add(new Label("Observações:"), 0, row);
        grid.add(txtObs, 1, row++);

        VBox parcelasBox = new VBox(8, new Label("Parcelas (soma deve igualar o valor total):"), tblParcelas, btnAddParcela);
        grid.add(new Label("Parcelas *:"), 0, row);
        grid.add(parcelasBox, 1, row);

        Label lblValidacao = new Label();
        lblValidacao.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11;");

        VBox conteudo = new VBox(12, grid, lblValidacao);
        conteudo.setPadding(new Insets(12));
        conteudo.setPrefWidth(600);
        dialog.getDialogPane().setContent(conteudo);

        Node salvarBtn = dialog.getDialogPane().lookupButton(btnSalvar);
        salvarBtn.addEventFilter(javafx.event.ActionEvent.ACTION, evt -> {
            if (!validarCampos(txtDescricao, cmbCategoria, txtValor, parcelasData, lblValidacao)) {
                evt.consume();
                return;
            }
            try {
                PayableAccount conta = construirConta(contaEdicao, txtDescricao, cmbFornecedor, cmbCategoria,
                        dpEmissao, dpCompetencia, txtValor, txtObs, parcelasData);
                PayableAccount salva = payableAccountService.cadastrar(conta,
                        converterParcelas(parcelasData),
                        "Operador"); // TODO: obter operador real
                dialog.setResult(salva);
            } catch (FinanceiroInvalidoException ex) {
                lblValidacao.setText(ex.getMessage());
                evt.consume();
            } catch (Exception ex) {
                log.error("Erro ao salvar conta", ex);
                lblValidacao.setText("Erro inesperado: " + ex.getMessage());
                evt.consume();
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == btnSalvar) {
                // O resultado já foi setado no event filter
                return null;
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private static boolean validarCampos(TextField txtDescricao, ComboBox<ExpenseCategory> cmbCategoria,
                                         TextField txtValor, ObservableList<ParcelaRow> parcelas,
                                         Label lblValidacao) {
        if (txtDescricao.getText() == null || txtDescricao.getText().trim().isEmpty()) {
            lblValidacao.setText("Descrição é obrigatória");
            return false;
        }
        if (cmbCategoria.getValue() == null) {
            lblValidacao.setText("Categoria é obrigatória");
            return false;
        }
        BigDecimal valor = parseMoeda(txtValor.getText());
        if (valor == null || valor.signum() <= 0) {
            lblValidacao.setText("Valor total deve ser maior que zero");
            return false;
        }
        if (parcelas.isEmpty()) {
            lblValidacao.setText("Pelo menos uma parcela é obrigatória");
            return false;
        }
        BigDecimal soma = BigDecimal.ZERO;
        for (ParcelaRow p : parcelas) {
            if (p.valor == null || p.valor.signum() <= 0) {
                lblValidacao.setText("Todas as parcelas devem ter valor maior que zero");
                return false;
            }
            if (p.vencimento == null) {
                lblValidacao.setText("Todas as parcelas devem ter data de vencimento");
                return false;
            }
            soma = soma.add(p.valor);
        }
        // A validação exata da soma será feita no service
        lblValidacao.setText("");
        return true;
    }

    private static PayableAccount construirConta(PayableAccount edicao,
                                                 TextField txtDescricao, ComboBox<Supplier> cmbFornecedor,
                                                 ComboBox<ExpenseCategory> cmbCategoria,
                                                 DatePicker dpEmissao, DatePicker dpCompetencia,
                                                 TextField txtValor, TextArea txtObs,
                                                 ObservableList<ParcelaRow> parcelas) {
        PayableAccount conta = edicao != null ? edicao : new PayableAccount();
        conta.setDescricao(txtDescricao.getText().trim());
        conta.setFornecedor(cmbFornecedor.getValue());
        conta.setCategoria(cmbCategoria.getValue());
        conta.setDataEmissao(dpEmissao.getValue());
        conta.setDataCompetencia(dpCompetencia.getValue());
        conta.setValorOriginal(Dinheiro.arredondar(parseMoeda(txtValor.getText())));
        conta.setObservacoes(txtObs.getText() == null || txtObs.getText().trim().isEmpty() ? null : txtObs.getText().trim());
        return conta;
    }

    private static List<PayableAccountService.ParcelaInput> converterParcelas(ObservableList<ParcelaRow> parcelas) {
        List<PayableAccountService.ParcelaInput> result = new ArrayList<>();
        for (ParcelaRow p : parcelas) {
            PayableAccountService.ParcelaInput input = new PayableAccountService.ParcelaInput();
            input.setNumeroParcela(p.numero);
            input.setDataVencimento(p.vencimento);
            input.setValor(p.valor);
            result.add(input);
        }
        return result;
    }

    private static void adicionarParcela(ObservableList<ParcelaRow> data, TableView<ParcelaRow> tabela) {
        ParcelaRow row = new ParcelaRow();
        row.numero = data.size() + 1;
        row.vencimento = LocalDate.now().plusDays(30 * row.numero);
        row.valor = BigDecimal.ZERO;
        data.add(row);
        reordenarParcelas(data);
        tabela.scrollTo(row);
    }

    private static void reordenarParcelas(ObservableList<ParcelaRow> data) {
        for (int i = 0; i < data.size(); i++) {
            data.get(i).numero = i + 1;
        }
    }

    public static BigDecimal parseMoeda(String texto) {
        if (texto == null || texto.isBlank()) return null;
        try {
            String v = texto.trim().replace("R$", "").replaceAll("\\s", "");
            if (v.contains(",")) {
                v = v.replace(".", "").replace(",", ".");
            }
            return Dinheiro.arredondar(new BigDecimal(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static class ParcelaRow {
        int numero;
        LocalDate vencimento;
        BigDecimal valor;
    }
}