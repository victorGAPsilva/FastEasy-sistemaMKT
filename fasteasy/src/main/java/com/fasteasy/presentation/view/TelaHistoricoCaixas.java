package com.fasteasy.presentation.view;

import com.fasteasy.domain.Dinheiro;
import com.fasteasy.domain.StatusSessaoCaixa;
import com.fasteasy.presentation.util.TarefaAssincrona;
import com.fasteasy.service.CashSessionService;
import com.fasteasy.service.dto.MovimentacaoCaixaDTO;
import com.fasteasy.service.dto.SessaoCaixaDetalhe;
import com.fasteasy.service.dto.SessaoCaixaResumo;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Tela de histórico de sessões de caixa (aberturas e fechamentos).
 *
 * <p>Permite consultar sessões por caixa, período e status, visualizar
 * detalhes e reimprimir o relatório de fechamento.</p>
 */
@Component
public class TelaHistoricoCaixas extends VBox {

    private static final Logger log = LoggerFactory.getLogger(TelaHistoricoCaixas.class);
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final CashSessionService cashSessionService;

    private final ComboBox<String> filtroStatus = new ComboBox<>();
    private final DatePicker dtInicio = new DatePicker();
    private final DatePicker dtFim = new DatePicker();
    private final Button btnBuscar = new Button("Buscar");
    private final Button btnLimpar = new Button("Limpar");

    private final ObservableList<SessaoCaixaResumo> dados = FXCollections.observableArrayList();
    private final TableView<SessaoCaixaResumo> tabela = new TableView<>(dados);

    public TelaHistoricoCaixas(CashSessionService cashSessionService) {
        this.cashSessionService = cashSessionService;
        setSpacing(12);
        setPadding(new Insets(16));

        configurarFiltros();
        configurarTabela();
        getChildren().addAll(filtrosBox(), tabela);
        VBox.setVgrow(tabela, Priority.ALWAYS);
    }

    private void configurarFiltros() {
        filtroStatus.getItems().addAll("Todos", StatusSessaoCaixa.OPEN.getDescricao(), StatusSessaoCaixa.CLOSED.getDescricao());
        filtroStatus.setValue("Todos");
        filtroStatus.setPrefWidth(150);

        btnBuscar.getStyleClass().add("button-primary");
        btnBuscar.setOnAction(e -> atualizarLista());

        btnLimpar.getStyleClass().add("button-secondary");
        btnLimpar.setOnAction(e -> {
            filtroStatus.setValue("Todos");
            dtInicio.setValue(null);
            dtFim.setValue(null);
            atualizarLista();
        });
    }

