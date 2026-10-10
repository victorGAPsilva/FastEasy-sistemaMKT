package com.fasteasy.presentation.view;

import com.fasteasy.domain.Dinheiro;
import com.fasteasy.presentation.util.TarefaAssincrona;
import com.fasteasy.service.FinancialService;
import com.fasteasy.service.PayableAccountService;
import com.fasteasy.service.dto.FluxoCaixaDiarioDTO;
import com.fasteasy.service.dto.FluxoCaixaPrevistoDTO;
import com.fasteasy.service.dto.ResumoFinanceiroDTO;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Tela de Fluxo de Caixa Financeiro (previsto e realizado).
 */
@Component
public class TelaFluxoCaixa extends VBox {

    private static final Logger log = LoggerFactory.getLogger(TelaFluxoCaixa.class);
    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final FinancialService financialService;
    private final PayableAccountService payableAccountService;

    private final ComboBox<String> periodoRealizado = new ComboBox<>();
    private final DatePicker dtInicio = new DatePicker();
    private final DatePicker dtFim = new DatePicker();
    private final Button btnAtualizar = new Button("Atualizar");

    private final ObservableList<FluxoCaixaDiarioDTO> dadosRealizado = FXCollections.observableArrayList();
    private final TableView<FluxoCaixaDiarioDTO> tblRealizado = new TableView<>(dadosRealizado);

    private final ObservableList<FluxoCaixaPrevistoDTO.ObrigacaoPrevistaDTO> dadosPrevisto = FXCollections.observableArrayList();
    private final TableView<FluxoCaixaPrevistoDTO.ObrigacaoPrevistaDTO> tblPrevisto = new TableView<>(dadosPrevisto);

    private final Label lblResumo = new Label();
    private final Label lblMensagem = new Label("Selecione o período e clique em Atualizar.");

    public TelaFluxoCaixa(FinancialService financialService, PayableAccountService payableAccountService) {
        this.financialService = financialService;
        this.payableAccountService = payableAccountService;

        setSpacing(12);
        setPadding(new Insets(16));

        configurarControles();
        configurarTabelas();

        getChildren().addAll(
                cabecalho(),
                filtrosBox(),
                new Label("Resumo Financeiro"),
                lblResumo,
                new Separator(),
                new Label("Fluxo Realizado (Saídas Registradas)"),
                tblRealizado,
                new Separator(),
                new Label("Fluxo Previsto (Obrigações a Vencer)"),
                tblPrevisto,
                lblMensagem
        );
        VBox.setVgrow(tblRealizado, Priority.ALWAYS);
        VBox.setVgrow(tblPrevisto, Priority.ALWAYS);
    }

    private void configurarControles() {
        periodoRealizado.getItems().addAll("Hoje", "Últimos 7 dias", "Últimos 30 dias", "Mês atual", "Personalizado");
        periodoRealizado.setValue("Últimos 30 dias");
        periodoRealizado.setPrefWidth(150);
        periodoRealizado.valueProperty().addListener((obs, a, b) -> atualizarVisibilidadeDatas());

        dtInicio.setPrefWidth(130);
        dtFim.setPrefWidth(130);
        dtInicio.setVisible(false);
        dtFim.setVisible(false);

        btnAtualizar.getStyleClass().add("button-primary");
        btnAtualizar.setOnAction(e -> atualizar());
    }

    private void atualizarVisibilidadeDatas() {
        boolean vis = "Personalizado".equals(periodoRealizado.getValue());
        dtInicio.setVisible(vis);
        dtFim.setVisible(vis);
    }

    private HBox cabecalho() {
        Label titulo = new Label("Fluxo de Caixa Financeiro");
        titulo.getStyleClass().add("page-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox box = new HBox(12, titulo, spacer);
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
                new Label("Período Realizado:"), periodoRealizado,
                new Label("Início:"), dtInicio,
                new Label("Fim:"), dtFim,
                btnAtualizar
        );
        return box;
    }

