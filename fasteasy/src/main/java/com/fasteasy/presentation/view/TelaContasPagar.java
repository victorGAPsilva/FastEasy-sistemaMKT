package com.fasteasy.presentation.view;

import com.fasteasy.domain.PayableAccountStatus;
import com.fasteasy.presentation.util.TarefaAssincrona;
import com.fasteasy.service.ExpenseCategoryService;
import com.fasteasy.service.PayableAccountService;
import com.fasteasy.service.SupplierService;
import com.fasteasy.service.dto.PayableAccountDetalhe;
import com.fasteasy.service.dto.PayableAccountResumo;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Tela de gestão de contas a pagar.
 *
 * <p>Permite cadastrar, consultar, pagar, parcelar e cancelar obrigações financeiras.</p>
 */
@Component
public class TelaContasPagar extends VBox {

    private static final Logger log = LoggerFactory.getLogger(TelaContasPagar.class);
    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PayableAccountService payableAccountService;
    private final SupplierService supplierService;
    private final ExpenseCategoryService expenseCategoryService;

    private final ComboBox<String> filtroStatus = new ComboBox<>();
    private final ComboBox<String> filtroPeriodo = new ComboBox<>();
    private final DatePicker dtInicio = new DatePicker();
    private final DatePicker dtFim = new DatePicker();
    private final Button btnBuscar = new Button("Buscar");
    private final Button btnLimpar = new Button("Limpar");
    private final Button btnNovaConta = new Button("Nova Conta");
    private final Button btnDespesa = new Button("Nova Despesa");

    private final ObservableList<PayableAccountResumo> dados = FXCollections.observableArrayList();
    private final TableView<PayableAccountResumo> tabela = new TableView<>(dados);

    private final Label lblResumo = new Label();
    private final Label lblMensagem = new Label("Pronto.");

    public TelaContasPagar(PayableAccountService payableAccountService,
                           SupplierService supplierService,
                           ExpenseCategoryService expenseCategoryService) {
        this.payableAccountService = payableAccountService;
        this.supplierService = supplierService;
        this.expenseCategoryService = expenseCategoryService;

        setSpacing(12);
        setPadding(new Insets(16));

        configurarFiltros();
        configurarTabela();
        getChildren().addAll(cabecalho(), filtrosBox(), tabela, lblResumo, lblMensagem);
        VBox.setVgrow(tabela, Priority.ALWAYS);
    }

    private void configurarFiltros() {
        filtroStatus.getItems().addAll("Todos", "Aberta", "Parcialmente paga", "Paga", "Cancelada", "Vencidas", "Próx. 7 dias", "Próx. 30 dias");
        filtroStatus.setValue("Todos");
        filtroStatus.setPrefWidth(160);

        filtroPeriodo.getItems().addAll("Todos", "Últimos 7 dias", "Últimos 30 dias", "Mês atual", "Personalizado");
        filtroPeriodo.setValue("Últimos 30 dias");
        filtroPeriodo.setPrefWidth(140);
        filtroPeriodo.valueProperty().addListener((obs, a, b) -> atualizarVisibilidadeDatas());

        dtInicio.setPrefWidth(130);
        dtFim.setPrefWidth(130);
        dtInicio.setVisible(false);
        dtFim.setVisible(false);

        btnBuscar.getStyleClass().add("button-primary");
        btnBuscar.setOnAction(e -> atualizarLista());

        btnLimpar.getStyleClass().add("button-secondary");
        btnLimpar.setOnAction(e -> {
            filtroStatus.setValue("Todos");
            filtroPeriodo.setValue("Últimos 30 dias");
            dtInicio.setValue(null);
            dtFim.setValue(null);
            atualizarLista();
        });

        btnNovaConta.getStyleClass().add("button-primary");
        btnNovaConta.setOnAction(e -> novaConta());

        btnDespesa.getStyleClass().add("button-secondary");
        btnDespesa.setOnAction(e -> novaDespesa());
    }

    private void atualizarVisibilidadeDatas() {
        boolean visivel = "Personalizado".equals(filtroPeriodo.getValue());
        dtInicio.setVisible(visivel);
        dtFim.setVisible(visivel);
    }

    private HBox cabecalho() {
        Label titulo = new Label("Contas a Pagar");
        titulo.getStyleClass().add("page-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox box = new HBox(12, titulo, spacer, btnNovaConta, btnDespesa);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(10, 14, 10, 14));
        return box;
    }

    private HBox filtrosBox() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(12));