    private HBox filtrosBox() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(12));

        box.getChildren().addAll(
                new Label("Status:"), filtroStatus,
                new Label("Início:"), dtInicio,
                new Label("Fim:"), dtFim,
                btnBuscar, btnLimpar
        );
        return box;
    }

    private void configurarTabela() {
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabela.setPlaceholder(new Label("Nenhuma sessão encontrada"));

        TableColumn<SessaoCaixaResumo, String> colSessao = new TableColumn<>("Sessão");
        colSessao.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().id().toString()));
        colSessao.setMinWidth(70);

        TableColumn<SessaoCaixaResumo, String> colCaixa = new TableColumn<>("Caixa");
        colCaixa.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().caixaNome()));
        colCaixa.setMinWidth(100);

        TableColumn<SessaoCaixaResumo, String> colOperador = new TableColumn<>("Operador");
        colOperador.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().operador()));
        colOperador.setMinWidth(120);

        TableColumn<SessaoCaixaResumo, String> colAbertura = new TableColumn<>("Abertura");
        colAbertura.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().dataAbertura() == null ? "" : c.getValue().dataAbertura().format(FORMATO)));
        colAbertura.setMinWidth(140);

        TableColumn<SessaoCaixaResumo, String> colFechamento = new TableColumn<>("Fechamento");
        colFechamento.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().dataFechamento() == null ? "" : c.getValue().dataFechamento().format(FORMATO)));
        colFechamento.setMinWidth(140);

        TableColumn<SessaoCaixaResumo, String> colSaldoInicial = new TableColumn<>("Saldo Inicial");
        colSaldoInicial.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().saldoInicial() == null ? "" : Dinheiro.formatar(c.getValue().saldoInicial())));
        colSaldoInicial.setMinWidth(110);

        TableColumn<SessaoCaixaResumo, String> colEsperado = new TableColumn<>("Saldo Esperado");
        colEsperado.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().saldoEsperado() == null ? "" : Dinheiro.formatar(c.getValue().saldoEsperado())));
        colEsperado.setMinWidth(110);

        TableColumn<SessaoCaixaResumo, String> colContado = new TableColumn<>("Total Contado");
        colContado.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().totalContado() == null ? "" : Dinheiro.formatar(c.getValue().totalContado())));
        colContado.setMinWidth(110);

        TableColumn<SessaoCaixaResumo, String> colDiferenca = new TableColumn<>("Diferença");
        colDiferenca.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().diferenca() == null ? "" : Dinheiro.formatar(c.getValue().diferenca())));
        colDiferenca.setMinWidth(110);

        TableColumn<SessaoCaixaResumo, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().status() == null ? "" : c.getValue().status().getDescricao()));
        colStatus.setMinWidth(100);

        tabela.getColumns().addAll(colSessao, colCaixa, colOperador, colAbertura, colFechamento,
                colSaldoInicial, colEsperado, colContado, colDiferenca, colStatus);

        TableView.TableViewSelectionModel<SessaoCaixaResumo> sm = tabela.getSelectionModel();
        sm.setSelectionMode(SelectionMode.SINGLE);
        sm.selectedItemProperty().addListener((obs, antigo, novo) -> {
            if (novo != null) {
                verDetalhes(novo);
            }
        });
    }

    private void verDetalhes(SessaoCaixaResumo resumo) {
        TarefaAssincrona.executar(
                () -> cashSessionService.detalhe(resumo.id()),
                detalhe -> RelatorioCaixaDialog.mostrar(janela(), detalhe),
                e -> erro("Não foi possível carregar o detalhe da sessão", e));
    }

    /** Chamado ao exibir a tela. */
    public void aoExibir() {
        atualizarLista();
    }

    public void atualizarLista() {
        StatusSessaoCaixa status = statusSelecionado();
        LocalDateTime inicio = dtInicio.getValue() == null ? null : dtInicio.getValue().atStartOfDay();
        LocalDateTime fim = dtFim.getValue() == null ? null : dtFim.getValue().plusDays(1).atStartOfDay();

        TarefaAssincrona.executar(
                () -> cashSessionService.listarHistorico(null, inicio, fim, status),
                lista -> dados.setAll(lista),
                e -> erro("Erro ao carregar histórico", e));
    }

    private StatusSessaoCaixa statusSelecionado() {
        String valor = filtroStatus.getValue();
        if (valor == null || valor.equals("Todos")) {
            return null;
        }
        for (StatusSessaoCaixa s : StatusSessaoCaixa.values()) {
            if (valor.equals(s.getDescricao())) {
                return s;
            }
        }
        return null;
    }

    private void erro(String titulo, Throwable e) {
        if (e instanceof IllegalArgumentException) {
            aviso(e.getMessage());
        } else {
            log.error("{}: {}", titulo, e.getMessage(), e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro");
            alert.setHeaderText(null);
            alert.setContentText("Ocorreu um erro inesperado. Consulte os logs do sistema.");
            if (janela() != null) {
                alert.initOwner(janela());
            }
            alert.showAndWait();
        }
    }

    private void aviso(String mensagem) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Atenção");
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        if (janela() != null) {
            alert.initOwner(janela());
        }
        alert.showAndWait();
    }

    private Window janela() {
        return getScene() == null ? null : getScene().getWindow();
    }
}