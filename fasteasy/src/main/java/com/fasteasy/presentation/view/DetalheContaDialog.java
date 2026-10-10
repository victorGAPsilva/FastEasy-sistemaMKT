package com.fasteasy.presentation.view;

import com.fasteasy.domain.Dinheiro;
import com.fasteasy.domain.FinancialPaymentMethod;
import com.fasteasy.domain.FinanceiroInvalidoException;
import com.fasteasy.domain.PayableAccountStatus;
import com.fasteasy.domain.PayableInstallmentStatus;
import com.fasteasy.presentation.util.TarefaAssincrona;
import com.fasteasy.service.PayableAccountService;
import com.fasteasy.service.dto.FinancialPaymentResumo;
import com.fasteasy.service.dto.PayableAccountDetalhe;
import com.fasteasy.service.dto.PayableAccountResumo;
import com.fasteasy.service.dto.PayableInstallmentResumo;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Diálogo de detalhes de uma conta a pagar: parcelas, pagamentos, ações.
 */
public final class DetalheContaDialog {

    private static final Logger log = LoggerFactory.getLogger(DetalheContaDialog.class);
    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private DetalheContaDialog() {}

    public static void mostrar(Window owner, PayableAccountDetalhe detalhe, PayableAccountService service) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Detalhes da Conta - " + detalhe.resumo().descricao());
        if (owner != null) dialog.initOwner(owner);
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Fechar", ButtonBar.ButtonData.CANCEL_CLOSE));

        PayableAccountResumo resumo = detalhe.resumo();

        // Cabeçalho com resumo
        GridPane gridResumo = new GridPane();
        gridResumo.setHgap(20);
        gridResumo.setVgap(6);
        int r = 0;
        addInfo(gridResumo, 0, r, "Descrição:", resumo.descricao());
        addInfo(gridResumo, 1, r++, "Fornecedor:", resumo.fornecedorNome() != null ? resumo.fornecedorNome() : "—");
        addInfo(gridResumo, 0, r, "Categoria:", resumo.categoriaNome() != null ? resumo.categoriaNome() : "—");
        addInfo(gridResumo, 1, r++, "Emissão:", resumo.dataEmissao() != null ? resumo.dataEmissao().format(FMT_DATA) : "—");
        addInfo(gridResumo, 0, r, "Valor Original:", Dinheiro.formatar(resumo.valorOriginal()));
        addInfo(gridResumo, 1, r++, "Valor Pago:", Dinheiro.formatar(resumo.valorPago()));
        addInfo(gridResumo, 0, r, "Saldo Pendente:", Dinheiro.formatar(resumo.saldoPendente()));
        addInfo(gridResumo, 1, r++, "Status:", resumo.status().getDescricao());
        addInfo(gridResumo, 0, r, "Parcelas:", resumo.parcelasPagas() + "/" + resumo.quantidadeParcelas());
        addInfo(gridResumo, 1, r, "Próx. Venc.:", resumo.proximoVencimento() != null ? resumo.proximoVencimento().format(FMT_DATA) : "—");

        // Parcelas
        ObservableList<PayableInstallmentResumo> parcelasData = FXCollections.observableArrayList(detalhe.parcelas());
        TableView<PayableInstallmentResumo> tblParcelas = new TableView<>(parcelasData);
        tblParcelas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tblParcelas.setPrefHeight(160);