        box.getChildren().addAll(
                new Label("Status:"), filtroStatus,
                new Label("Período:"), filtroPeriodo,
                new Label("Início:"), dtInicio,
                new Label("Fim:"), dtFim,
                btnBuscar, btnLimpar
        );
        return box;
    }

    private void configurarTabela() {
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabela.setPlaceholder(new Label("Nenhuma conta encontrada"));

        TableColumn<PayableAccountResumo, String> colId = new TableColumn<>("#");
        colId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().id().toString()));
        colId.setMinWidth(60);

        TableColumn<PayableAccountResumo, String> colDescricao = new TableColumn<>("Descrição");
        colDescricao.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().descricao()));
        colDescricao.setMinWidth(200);

        TableColumn<PayableAccountResumo, String> colFornecedor = new TableColumn<>("Fornecedor");
        colFornecedor.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().fornecedorNome() != null ? c.getValue().fornecedorNome() : "—"));
        colFornecedor.setMinWidth(150);

        TableColumn<PayableAccountResumo, String> colCategoria = new TableColumn<>("Categoria");
        colCategoria.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().categoriaNome() != null ? c.getValue().categoriaNome() : "—"));
        colCategoria.setMinWidth(130);

        TableColumn<PayableAccountResumo, String> colEmissao = new TableColumn<>("Emissão");
        colEmissao.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().dataEmissao() != null ? c.getValue().dataEmissao().format(FMT_DATA) : "—"));
        colEmissao.setMinWidth(100);

        TableColumn<PayableAccountResumo, String> colVencimento = new TableColumn<>("Próx. Venc.");
        colVencimento.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().proximoVencimento() != null ? c.getValue().proximoVencimento().format(FMT_DATA) : "—"));
        colVencimento.setMinWidth(100);

        TableColumn<PayableAccountResumo, String> colOriginal = new TableColumn<>("Valor Original");
        colOriginal.setCellValueFactory(c -> new SimpleStringProperty(
                com.fasteasy.domain.Dinheiro.formatar(c.getValue().valorOriginal())));
        colOriginal.setMinWidth(120);

        TableColumn<PayableAccountResumo, String> colPago = new TableColumn<>("Valor Pago");
        colPago.setCellValueFactory(c -> new SimpleStringProperty(
                com.fasteasy.domain.Dinheiro.formatar(c.getValue().valorPago())));
        colPago.setMinWidth(110);

        TableColumn<PayableAccountResumo, String> colSaldo = new TableColumn<>("Saldo Pendente");
        colSaldo.setCellValueFactory(c -> new SimpleStringProperty(
                com.fasteasy.domain.Dinheiro.formatar(c.getValue().saldoPendente())));
        colSaldo.setMinWidth(120);

        TableColumn<PayableAccountResumo, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status().getDescricao()));
        colStatus.setMinWidth(130);
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    PayableAccountResumo conta = getTableView().getItems().get(getIndex());
                    if (conta.isVencida()) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else if (conta.isProximaVencimento(7)) {
                        setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    } else if (conta.status() == PayableAccountStatus.PAID) {
                        setStyle("-fx-text-fill: #27ae60;");
                    } else if (conta.status() == PayableAccountStatus.CANCELLED) {
                        setStyle("-fx-text-fill: #95a5a6;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        TableColumn<PayableAccountResumo, String> colParcelas = new TableColumn<>("Parcelas");
        colParcelas.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().parcelasPagas() + "/" + c.getValue().quantidadeParcelas()));
        colParcelas.setMinWidth(90);

        tabela.getColumns().addAll(colId, colDescricao, colFornecedor, colCategoria, colEmissao,
                colVencimento, colOriginal, colPago, colSaldo, colStatus, colParcelas);

        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            if (novo != null) {
                verDetalhes(novo);
            }
        });

        tabela.setRowFactory(tv -> new TableRow<PayableAccountResumo>() {
            @Override
            protected void updateItem(PayableAccountResumo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (item.isVencida()) {
                    setStyle("-fx-background-color: #fdf2f2;");
                } else if (item.isProximaVencimento(7)) {
                    setStyle("-fx-background-color: #fef9e7;");
                } else if (item.status() == PayableAccountStatus.PAID) {
                    setStyle("-fx-background-color: #e8f8f5;");
                } else if (item.status() == PayableAccountStatus.CANCELLED) {
                    setStyle("-fx-background-color: #f2f3f4;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    public void aoExibir() {
        atualizarLista();
    }

    public void atualizarLista() {
        PayableAccountStatus status = null;
        LocalDate inicio = null;
        LocalDate fim = null;

        String statusStr = filtroStatus.getValue();
        if ("Aberta".equals(statusStr)) status = PayableAccountStatus.OPEN;
        else if ("Parcialmente paga".equals(statusStr)) status = PayableAccountStatus.PARTIALLY_PAID;
        else if ("Paga".equals(statusStr)) status = PayableAccountStatus.PAID;
        else if ("Cancelada".equals(statusStr)) status = PayableAccountStatus.CANCELLED;

        String periodoStr = filtroPeriodo.getValue();
        LocalDate hoje = LocalDate.now();
        if ("Últimos 7 dias".equals(periodoStr)) {
            inicio = hoje.minusDays(7);
            fim = hoje;
        } else if ("Últimos 30 dias".equals(periodoStr)) {
            inicio = hoje.minusDays(30);
            fim = hoje;
        } else if ("Mês atual".equals(periodoStr)) {
            inicio = hoje.withDayOfMonth(1);
            fim = hoje;
        } else if ("Personalizado".equals(periodoStr)) {
            inicio = dtInicio.getValue();
            fim = dtFim.getValue();
        }

        final String statusFinal = statusStr;
        final PayableAccountStatus statusFinalEnum = status;
        final LocalDate inicioFinal = inicio;
        final LocalDate fimFinal = fim;

        TarefaAssincrona.executar(
                () -> {
                    if ("Vencidas".equals(statusFinal)) {
                        return payableAccountService.buscarVencidas();
                    } else if ("Próx. 7 dias".equals(statusFinal)) {
                        return payableAccountService.buscarProximasVencimento(7);
                    } else if ("Próx. 30 dias".equals(statusFinal)) {
                        return payableAccountService.buscarProximasVencimento(30);
                    } else {
                        return payableAccountService.pesquisar(null, null, statusFinalEnum, inicioFinal, fimFinal);
                    }
                },
                lista -> {
                    dados.setAll(lista);
                    atualizarResumo(lista);
                    lblMensagem.setText(lista.size() + " conta(s) encontrada(s).");
                },
                e -> lblMensagem.setText("Erro ao carregar contas: " + e.getMessage())
        );
    }

    private void atualizarResumo(List<PayableAccountResumo> lista) {
        BigDecimal totalOriginal = BigDecimal.ZERO;
        BigDecimal totalPago = BigDecimal.ZERO;
        BigDecimal totalPendente = BigDecimal.ZERO;
        long vencidas = 0;
        long aVencer7 = 0;

        for (PayableAccountResumo c : lista) {
            totalOriginal = totalOriginal.add(com.fasteasy.domain.Dinheiro.ouZero(c.valorOriginal()));
            totalPago = totalPago.add(com.fasteasy.domain.Dinheiro.ouZero(c.valorPago()));
            totalPendente = totalPendente.add(com.fasteasy.domain.Dinheiro.ouZero(c.saldoPendente()));
            if (c.isVencida()) vencidas++;
            if (c.isProximaVencimento(7)) aVencer7++;
        }

        lblResumo.setText(String.format(
                "Total original: %s  |  Total pago: %s  |  Saldo pendente: %s  |  Vencidas: %d  |  Próx. 7 dias: %d",
                com.fasteasy.domain.Dinheiro.formatar(totalOriginal),
                com.fasteasy.domain.Dinheiro.formatar(totalPago),
                com.fasteasy.domain.Dinheiro.formatar(totalPendente),
                vencidas, aVencer7
        ));
    }

    private void novaConta() {
        FormContaPagarDialog.mostrar(janela(), supplierService, expenseCategoryService, payableAccountService, null)
                .ifPresent(conta -> {
                    lblMensagem.setText("Conta criada: " + conta.getDescricao());
                    atualizarLista();
                });
    }

    private void novaDespesa() {
        FormContaPagarDialog.mostrar(janela(), supplierService, expenseCategoryService, payableAccountService, null)
                .ifPresent(conta -> {
                    lblMensagem.setText("Despesa criada: " + conta.getDescricao());
                    atualizarLista();
                });
    }

    private void verDetalhes(PayableAccountResumo resumo) {
        TarefaAssincrona.executar(
                () -> payableAccountService.detalhe(resumo.id()),
                detalhe -> DetalheContaDialog.mostrar(janela(), detalhe, payableAccountService),
                e -> lblMensagem.setText("Erro ao carregar detalhes: " + e.getMessage())
        );
    }

    private Window janela() {
        return getScene() == null ? null : getScene().getWindow();
    }
}