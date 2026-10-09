package com.fasteasy.presentation.view;

import com.fasteasy.domain.CashRegister;
import com.fasteasy.domain.CashSession;
import com.fasteasy.domain.Dinheiro;
import com.fasteasy.domain.FormaPagamento;
import com.fasteasy.domain.TipoMovimentacaoCaixa;
import com.fasteasy.presentation.util.TarefaAssincrona;
import com.fasteasy.service.CashRegisterService;
import com.fasteasy.service.CashSessionService;
import com.fasteasy.service.dto.MovimentacaoCaixaDTO;
import com.fasteasy.service.dto.ResumoCaixaDTO;
import com.fasteasy.service.dto.SessaoCaixaDetalhe;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Tela de gestão de caixa: abertura, movimentações, resumo e fechamento.
 *
 * <p>Nenhuma regra de negócio ou acesso a banco é executado na thread da
 * interface: as operações usam {@link TarefaAssincrona} e delegam ao
 * {@link CashSessionService}.</p>
 */
@Component
public class TelaCaixa extends VBox {

    private static final Logger log = LoggerFactory.getLogger(TelaCaixa.class);
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final CashRegisterService cashRegisterService;
    private final CashSessionService cashSessionService;

    private final ComboBox<CashRegister> cmbCaixa = new ComboBox<>();
    private final TextField txtOperador = new TextField();
    private final Label lblStatusSessao = new Label("—");
    private final Label lblMensagem = new Label("Selecione um caixa para começar.");

    private final VBox painelConteudo = new VBox(14);
    private final ObservableList<MovimentacaoCaixaDTO> movimentos = FXCollections.observableArrayList();
    private final ComboBox<String> filtroMovimento = new ComboBox<>();

    private ResumoCaixaDTO resumoAtual;

    public TelaCaixa(CashRegisterService cashRegisterService, CashSessionService cashSessionService) {
        this.cashRegisterService = cashRegisterService;
        this.cashSessionService = cashSessionService;
        setSpacing(12);
        setPadding(new Insets(4, 0, 0, 0));

        ScrollPane scroll = new ScrollPane(painelConteudo);
        scroll.setFitToWidth(true);
        getChildren().addAll(cabecalho(), scroll, lblMensagem);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        filtroMovimento.getItems().add("Todos os tipos");
        for (TipoMovimentacaoCaixa tipo : TipoMovimentacaoCaixa.values()) {
            filtroMovimento.getItems().add(tipo.getDescricao());
        }
        filtroMovimento.setValue("Todos os tipos");
        filtroMovimento.setPrefWidth(180);
        filtroMovimento.valueProperty().addListener((obs, antigo, novo) -> carregarMovimentos());
    }

    // ------------------------------------------------------------------
    // Cabeçalho e navegação
    // ------------------------------------------------------------------