        TableColumn<PayableInstallmentResumo, String> colNum = new TableColumn<>("Nº");
        colNum.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().numeroParcela())));
        colNum.setMinWidth(50);

        TableColumn<PayableInstallmentResumo, String> colVenc = new TableColumn<>("Vencimento");
        colVenc.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().dataVencimento() != null ? c.getValue().dataVencimento().format(FMT_DATA) : ""));
        colVenc.setMinWidth(110);

        TableColumn<PayableInstallmentResumo, String> colOriginal = new TableColumn<>("Original");
        colOriginal.setCellValueFactory(c -> new SimpleStringProperty(Dinheiro.formatar(c.getValue().valorOriginal())));
        colOriginal.setMinWidth(100);

        TableColumn<PayableInstallmentResumo, String> colPago = new TableColumn<>("Pago");
        colPago.setCellValueFactory(c -> new SimpleStringProperty(Dinheiro.formatar(c.getValue().valorPago())));
        colPago.setMinWidth(100);

        TableColumn<PayableInstallmentResumo, String> colSaldo = new TableColumn<>("Saldo");
        colSaldo.setCellValueFactory(c -> new SimpleStringProperty(Dinheiro.formatar(c.getValue().saldoPendente())));
        colSaldo.setMinWidth(100);

        TableColumn<PayableInstallmentResumo, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status().getDescricao()));
        colStatus.setMinWidth(110);
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item);
                    PayableInstallmentResumo p = getTableView().getItems().get(getIndex());
                    if (p.isVencida()) setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    else if (p.status() == PayableInstallmentStatus.PAID) setStyle("-fx-text-fill: #27ae60;");
                    else if (p.status() == PayableInstallmentStatus.CANCELLED) setStyle("-fx-text-fill: #95a5a6;");
                    else setStyle("");
                }
            }
        });

        tblParcelas.getColumns().addAll(colNum, colVenc, colOriginal, colPago, colSaldo, colStatus);

        // Pagamentos
        ObservableList<FinancialPaymentResumo> pagamentosData = FXCollections.observableArrayList(detalhe.pagamentos());
        TableView<FinancialPaymentResumo> tblPagamentos = new TableView<>(pagamentosData);
        tblPagamentos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tblPagamentos.setPrefHeight(140);

        TableColumn<FinancialPaymentResumo, String> colPgData = new TableColumn<>("Data/Hora");
        colPgData.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().dataHora() != null ? c.getValue().dataHora().format(FMT_DATA_HORA) : ""));
        colPgData.setMinWidth(130);

        TableColumn<FinancialPaymentResumo, String> colPgValor = new TableColumn<>("Valor");
        colPgValor.setCellValueFactory(c -> new SimpleStringProperty(Dinheiro.formatar(c.getValue().valor())));
        colPgValor.setMinWidth(100);

        TableColumn<FinancialPaymentResumo, String> colPgMetodo = new TableColumn<>("Método");
        colPgMetodo.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().metodoPagamento() != null ? c.getValue().metodoPagamento().getDescricao() : ""));
        colPgMetodo.setMinWidth(100);

        TableColumn<FinancialPaymentResumo, String> colPgRef = new TableColumn<>("Referência");
        colPgRef.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().referencia() != null ? c.getValue().referencia() : ""));
        colPgRef.setMinWidth(120);

        TableColumn<FinancialPaymentResumo, String> colPgObs = new TableColumn<>("Obs.");
        colPgObs.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().observacoes() != null ? c.getValue().observacoes() : ""));
        colPgObs.setMinWidth(150);

        TableColumn<FinancialPaymentResumo, String> colPgOper = new TableColumn<>("Operador");
        colPgOper.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().operador() != null ? c.getValue().operador() : ""));
        colPgOper.setMinWidth(100);

        tblPagamentos.getColumns().addAll(colPgData, colPgValor, colPgMetodo, colPgRef, colPgObs, colPgOper);

        // Ações
        Button btnPagar = new Button("Registrar Pagamento");
        btnPagar.getStyleClass().add("button-primary");
        btnPagar.setOnAction(e -> registrarPagamento(dialog, detalhe, service));

        Button btnCancelar = new Button("Cancelar Conta");
        btnCancelar.getStyleClass().add("button-danger");
        btnCancelar.setOnAction(e -> cancelarConta(dialog, detalhe, service));
        btnCancelar.setDisable(resumo.status() == PayableAccountStatus.PAID || resumo.valorPago().signum() > 0);

        Button btnImprimir = new Button("Imprimir");
        btnImprimir.getStyleClass().add("button-secondary");
        btnImprimir.setOnAction(e -> imprimirRelatorio(dialog, detalhe));

        HBox acoes = new HBox(10, btnPagar, btnCancelar, btnImprimir);
        acoes.setAlignment(Pos.CENTER_LEFT);

        VBox conteudo = new VBox(14, new Label("Resumo"), gridResumo, new Separator(),
                new Label("Parcelas"), tblParcelas, new Separator(),
                new Label("Histórico de Pagamentos"), tblPagamentos, acoes);
        conteudo.setPadding(new Insets(12));
        conteudo.setPrefWidth(800);
        VBox.setVgrow(tblParcelas, Priority.ALWAYS);
        VBox.setVgrow(tblPagamentos, Priority.ALWAYS);

        ScrollPane scroll = new ScrollPane(conteudo);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(550);
        dialog.getDialogPane().setContent(scroll);

        dialog.showAndWait();
    }

    private static void registrarPagamento(Dialog<?> parent, PayableAccountDetalhe detalhe, PayableAccountService service) {
        PayableAccountResumo resumo = detalhe.resumo();

        // Encontra a primeira parcela em aberto (FIFO)
        Optional<PayableInstallmentResumo> parcelaAberta = detalhe.parcelas().stream()
                .filter(p -> p.status() == PayableInstallmentStatus.OPEN || p.status() == PayableInstallmentStatus.PARTIALLY_PAID)
                .findFirst();

        if (parcelaAberta.isEmpty()) {
            aviso(parent, "Não há parcelas em aberto para pagamento.");
            return;
        }

        Dialog<PaymentData> dialog = new Dialog<>();
        dialog.setTitle("Registrar Pagamento");
        dialog.setHeaderText("Conta: " + resumo.descricao() + " | Parcela " + parcelaAberta.get().numeroParcela() +
                " | Saldo: " + Dinheiro.formatar(parcelaAberta.get().saldoPendente()));
        if (parent.getDialogPane().getScene() != null) {
            dialog.initOwner(parent.getDialogPane().getScene().getWindow());
        }

        ButtonType btnConfirmar = new ButtonType("Confirmar", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnConfirmar, btnCancelar);

        ComboBox<FinancialPaymentMethod> cmbMetodo = new ComboBox<>();
        cmbMetodo.getItems().addAll(FinancialPaymentMethod.values());
        cmbMetodo.setValue(FinancialPaymentMethod.PIX);
        cmbMetodo.setPrefWidth(200);

        TextField txtValor = new TextField();
        txtValor.setPromptText("0,00");
        txtValor.setPrefWidth(140);
        txtValor.setText(Dinheiro.formatar(parcelaAberta.get().saldoPendente()));

        DatePicker dpData = new DatePicker(LocalDate.now());
        dpData.setPrefWidth(140);

        TextField txtRef = new TextField();
        txtRef.setPromptText("Nº boleto, comprovante, etc. (opcional)");
        txtRef.setPrefWidth(300);

        TextArea txtObs = new TextArea();
        txtObs.setPromptText("Observações (opcional)");
        txtObs.setPrefRowCount(3);
        txtObs.setPrefWidth(400);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        int row = 0;
        grid.add(new Label("Método *:"), 0, row);
        grid.add(cmbMetodo, 1, row++);
        grid.add(new Label("Valor *:"), 0, row);
        grid.add(txtValor, 1, row++);
        grid.add(new Label("Data *:"), 0, row);
        grid.add(dpData, 1, row++);
        grid.add(new Label("Referência:"), 0, row);
        grid.add(txtRef, 1, row++);
        grid.add(new Label("Observações:"), 0, row);
        grid.add(txtObs, 1, row++);

        Label lblValidacao = new Label();
        lblValidacao.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11;");

        VBox box = new VBox(10, grid, lblValidacao);
        box.setPadding(new Insets(12));
        box.setPrefWidth(450);
        dialog.getDialogPane().setContent(box);

        Node btnOk = dialog.getDialogPane().lookupButton(btnConfirmar);
        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, evt -> {
            BigDecimal valor = FormContaPagarDialog.parseMoeda(txtValor.getText());
            if (valor == null || valor.signum() <= 0) {
                lblValidacao.setText("Valor deve ser maior que zero");
                evt.consume();
                return;
            }
            if (valor.compareTo(parcelaAberta.get().saldoPendente()) > 0) {
                lblValidacao.setText("Valor excede o saldo da parcela (" + Dinheiro.formatar(parcelaAberta.get().saldoPendente()) + ")");
                evt.consume();
                return;
            }
            if (cmbMetodo.getValue() == null) {
                lblValidacao.setText("Selecione o método de pagamento");
                evt.consume();
                return;
            }
            if (dpData.getValue() == null) {
                lblValidacao.setText("Data é obrigatória");
                evt.consume();
                return;
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn != btnConfirmar) return null;
            BigDecimal valor = FormContaPagarDialog.parseMoeda(txtValor.getText());
            if (valor == null || valor.signum() <= 0) {
                lblValidacao.setText("Valor deve ser maior que zero");
                return null;
            }
            if (valor.compareTo(parcelaAberta.get().saldoPendente()) > 0) {
                lblValidacao.setText("Valor excede o saldo da parcela (" + Dinheiro.formatar(parcelaAberta.get().saldoPendente()) + ")");
                return null;
            }
            if (cmbMetodo.getValue() == null) {
                lblValidacao.setText("Selecione o método de pagamento");
                return null;
            }
            if (dpData.getValue() == null) {
                lblValidacao.setText("Data é obrigatória");
                return null;
            }
            // Return all payment data as a map-like object
            return new PaymentData(valor, cmbMetodo.getValue(), dpData.getValue().atStartOfDay(),
                    txtRef.getText(), txtObs.getText());
        });

        Optional<PaymentData> result = dialog.showAndWait();
        if (result.isPresent()) {
            PaymentData pd = result.get();
            try {
                service.registrarPagamento(
                        detalhe.resumo().id(),
                        parcelaAberta.get().id(),
                        pd.valor(),
                        pd.metodo(),
                        pd.referencia(),
                        pd.observacoes(),
                        "Operador" // TODO: operador real
                );
                // Fecha o diálogo de detalhes e reabre com dados atualizados
                parent.close();
                TarefaAssincrona.executar(
                        () -> service.detalhe(detalhe.resumo().id()),
                        atualizado -> DetalheContaDialog.mostrar(owner(parent), atualizado, service),
                        e -> log.error("Erro ao recarregar detalhes", e)
                );
            } catch (FinanceiroInvalidoException ex) {
                aviso(parent, ex.getMessage());
            } catch (Exception ex) {
                log.error("Erro ao registrar pagamento", ex);
                aviso(parent, "Erro inesperado: " + ex.getMessage());
            }
        }
    }

    private static void cancelarConta(Dialog<?> parent, PayableAccountDetalhe detalhe, PayableAccountService service) {
        if (!confirmar(parent, "Cancelar Conta",
                "Tem certeza que deseja cancelar a conta \"" + detalhe.resumo().descricao() + "\"?\n" +
                        "Esta ação não pode ser desfeita.")) {
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Cancelar Conta");
        dialog.setHeaderText("Informe o motivo do cancelamento:");
        dialog.setContentText("Motivo:");
        if (owner(parent) != null) dialog.initOwner(owner(parent));

        Optional<String> motivo = dialog.showAndWait();
        motivo.ifPresent(m -> {
            if (m == null || m.trim().isEmpty()) {
                aviso(parent, "Motivo é obrigatório para cancelamento.");
                return;
            }
            try {
                service.cancelar(detalhe.resumo().id(), m.trim(), "Operador");
                parent.close();
            } catch (FinanceiroInvalidoException ex) {
                aviso(parent, ex.getMessage());
            } catch (Exception ex) {
                log.error("Erro ao cancelar conta", ex);
                aviso(parent, "Erro inesperado: " + ex.getMessage());
            }
        });
    }

    private static void imprimirRelatorio(Dialog<?> parent, PayableAccountDetalhe detalhe) {
        // TODO: implementar impressão do relatório da conta
        aviso(parent, "Impressão de relatório individual será implementada na próxima versão.");
    }

    private static void addInfo(GridPane grid, int col, int row, String titulo, String valor) {
        Label t = new Label(titulo);
        t.setStyle("-fx-text-fill: #546e7a;");
        Label v = new Label(valor);
        HBox h = new HBox(6, t, v);
        grid.add(h, col, row);
    }

    private static void aviso(Dialog<?> parent, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Atenção");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        if (owner(parent) != null) alert.initOwner(owner(parent));
        alert.showAndWait();
    }

    private static boolean confirmar(Dialog<?> parent, String titulo, String msg) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        if (owner(parent) != null) alert.initOwner(owner(parent));
        return alert.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    private static Window owner(Dialog<?> dialog) {
        return dialog.getDialogPane().getScene() == null ? null : dialog.getDialogPane().getScene().getWindow();
    }

    private record PaymentData(BigDecimal valor, FinancialPaymentMethod metodo, LocalDateTime data,
                               String referencia, String observacoes) {
    }
}