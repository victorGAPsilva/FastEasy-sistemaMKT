package com.fasteasy.presentation.view;

import com.fasteasy.domain.Dinheiro;
import com.fasteasy.domain.StatusVenda;
import com.fasteasy.presentation.util.TarefaAssincrona;
import com.fasteasy.service.SaleService;
import com.fasteasy.service.dto.VendaResumo;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Tela de histórico de vendas.
 *
 * <p>Permite consultar vendas por número, período e status, visualizar
 * detalhes e reimprimir o comprovante. Vendas concluídas não são alteradas
 * por esta tela.</p>
 */
@Component
public class TelaHistoricoVendas extends VBox {

    private static final Logger log = LoggerFactory.getLogger(TelaHistoricoVendas.class);
    private static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final SaleService saleService;

    private final TextField txtNumero = new TextField();
    private final DatePicker dateInicio = new DatePicker();
    private final DatePicker dateFim = new DatePicker();
    private final ComboBox<String> filtroStatus = new ComboBox<>();
    private final ObservableList<VendaResumo> dados = FXCollections.observableArrayList();
    private final TableView<VendaResumo> tabela = new TableView<>(dados);
    private final Label lblMensagem = new Label("Informe os filtros e clique em Pesquisar.");

    public TelaHistoricoVendas(SaleService saleService) {
        this.saleService = saleService;
        setSpacing(12);
        setPadding(new Insets(4, 0, 0, 0));
        getChildren().addAll(filtros(), tabela, lblMensagem);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        configurarColunas();
    }

    private Region filtros() {
        txtNumero.setPromptText("Número da venda");
        txtNumero.setPrefWidth(150);
        txtNumero.setOnAction(e -> pesquisar());

        dateInicio.setPromptText("Data inicial");
        dateFim.setPromptText("Data final");

        filtroStatus.setItems(FXCollections.observableArrayList(
                "Todas", "Concluídas", "Canceladas", "Em andamento"));
        filtroStatus.setValue("Todas");
        filtroStatus.setPrefWidth(140);

        Button btnPesquisar = new Button("Pesquisar");
        btnPesquisar.getStyleClass().add("button-primary");
        btnPesquisar.setOnAction(e -> pesquisar());

        Button btnLimpar = new Button("Limpar");
        btnLimpar.getStyleClass().add("button-secondary");
        btnLimpar.setOnAction(e -> limparFiltros());

        Button btnDetalhe = new Button("Ver / Reimprimir comprovante");
        btnDetalhe.getStyleClass().add("button-secondary");
        btnDetalhe.setOnAction(e -> verDetalhe());

        HBox linha = new HBox(10,
                new Label("Número:"), txtNumero,
                new Label("De:"), dateInicio,
                new Label("Até:"), dateFim,
                new Label("Status:"), filtroStatus,
                btnPesquisar, btnLimpar);
        linha.setAlignment(Pos.CENTER_LEFT);

        HBox acoes = new HBox(10, btnDetalhe);
        acoes.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(10, linha, acoes);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(10, 14, 10, 14));
        return box;
    }

    private void configurarColunas() {
        tabela.setPlaceholder(new Label("Nenhuma venda encontrada"));
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<VendaResumo, String> colNumero = coluna("Número", VendaResumo::numero, 90);
        TableColumn<VendaResumo, String> colData = coluna("Data/Hora",
                v -> v.dataHora() == null ? "" : v.dataHora().format(FORMATO_DATA_HORA), 150);
        TableColumn<VendaResumo, String> colTotal = coluna("Total",
                v -> Dinheiro.formatar(v.valorTotal()), 110);
        TableColumn<VendaResumo, String> colPagamento = coluna("Pagamento(s)", VendaResumo::formasPagamento, 140);
        TableColumn<VendaResumo, String> colStatus = coluna("Status",
                v -> v.status() == null ? "" : v.status().getDescricao(), 100);
        TableColumn<VendaResumo, String> colOperador = coluna("Operador",
                v -> v.operador() == null ? "—" : v.operador(), 130);

        tabela.getColumns().addAll(colNumero, colData, colTotal, colPagamento, colStatus, colOperador);

        tabela.setRowFactory(tv -> {
            TableRow<VendaResumo> linha = new TableRow<>();
            linha.setOnMouseClicked(evento -> {
                if (evento.getClickCount() == 2 && !linha.isEmpty()) {
                    verDetalhe(linha.getItem());
                }
            });
            return linha;
        });
    }

    private TableColumn<VendaResumo, String> coluna(String titulo, java.util.function.Function<VendaResumo, String> extrator, double min) {
        TableColumn<VendaResumo, String> coluna = new TableColumn<>(titulo);
        coluna.setCellValueFactory(cell -> new SimpleStringProperty(extrator.apply(cell.getValue())));
        coluna.setMinWidth(min);
        return coluna;
    }

    /** Recarrega a lista using os filtros atuais. */
    public void atualizarLista() {
        pesquisar();
    }

    private void pesquisar() {
        String numero = txtNumero.getText();
        LocalDateTime inicio = dateInicio.getValue() == null ? null : dateInicio.getValue().atStartOfDay();
        LocalDateTime fim = dateFim.getValue() == null ? null : dateFim.getValue().plusDays(1).atStartOfDay();
        StatusVenda status = mapearStatus(filtroStatus.getValue());

        TarefaAssincrona.executar(
                () -> saleService.listarHistorico(numero, inicio, fim, status),
                resultado -> {
                    dados.setAll(resultado);
                    lblMensagem.setText(resultado.size() + " venda(s) encontrada(s).");
                },
                e -> {
                    log.error("Erro ao consultar histórico de vendas: {}", e.getMessage(), e);
                    lblMensagem.setText("Não foi possível consultar o histórico. Consulte os logs.");
                });
    }

    private void limparFiltros() {
        txtNumero.clear();
        dateInicio.setValue(null);
        dateFim.setValue(null);
        filtroStatus.setValue("Todas");
        atualizarLista();
    }

    private void verDetalhe() {
        VendaResumo selecionada = tabela.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            aviso("Selecione uma venda na lista.");
            return;
        }
        verDetalhe(selecionada);
    }

    private void verDetalhe(VendaResumo venda) {
        if (venda == null) {
            return;
        }
        TarefaAssincrona.executar(
                () -> saleService.buscarDetalhe(venda.id()),
                detalhe -> ComprovanteDialog.mostrar(janela(), detalhe),
                e -> {
                    log.error("Erro ao carregar detalhe da venda {}: {}", venda.id(), e.getMessage(), e);
                    aviso("Não foi possível carregar o comprovante da venda.");
                });
    }

    private StatusVenda mapearStatus(String valor) {
        if (valor == null) {
            return null;
        }
        return switch (valor) {
            case "Concluídas" -> StatusVenda.COMPLETED;
            case "Canceladas" -> StatusVenda.CANCELLED;
            case "Em andamento" -> StatusVenda.IN_PROGRESS;
            default -> null;
        };
    }

    private javafx.stage.Window janela() {
        return getScene() == null ? null : getScene().getWindow();
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
}