    private void configurarTabelas() {
        // Realizado
        tblRealizado.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tblRealizado.setPlaceholder(new Label("Nenhum pagamento no período"));
        tblRealizado.setPrefHeight(220);

        TableColumn<FluxoCaixaDiarioDTO, String> colDataR = new TableColumn<>("Data");
        colDataR.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().data() != null ? c.getValue().data().format(FMT_DATA) : ""));
        colDataR.setMinWidth(120);

        TableColumn<FluxoCaixaDiarioDTO, String> colTotalR = new TableColumn<>("Total Saídas");
        colTotalR.setCellValueFactory(c -> new SimpleStringProperty(
                Dinheiro.formatar(c.getValue().totalSaidas())));
        colTotalR.setMinWidth(150);

        tblRealizado.getColumns().addAll(colDataR, colTotalR);

        // Previsto
        tblPrevisto.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tblPrevisto.setPlaceholder(new Label("Nenhuma obrigação a vencer"));
        tblPrevisto.setPrefHeight(220);

        TableColumn<FluxoCaixaPrevistoDTO.ObrigacaoPrevistaDTO, String> colDesc = new TableColumn<>("Descrição");
        colDesc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().descricao()));
        colDesc.setMinWidth(200);

        TableColumn<FluxoCaixaPrevistoDTO.ObrigacaoPrevistaDTO, String> colForn = new TableColumn<>("Fornecedor");
        colForn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().fornecedorNome() != null ? c.getValue().fornecedorNome() : "—"));
        colForn.setMinWidth(150);

        TableColumn<FluxoCaixaPrevistoDTO.ObrigacaoPrevistaDTO, String> colCat = new TableColumn<>("Categoria");
        colCat.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().categoriaNome() != null ? c.getValue().categoriaNome() : "—"));
        colCat.setMinWidth(130);

        TableColumn<FluxoCaixaPrevistoDTO.ObrigacaoPrevistaDTO, String> colVenc = new TableColumn<>("Vencimento");
        colVenc.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().dataVencimento() != null ? c.getValue().dataVencimento().format(FMT_DATA) : ""));
        colVenc.setMinWidth(110);

        TableColumn<FluxoCaixaPrevistoDTO.ObrigacaoPrevistaDTO, String> colValor = new TableColumn<>("Valor");
        colValor.setCellValueFactory(c -> new SimpleStringProperty(
                Dinheiro.formatar(c.getValue().valor())));
        colValor.setMinWidth(120);

        TableColumn<FluxoCaixaPrevistoDTO.ObrigacaoPrevistaDTO, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().statusParcela() != null ? c.getValue().statusParcela().getDescricao() : ""));
        colStatus.setMinWidth(120);
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item);
                    FluxoCaixaPrevistoDTO.ObrigacaoPrevistaDTO p = getTableView().getItems().get(getIndex());
                    if (p.dataVencimento().isBefore(LocalDate.now()) &&
                            p.statusParcela() != com.fasteasy.domain.PayableInstallmentStatus.PAID) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else if (p.statusParcela() == com.fasteasy.domain.PayableInstallmentStatus.PAID) {
                        setStyle("-fx-text-fill: #27ae60;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        tblPrevisto.getColumns().addAll(colDesc, colForn, colCat, colVenc, colValor, colStatus);
    }

    public void aoExibir() {
        atualizar();
    }

    private void atualizar() {
        LocalDateTime inicio = null;
        LocalDateTime fim = LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0);

        String periodo = periodoRealizado.getValue();
        LocalDate hoje = LocalDate.now();
        if ("Hoje".equals(periodo)) {
            inicio = hoje.atStartOfDay();
        } else if ("Últimos 7 dias".equals(periodo)) {
            inicio = hoje.minusDays(7).atStartOfDay();
        } else if ("Últimos 30 dias".equals(periodo)) {
            inicio = hoje.minusDays(30).atStartOfDay();
        } else if ("Mês atual".equals(periodo)) {
            inicio = hoje.withDayOfMonth(1).atStartOfDay();
        } else if ("Personalizado".equals(periodo)) {
            inicio = dtInicio.getValue() != null ? dtInicio.getValue().atStartOfDay() : null;
            fim = dtFim.getValue() != null ? dtFim.getValue().plusDays(1).atStartOfDay() : fim;
        }

        final LocalDateTime inicioFinal = inicio;
        final LocalDateTime fimFinal = fim;

        // Resumo financeiro
        TarefaAssincrona.executar(payableAccountService::resumoFinanceiro,
                resumo -> atualizarResumo(resumo),
                e -> lblMensagem.setText("Erro ao carregar resumo: " + e.getMessage()));

        // Fluxo realizado
        TarefaAssincrona.executar(
                () -> financialService.fluxoRealizadoPorPeriodo(inicioFinal, fimFinal),
                lista -> {
                    dadosRealizado.setAll(lista);
                    BigDecimal total = lista.stream()
                            .map(FluxoCaixaDiarioDTO::totalSaidas)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    lblMensagem.setText("Fluxo realizado: " + lista.size() + " dia(s) | Total: " + Dinheiro.formatar(total));
                },
                e -> lblMensagem.setText("Erro no fluxo realizado: " + e.getMessage())
        );

        // Fluxo previsto (próximos 60 dias)
        TarefaAssincrona.executar(
                () -> financialService.fluxoPrevisto(60),
                previsto -> {
                    dadosPrevisto.setAll(previsto.obrigacoes());
                    lblMensagem.setText(lblMensagem.getText() + " | Previsto: " + previsto.obrigacoes().size() +
                            " obrigações | Total: " + Dinheiro.formatar(previsto.totalPrevisto()));
                },
                e -> lblMensagem.setText("Erro no fluxo previsto: " + e.getMessage())
        );
    }

    private void atualizarResumo(ResumoFinanceiroDTO resumo) {
        lblResumo.setText(String.format(
                "Em aberto: %s  |  Vencidas: %s  |  Próx. 7 dias: %s  |  Próx. 30 dias: %s  |  Qtd contas: %d  |  Qtd vencidas: %d",
                Dinheiro.formatar(resumo.totalEmAberto()),
                Dinheiro.formatar(resumo.totalVencidas()),
                Dinheiro.formatar(resumo.totalAVencer7Dias()),
                Dinheiro.formatar(resumo.totalAVencer30Dias()),
                resumo.quantidadeContasAberto(),
                resumo.quantidadeContasVencidas()
        ));
    }
}