    private Node cabecalho() {
        cmbCaixa.setPrefWidth(190);
        cmbCaixa.valueProperty().addListener((obs, antigo, novo) -> atualizarSessao());

        txtOperador.setPromptText("Operador");
        txtOperador.setPrefWidth(150);

        Button btnGerenciar = new Button("Gerenciar caixas");
        btnGerenciar.getStyleClass().add("button-secondary");
        btnGerenciar.setOnAction(e -> {
            CadastroCaixasDialog.mostrar(janela(), cashRegisterService);
            carregarCaixas();
        });

        lblStatusSessao.setStyle("-fx-font-weight: bold; -fx-text-fill: #546e7a;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox box = new HBox(12, new Label("Caixa:"), cmbCaixa, btnGerenciar,
                spacer, new Label("Operador:"), txtOperador, lblStatusSessao);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(10, 14, 10, 14));
        return box;
    }

    /** Chamado ao exibir a tela. */
    public void aoExibir() {
        carregarCaixas();
    }

    private void carregarCaixas() {
        TarefaAssincrona.executar(
                () -> cashRegisterService.listarAtivos(),
                caixas -> {
                    Long selecionado = cmbCaixa.getValue() == null ? null : cmbCaixa.getValue().getId();
                    cmbCaixa.setItems(FXCollections.observableArrayList(caixas));
                    if (caixas.isEmpty()) {
                        resumoAtual = null;
                        lblStatusSessao.setText("Sem caixas");
                        painelConteudo.getChildren().setAll(mensagemCard(
                                "Nenhum caixa ativo cadastrado. Use \"Gerenciar caixas\" para cadastrar."));
                        return;
                    }
                    CashRegister alvo = caixas.stream()
                            .filter(c -> c.getId().equals(selecionado))
                            .findFirst()
                            .orElse(caixas.get(0));
                    cmbCaixa.setValue(alvo);
                    atualizarSessao();
                },
                e -> lblMensagem.setText("Não foi possível carregar os caixas. Consulte os logs."));
    }

    private void atualizarSessao() {
        CashRegister caixa = cmbCaixa.getValue();
        if (caixa == null) {
            return;
        }
        Long caixaId = caixa.getId();
        TarefaAssincrona.executar(
                () -> {
                    Optional<CashSession> sessao = cashSessionService.buscarSessaoAberta(caixaId);
                    return sessao.map(s -> cashSessionService.detalhe(s.getId())).orElse(null);
                },
                detalhe -> {
                    if (detalhe == null) {
                        renderAbertura(caixa);
                    } else {
                        renderSessao(caixa, detalhe);
                    }
                },
                e -> lblMensagem.setText("Não foi possível consultar a sessão de caixa. Consulte os logs."));
    }

    // ------------------------------------------------------------------
    // Abertura
    // ------------------------------------------------------------------

    private void renderAbertura(CashRegister caixa) {
        resumoAtual = null;
        lblStatusSessao.setText("Fechado");
        lblStatusSessao.setStyle("-fx-font-weight: bold; -fx-text-fill: #e74c3c;");

        TextField txtSaldo = new TextField("0,00");
        txtSaldo.setPrefWidth(140);
        TextArea txtObs = new TextArea();
        txtObs.setPromptText("Observações de abertura (opcional)");
        txtObs.setPrefRowCount(2);
        txtObs.setPrefWidth(380);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Caixa:"), 0, 0);
        grid.add(new Label(caixa.getNome()), 1, 0);
        grid.add(new Label("Saldo inicial:"), 0, 1);
        grid.add(txtSaldo, 1, 1);
        grid.add(new Label("Observações:"), 0, 2);
        grid.add(txtObs, 1, 2);

        Button btnAbrir = new Button("Abrir caixa");
        btnAbrir.getStyleClass().add("button-primary");
        btnAbrir.setOnAction(e -> {
            BigDecimal saldo = parseMoeda(txtSaldo.getText());
            if (saldo == null) {
                saldo = BigDecimal.ZERO;
            }
            if (saldo.signum() < 0) {
                aviso("O saldo inicial não pode ser negativo.");
                return;
            }
            btnAbrir.setDisable(true);
            Long caixaId = caixa.getId();
            BigDecimal valor = saldo;
            TarefaAssincrona.executar(
                    () -> cashSessionService.abrirSessao(caixaId, valor, txtObs.getText(), operador()),
                    sessao -> {
                        lblMensagem.setText("Caixa " + caixa.getNome() + " aberto com "
                                + Dinheiro.formatar(valor) + ".");
                        atualizarSessao();
                    },
                    erro -> {
                        btnAbrir.setDisable(false);
                        tratarErro("Erro ao abrir o caixa", erro);
                    });
        });

        painelConteudo.getChildren().setAll(card("Abertura de caixa",
                new Label("Informe o saldo inicial (fundo de troco) e confirme."), grid, btnAbrir));
        lblMensagem.setText("O caixa " + caixa.getNome() + " está fechado.");
    }

    // ------------------------------------------------------------------
    // Sessão aberta
    // ------------------------------------------------------------------

    private void renderSessao(CashRegister caixa, SessaoCaixaDetalhe detalhe) {
        resumoAtual = detalhe.resumo();
        lblStatusSessao.setText("Aberto");
        lblStatusSessao.setStyle("-fx-font-weight: bold; -fx-text-fill: #27ae60;");

        VBox movimentosBox = cardMovimentos(caixa);
        VBox fechamentoBox = cardFechamento(caixa);
        HBox.setHgrow(movimentosBox, Priority.ALWAYS);
        fechamentoBox.setPrefWidth(330);
        fechamentoBox.setMinWidth(310);

        painelConteudo.getChildren().setAll(
                cardResumo(resumoAtual),
                new HBox(14, movimentosBox, fechamentoBox));
        carregarMovimentos();
        lblMensagem.setText("Caixa " + caixa.getNome() + " aberto. Adicione movimentações ou feche o caixa.");
    }

    private VBox cardResumo(ResumoCaixaDTO r) {
        GridPane grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(6);
        int row = 0;
        info(grid, 0, row, "Caixa", texto(r.caixaNome()));
        info(grid, 1, row++, "Operador", texto(r.operador()));
        info(grid, 0, row, "Abertura", r.dataAbertura() == null ? "—" : r.dataAbertura().format(FORMATO));
        info(grid, 1, row++, "Sessão nº", String.valueOf(r.sessaoId()));
        info(grid, 0, row, "Saldo inicial", Dinheiro.formatar(r.saldoInicial()));
        info(grid, 1, row++, "Saldo esperado (dinheiro)", Dinheiro.formatar(r.saldoEsperado()));
        info(grid, 0, row, "Dinheiro recebido (entregue)", Dinheiro.formatar(r.totalDinheiroRecebido()));
        info(grid, 1, row++, "Trocos devolvidos", Dinheiro.formatar(r.totalTroco()));
        info(grid, 0, row, "Suprimentos", Dinheiro.formatar(r.totalSuprimentos()));
        info(grid, 1, row++, "Sangrias", Dinheiro.formatar(r.totalSangrias()));
        info(grid, 0, row, "Outras entradas", Dinheiro.formatar(r.totalOutrasEntradas()));
        info(grid, 1, row++, "Outras saídas", Dinheiro.formatar(r.totalOutrasSaidas()));
        info(grid, 0, row, "Vendas (dinheiro)", Dinheiro.formatar(r.totalPorForma(FormaPagamento.DINHEIRO)));
        info(grid, 1, row++, "PIX", Dinheiro.formatar(r.totalPorForma(FormaPagamento.PIX)));
        info(grid, 0, row, "Débito", Dinheiro.formatar(r.totalPorForma(FormaPagamento.DEBITO)));
        info(grid, 1, row++, "Crédito", Dinheiro.formatar(r.totalPorForma(FormaPagamento.CREDITO)));
        info(grid, 0, row, "Quantidade de vendas", String.valueOf(r.quantidadeVendas()));
        info(grid, 1, row, "Faturamento total", Dinheiro.formatar(faturamento(r)));
        return card("Resumo da sessão", grid);
    }

    private VBox cardMovimentos(CashRegister caixa) {
        Button btnSuprimento = botaoMovimento("Suprimento", TipoMovimentacaoCaixa.SUPPLY, caixa);
        Button btnSangria = botaoMovimento("Sangria", TipoMovimentacaoCaixa.WITHDRAWAL, caixa);
        Button btnOutraEntrada = botaoMovimento("Outra entrada", TipoMovimentacaoCaixa.OTHER_IN, caixa);
        Button btnOutraSaida = botaoMovimento("Outra saída", TipoMovimentacaoCaixa.OTHER_OUT, caixa);

        TableView<MovimentacaoCaixaDTO> tabela = criarTabelaMovimentos();
        VBox.setVgrow(tabela, Priority.ALWAYS);

        HBox acoes = new HBox(8, btnSuprimento, btnSangria, btnOutraEntrada, btnOutraSaida);
        acoes.setAlignment(Pos.CENTER_LEFT);

        VBox box = card("Movimentações",
                acoes,
                new HBox(8, new Label("Filtrar por tipo:"), filtroMovimento),
                tabela);
        return box;
    }

    private TableView<MovimentacaoCaixaDTO> criarTabelaMovimentos() {
        TableView<MovimentacaoCaixaDTO> tabela = new TableView<>(movimentos);
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabela.setPlaceholder(new Label("Nenhuma movimentação registrada"));
        tabela.setPrefHeight(280);

        TableColumn<MovimentacaoCaixaDTO, String> colData = new TableColumn<>("Data/Hora");
        colData.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().dataHora() == null ? "" : c.getValue().dataHora().format(FORMATO)));
        colData.setMinWidth(140);

        TableColumn<MovimentacaoCaixaDTO, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().tipo() == null ? "" : c.getValue().tipo().getDescricao()));
        colTipo.setMinWidth(110);

        TableColumn<MovimentacaoCaixaDTO, String> colValor = new TableColumn<>("Valor");
        colValor.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                Dinheiro.formatar(c.getValue().valor())));
        colValor.setMinWidth(100);

        TableColumn<MovimentacaoCaixaDTO, String> colMotivo = new TableColumn<>("Motivo");
        colMotivo.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().motivo() == null ? "" : c.getValue().motivo()));
        colMotivo.setMinWidth(160);

        TableColumn<MovimentacaoCaixaDTO, String> colOperador = new TableColumn<>("Operador");
        colOperador.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().operador() == null ? "—" : c.getValue().operador()));
        colOperador.setMinWidth(110);

        tabela.getColumns().clear();
        tabela.getColumns().addAll(colData, colTipo, colValor, colMotivo, colOperador);
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        return tabela;
    }

    private void carregarMovimentos() {
        if (resumoAtual == null) {
            movimentos.clear();
            return;
        }
        Long sessaoId = resumoAtual.sessaoId();
        TipoMovimentacaoCaixa tipo = tipoSelecionadoFiltro();
        TarefaAssincrona.executar(
                () -> cashSessionService.listarMovimentacoes(sessaoId, tipo),
                movimentos::setAll,
                e -> lblMensagem.setText("Não foi possível carregar as movimentações."));
    }

    private TipoMovimentacaoCaixa tipoSelecionadoFiltro() {
        String valor = filtroMovimento.getValue();
        if (valor == null || valor.equals("Todos os tipos")) {
            return null;
        }
        for (TipoMovimentacaoCaixa tipo : TipoMovimentacaoCaixa.values()) {
            if (valor.equals(tipo.getDescricao())) {
                return tipo;
            }
        }
        return null;
    }

    private Button botaoMovimento(String texto, TipoMovimentacaoCaixa tipo, CashRegister caixa) {
        Button botao = new Button(texto);
        botao.getStyleClass().add(tipo.isEntrada() ? "button-primary" : "button-danger");
        botao.setOnAction(e -> registrarMovimento(tipo, caixa));
        return botao;
    }

    private void registrarMovimento(TipoMovimentacaoCaixa tipo, CashRegister caixa) {
        if (resumoAtual == null) {
            aviso("Abra o caixa antes de registrar movimentações.");
            return;
        }
        MovimentoCaixaDialog.MovimentoInput input =
                MovimentoCaixaDialog.mostrar(janela(), tipo, caixa.getNome());
        if (input == null) {
            return;
        }
        Long caixaId = caixa.getId();
        TarefaAssincrona.executar(
                () -> cashSessionService.registrarMovimentacao(caixaId, input.tipo(), input.valor(),
                        input.motivo(), operador()),
                movimento -> {
                    lblMensagem.setText(tipo.getDescricao() + " de "
                            + Dinheiro.formatar(movimento.getValor()) + " registrada.");
                    atualizarSessao();
                },
                erro -> tratarErro("Erro ao registrar movimentação", erro));
    }

    private VBox cardFechamento(CashRegister caixa) {
        TextField txtContado = new TextField();
        txtContado.setPromptText("Total contado em dinheiro");
        txtContado.setPrefWidth(150);

        TextArea txtObs = new TextArea();
        txtObs.setPromptText("Observações de fechamento (opcional)");
        txtObs.setPrefRowCount(3);
        txtObs.setPrefWidth(280);

        Label lblDiferenca = new Label("Diferença: informe o valor contado");
        txtContado.textProperty().addListener((obs, antigo, novo) -> {
            if (resumoAtual == null) {
                lblDiferenca.setText("Diferença: —");
                return;
            }
            BigDecimal contado = parseMoeda(novo);
            if (contado == null) {
                lblDiferenca.setText("Diferença: —");
                return;
            }
            BigDecimal diferenca = Dinheiro.arredondar(contado.subtract(resumoAtual.saldoEsperado()));
            String situacao = descricaoSituacao(diferenca);
            lblDiferenca.setText(Dinheiro.formatar(diferenca) + " (" + situacao + ")");
        });

        Button btnFechar = new Button("Fechar caixa");
        btnFechar.getStyleClass().add("button-primary");
        btnFechar.setMaxWidth(Double.MAX_VALUE);
        btnFechar.setOnAction(e -> {
            if (resumoAtual == null) {
                return;
            }
            BigDecimal contado = parseMoeda(txtContado.getText());
            if (contado == null || contado.signum() < 0) {
                aviso("Informe o total contado em dinheiro (valor não negativo).");
                return;
            }
            if (!confirmar("Fechar caixa",
                    "Confirmar o fechamento do caixa " + caixa.getNome() + "?")) {
                return;
            }
            btnFechar.setDisable(true);
            Long caixaId = caixa.getId();
            String obs = txtObs.getText();
            BigDecimal valorContado = contado;
            TarefaAssincrona.executar(
                    () -> {
                        ResumoCaixaDTO fechado = cashSessionService.fecharSessao(caixaId, valorContado, obs, operador());
                        return cashSessionService.detalhe(fechado.sessaoId());
                    },
                    detalhe -> {
                        lblMensagem.setText("Caixa fechado. Diferença: "
                                + Dinheiro.formatar(detalhe.resumo().diferenca())
                                + " (" + descricaoSituacao(detalhe.resumo().diferenca()) + ").");
                        atualizarSessao();
                        RelatorioCaixaDialog.mostrar(janela(), detalhe);
                    },
                    erro -> {
                        btnFechar.setDisable(false);
                        tratarErro("Erro ao fechar o caixa", erro);
                    });
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Saldo esperado:"), 0, 0);
        grid.add(new Label(Dinheiro.formatar(resumoAtual.saldoEsperado())), 1, 0);
        grid.add(new Label("Total contado:"), 0, 1);
        grid.add(txtContado, 1, 1);
        grid.add(new Label("Diferença:"), 0, 2);
        grid.add(lblDiferenca, 1, 2);

        VBox box = card("Fechamento", grid, txtObs, btnFechar);
        box.setSpacing(10);
        return box;
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private VBox card(String titulo, Node... conteudo) {
        Label label = new Label(titulo);
        label.getStyleClass().add("section-title");
        VBox box = new VBox(10);
        box.getChildren().add(label);
        box.getChildren().addAll(conteudo);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(12));
        return box;
    }

    private VBox mensagemCard(String mensagem) {
        Label label = new Label(mensagem);
        label.setWrapText(true);
        return card("Caixa", label);
    }

    private void info(GridPane grid, int col, int row, String titulo, String valor) {
        Label t = new Label(titulo + ":");
        t.setStyle("-fx-text-fill: #546e7a;");
        Label v = new Label(valor);
        HBox h = new HBox(6, t, v);
        grid.add(h, col, row);
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

    private void tratarErro(String titulo, Throwable e) {
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

    private boolean confirmar(String titulo, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        if (janela() != null) {
            alert.initOwner(janela());
        }
        return alert.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    private String operador() {
        String valor = txtOperador.getText();
        if (valor == null) {
            return null;
        }
        String t = valor.trim();
        return t.isEmpty() ? null : t;
    }

    private javafx.stage.Window janela() {
        return getScene() == null ? null : getScene().getWindow();
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? "—" : valor;
    }

    private BigDecimal faturamento(ResumoCaixaDTO r) {
        return r.totalPorForma(FormaPagamento.DINHEIRO)
                .add(r.totalPorForma(FormaPagamento.PIX))
                .add(r.totalPorForma(FormaPagamento.DEBITO))
                .add(r.totalPorForma(FormaPagamento.CREDITO));
    }

    static BigDecimal parseMoeda(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            String valor = texto.trim().replace("R$", "").replaceAll("\\s", "");
            if (valor.contains(",")) {
                valor = valor.replace(".", "").replace(",", ".");
            }
            return Dinheiro.arredondar(new BigDecimal(valor));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String descricaoSituacao(BigDecimal diferenca) {
        return RelatorioCaixaDialog.descricaoSituacao(diferenca);
    }
}
