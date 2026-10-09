package com.fasteasy.presentation.view;

import com.fasteasy.domain.CaixaInvalidoException;
import com.fasteasy.domain.CarrinhoVenda;
import com.fasteasy.domain.CashRegister;
import com.fasteasy.domain.CashSession;
import com.fasteasy.domain.Dinheiro;
import com.fasteasy.domain.ItemCarrinho;
import com.fasteasy.domain.Product;
import com.fasteasy.presentation.util.TarefaAssincrona;
import com.fasteasy.service.CashRegisterService;
import com.fasteasy.service.CashSessionService;
import com.fasteasy.service.ProductService;
import com.fasteasy.service.SaleService;
import com.fasteasy.service.dto.PagamentoDTO;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Tela principal do PDV (Frente de Caixa).
 *
 * <p>A interface é mantida simples: toda a regra de negócio do carrinho fica em
 * {@link CarrinhoVenda} e a finalização transacional no {@code SaleService}.
 * Nenhuma consulta a banco é executada na thread da interface — as operações
 * usam {@link TarefaAssincrona}.</p>
 */
@Component
public class TelaPdv extends VBox {

    private static final Logger log = LoggerFactory.getLogger(TelaPdv.class);
    private static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final ProductService productService;
    private final SaleService saleService;
    private final CashRegisterService cashRegisterService;
    private final CashSessionService cashSessionService;

    private final CarrinhoVenda carrinho = new CarrinhoVenda();
    private final ObservableList<ItemCarrinho> dados = FXCollections.observableArrayList();

    private final TextField txtCodigo = new TextField();
    private final TextField txtBuscaNome = new TextField();
    private final TextField txtOperador = new TextField();
    private final Label lblDataHora = new Label();
    private final TableView<ItemCarrinho> tabela = new TableView<>();

    private final ComboBox<CashRegister> cmbCaixa = new ComboBox<>();
    private final Label lblSessaoStatus = new Label("—");

    private final Label lblQtdItens = new Label("0");
    private final Label lblSubtotal = new Label(Dinheiro.formatar(BigDecimal.ZERO));
    private final Label lblDescontos = new Label(Dinheiro.formatar(BigDecimal.ZERO));
    private final Label lblTotal = new Label(Dinheiro.formatar(BigDecimal.ZERO));
    private final Label lblMensagem = new Label("Pronto para registrar produtos.");

    private final Button btnPagamento = new Button("Pagamento (F4)");
    private PauseTransition pausaCodigo;
    private boolean finalizando;

    public TelaPdv(ProductService productService,
                        SaleService saleService,
                        CashRegisterService cashRegisterService,
                        CashSessionService cashSessionService) {
        this.productService = productService;
        this.saleService = saleService;
        this.cashRegisterService = cashRegisterService;
        this.cashSessionService = cashSessionService;
        setSpacing(12);
        setPadding(new Insets(4, 0, 0, 0));
        inicializarComponentes();
        configurarColunas();
        configurarLeituraCodigo();
        iniciarRelogio();
        atualizarResumo();
    }

    // ------------------------------------------------------------------
    // Construção da interface
    // ------------------------------------------------------------------

    private void inicializarComponentes() {
        Node cabecalho = cabecalho();
        Node leitura = leitura();
        Node corpo = corpo();
        VBox.setVgrow(corpo, Priority.ALWAYS);
        getChildren().addAll(cabecalho, leitura, corpo, lblMensagem);
    }

    private Node cabecalho() {
        Label titulo = new Label("FastEasy — Frente de Caixa");
        titulo.getStyleClass().add("page-title");

        txtOperador.setPromptText("Operador");
        txtOperador.setPrefWidth(150);

        cmbCaixa.setPrefWidth(180);
        cmbCaixa.setPromptText("Selecione o caixa");
        cmbCaixa.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(CashRegister caixa, boolean empty) {
                super.updateItem(caixa, empty);
                setText(empty || caixa == null ? null : caixa.getNome());
            }
        });
        cmbCaixa.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(CashRegister caixa, boolean empty) {
                super.updateItem(caixa, empty);
                setText(empty || caixa == null ? null : caixa.getNome());
            }
        });
        cmbCaixa.valueProperty().addListener((obs, antigo, novo) -> atualizarSessao());

        lblSessaoStatus.getStyleClass().add("pdv-caixa");
        lblSessaoStatus.setStyle("-fx-font-weight: bold;");
        lblDataHora.getStyleClass().add("pdv-data-hora");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox box = new HBox(12, titulo, spacer,
                new Label("Operador:"), txtOperador,
                new Label("Caixa:"), cmbCaixa, lblSessaoStatus, lblDataHora);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(10, 14, 10, 14));
        return box;
    }

    private Node leitura() {
        txtCodigo.setPromptText("Código de barras / SKU");
        txtCodigo.setPrefWidth(260);
        txtCodigo.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        Button btnAdicionar = new Button("Adicionar");
        btnAdicionar.getStyleClass().add("button-primary");
        btnAdicionar.setOnAction(e -> adicionarPorCodigo());

        txtBuscaNome.setPromptText("Buscar por nome (F2)");
        txtBuscaNome.setPrefWidth(220);
        txtBuscaNome.setOnAction(e -> buscarPorNome());

        Button btnBuscar = new Button("Buscar");
        btnBuscar.getStyleClass().add("button-secondary");
        btnBuscar.setOnAction(e -> buscarPorNome());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox box = new HBox(10, new Label("Código:"), txtCodigo, btnAdicionar,
                spacer, new Label("Nome:"), txtBuscaNome, btnBuscar);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(10, 14, 10, 14));
        return box;
    }

    private Node corpo() {
        HBox box = new HBox(12, areaCarrinho(), areaResumo());
        HBox.setHgrow(box.getChildren().get(0), Priority.ALWAYS);
        return box;
    }

    private Node areaCarrinho() {
        tabela.setItems(dados);
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabela.setPlaceholder(new Label("Nenhum item no carrinho"));

        Button btnAumentar = new Button("Aumentar (+)");
        btnAumentar.getStyleClass().add("button-secondary");
        btnAumentar.setOnAction(e -> comItemSelecionado(this::aumentarItem));

        Button btnDiminuir = new Button("Diminuir (−)");
        btnDiminuir.getStyleClass().add("button-secondary");
        btnDiminuir.setOnAction(e -> comItemSelecionado(this::diminuirItem));

        Button btnQuantidade = new Button("Quantidade...");
        btnQuantidade.getStyleClass().add("button-secondary");
        btnQuantidade.setOnAction(e -> comItemSelecionado(this::alterarQuantidadeItem));

        Button btnRemover = new Button("Remover (Del)");
        btnRemover.getStyleClass().add("button-danger");
        btnRemover.setOnAction(e -> removerSelecionado(true));

        HBox acoes = new HBox(8, btnAumentar, btnDiminuir, btnQuantidade, btnRemover);
        acoes.setAlignment(Pos.CENTER_LEFT);

        VBox vbox = new VBox(8, acoes, tabela);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        vbox.getStyleClass().add("card");
        vbox.setPadding(new Insets(10));
        vbox.setPrefWidth(720);
        return vbox;
    }

    private Node areaResumo() {
        Label titulo = new Label("Resumo da Compra");
        titulo.getStyleClass().add("section-title");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.add(new Label("Itens:"), 0, 0);
        grid.add(lblQtdItens, 1, 0);
        grid.add(new Label("Subtotal:"), 0, 1);
        grid.add(lblSubtotal, 1, 1);
        grid.add(new Label("Descontos:"), 0, 2);
        grid.add(lblDescontos, 1, 2);
        grid.add(new Label("Total:"), 0, 3);
        lblTotal.getStyleClass().add("pdv-total");
        grid.add(lblTotal, 1, 3);
        GridPane.setHalignment(lblTotal, javafx.geometry.HPos.RIGHT);

        Button btnLimpar = new Button("Limpar venda");
        btnLimpar.getStyleClass().add("button-danger");
        btnLimpar.setMaxWidth(Double.MAX_VALUE);
        btnLimpar.setOnAction(e -> limparVenda());

        btnPagamento.getStyleClass().add("button-primary");
        btnPagamento.setMaxWidth(Double.MAX_VALUE);
        btnPagamento.setStyle("-fx-font-size: 16; -fx-padding: 14;");
        btnPagamento.setOnAction(e -> abrirPagamento());

        Region espacador = new Region();
        VBox.setVgrow(espacador, Priority.ALWAYS);
        VBox resumo = new VBox(12, titulo, new Separator(), grid,
                espacador, btnLimpar, btnPagamento);
        resumo.getStyleClass().add("card");
        resumo.setPadding(new Insets(14));
        resumo.setPrefWidth(280);
        resumo.setMinWidth(260);
        return resumo;
    }

    private void configurarColunas() {
        TableColumn<ItemCarrinho, String> colCodigo =
                coluna("Código", i -> texto(i.getCodigo()), 80);
        TableColumn<ItemCarrinho, String> colNome =
                coluna("Produto", ItemCarrinho::getNome, 190);
        TableColumn<ItemCarrinho, String> colQtd =
                coluna("Qtd", i -> Dinheiro.formatarQuantidade(i.getQuantidade()), 60);
        colQtd.setStyle("-fx-alignment: CENTER-RIGHT;");
        TableColumn<ItemCarrinho, String> colUnidade =
                coluna("Un.", i -> i.getUnidadeMedida() != null ? i.getUnidadeMedida().getCodigo() : "", 50);
        TableColumn<ItemCarrinho, String> colPreco =
                coluna("Preço Unit.", i -> Dinheiro.formatar(i.getPrecoUnitario()), 90);
        TableColumn<ItemCarrinho, String> colDesconto =
                coluna("Desconto", i -> Dinheiro.formatar(i.getDesconto()), 80);
        TableColumn<ItemCarrinho, String> colTotal =
                coluna("Total", i -> Dinheiro.formatar(i.getTotal()), 90);

        TableColumn<ItemCarrinho, Void> colAcoes = new TableColumn<>("Ações");
        colAcoes.setMinWidth(150);
        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnMais = new Button("+");
            private final Button btnMenos = new Button("−");
            private final Button btnRemover = new Button("X");
            private final HBox pane = new HBox(4, btnMais, btnMenos, btnRemover);

            {
                btnMais.getStyleClass().add("button-secondary");
                btnMenos.getStyleClass().add("button-secondary");
                btnRemover.getStyleClass().add("button-danger");
                btnMais.setStyle("-fx-font-size: 11; -fx-padding: 2 8 2 8;");
                btnMenos.setStyle("-fx-font-size: 11; -fx-padding: 2 8 2 8;");
                btnRemover.setStyle("-fx-font-size: 11; -fx-padding: 2 8 2 8;");
                btnMais.setOnAction(e -> aumentarItem(itemDaLinha()));
                btnMenos.setOnAction(e -> diminuirItem(itemDaLinha()));
                btnRemover.setOnAction(e -> {
                    selecionar(itemDaLinha());
                    removerSelecionado(true);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }

            private ItemCarrinho itemDaLinha() {
                TableView<ItemCarrinho> tabela = getTableView();
                int indice = getIndex();
                if (tabela == null || indice < 0 || indice >= tabela.getItems().size()) {
                    return null;
                }
                return tabela.getItems().get(indice);
            }
        });

        tabela.getColumns().addAll(colCodigo, colNome, colQtd, colUnidade, colPreco, colDesconto, colTotal, colAcoes);

        // Delete remove o item selecionado, porém apenas quando a tabela está focada,
        // evitando interferir na edição de texto em outras telas.
        tabela.setOnKeyPressed(evento -> {
            if (evento.getCode() == KeyCode.DELETE && selecionado() != null) {
                removerSelecionado(true);
                evento.consume();
            }
        });
    }

    private TableColumn<ItemCarrinho, String> coluna(String titulo, Function<ItemCarrinho, String> extrator, double min) {
        TableColumn<ItemCarrinho, String> coluna = new TableColumn<>(titulo);
        coluna.setCellValueFactory(cell -> new SimpleStringProperty(extrator.apply(cell.getValue())));
        coluna.setMinWidth(min);
        return coluna;
    }

    private void configurarLeituraCodigo() {
        txtCodigo.setOnAction(e -> adicionarPorCodigo());
        txtCodigo.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            // Leitores USB podem enviar Tab em vez de Enter: tratamos ambos.
            if (e.getCode() == KeyCode.TAB && !txtCodigo.getText().isBlank()) {
                adicionarPorCodigo();
                e.consume();
            }
        });
        // Leitores que não enviam caractere de finalização: adiciona após pausa.
        pausaCodigo = new PauseTransition(Duration.millis(400));
        pausaCodigo.setOnFinished(e -> tentarAutoAdicionar(txtCodigo.getText()));
        txtCodigo.textProperty().addListener((obs, antigo, novo) -> {
            if (novo != null && !novo.isBlank()) {
                pausaCodigo.playFromStart();
            }
        });
    }

    private void iniciarRelogio() {
        lblDataHora.setText(LocalDateTime.now().format(FORMATO_DATA_HORA));
        Timeline relogio = new Timeline(new KeyFrame(Duration.seconds(1),
                e -> lblDataHora.setText(LocalDateTime.now().format(FORMATO_DATA_HORA))));
        relogio.setCycleCount(Timeline.INDEFINITE);
        relogio.play();
    }

    // ------------------------------------------------------------------
    // Sessão de caixa
    // ------------------------------------------------------------------

    private void carregarCaixas() {
        TarefaAssincrona.executar(
                () -> cashRegisterService.listarAtivos(),
                caixas -> {
                    Long selecionado = cmbCaixa.getValue() == null ? null : cmbCaixa.getValue().getId();
                    cmbCaixa.setItems(FXCollections.observableArrayList(caixas));
                    if (caixas.isEmpty()) {
                        lblSessaoStatus.setText("Sem caixas");
                        lblSessaoStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                        return;
                    }
                    CashRegister alvo = caixas.stream()
                            .filter(c -> c.getId().equals(selecionado))
                            .findFirst()
                            .orElse(caixas.get(0));
                    cmbCaixa.setValue(alvo);
                    atualizarSessao();
                },
                e -> lblMensagem.setText("Não foi possível carregar os caixas."));
    }

    private void atualizarSessao() {
        CashRegister caixa = cmbCaixa.getValue();
        if (caixa == null) {
            lblSessaoStatus.setText("Selecione caixa");
            lblSessaoStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            return;
        }
        Long caixaId = caixa.getId();
        TarefaAssincrona.executar(
                () -> cashSessionService.buscarSessaoAberta(caixaId),
                sessao -> {
                    if (sessao.isPresent()) {
                        CashSession s = sessao.get();
                        lblSessaoStatus.setText("Aberto (" + s.getDataAbertura().format(FORMATO_DATA_HORA) + ")");
                        lblSessaoStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else {
                        lblSessaoStatus.setText("Fechado");
                        lblSessaoStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                },
                e -> lblSessaoStatus.setText("Erro"));
    }

    // ------------------------------------------------------------------
    // Atalhos e ciclo de exibição
    // ------------------------------------------------------------------

    /** Registra os atalhos de teclado na cena principal. */
    public void registrarAtalhos(Scene scene) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F2), () -> {
            if (estaVisivel()) {
                txtBuscaNome.requestFocus();
                txtBuscaNome.selectAll();
            }
        });
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F4), () -> {
            if (estaVisivel()) {
                abrirPagamento();
            }
        });
    }

    private boolean estaVisivel() {
        return getParent() != null && isVisible();
    }

    /** Chamado ao exibir a tela: atualiza valores e devolve o foco ao código. */
    public void aoExibir() {
        carregarCaixas();
        atualizarTabela();
        atualizarResumo();
        Platform.runLater(txtCodigo::requestFocus);
    }

    // ------------------------------------------------------------------
    // Operações do carrinho
    // ------------------------------------------------------------------

    private void adicionarPorCodigo() {
        String codigo = txtCodigo.getText();
        if (codigo == null || codigo.isBlank()) {
            txtCodigo.requestFocus();
            return;
        }
        if (pausaCodigo != null) {
            pausaCodigo.stop();
        }
        buscarEAdicionar(codigo.trim(), true);
    }

    private void tentarAutoAdicionar(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return;
        }
        buscarEAdicionar(codigo.trim(), false);
    }

    private void buscarEAdicionar(String codigo, boolean informarErro) {
        TarefaAssincrona.executar(
                () -> productService.buscarPorCodigo(codigo),
                resultado -> {
                    if (resultado.isPresent()) {
                        adicionarProduto(resultado.get());
                        limparCampoCodigo();
                    } else if (informarErro) {
                        mostrarAviso("Produto não encontrado para o código: " + codigo);
                        limparCampoCodigo();
                    }
                },
                e -> {
                    if (informarErro) {
                        tratarErro("Erro ao buscar produto", e);
                    } else {
                        log.warn("Falha ao buscar produto por código: {}", e.getMessage());
                    }
                });
    }

    private void adicionarProduto(Product produto) {
        try {
            carrinho.adicionar(produto, BigDecimal.ONE, estoqueDisponivel(produto));
            atualizarTabela();
            atualizarResumo();
            definirMensagem("Adicionado: " + produto.getNome());
        } catch (IllegalArgumentException e) {
            mostrarAviso(e.getMessage());
        }
    }

    private void buscarPorNome() {
        Product escolhido = SeletorProdutoDialog.mostrar(janela(), productService, txtBuscaNome.getText());
        if (escolhido != null) {
            adicionarProduto(escolhido);
        }
    }

    private void aumentarItem(ItemCarrinho item) {
        if (item == null) {
            return;
        }
        comEstoque(item, disponivel -> {
            try {
                carrinho.aumentar(item.getProdutoId(), BigDecimal.ONE, disponivel);
                atualizarTabela();
                atualizarResumo();
            } catch (IllegalArgumentException e) {
                mostrarAviso(e.getMessage());
            }
        });
    }

    private void diminuirItem(ItemCarrinho item) {
        if (item == null) {
            return;
        }
        try {
            carrinho.diminuir(item.getProdutoId(), BigDecimal.ONE);
            atualizarTabela();
            atualizarResumo();
        } catch (IllegalArgumentException e) {
            mostrarAviso(e.getMessage());
        }
    }

    private void alterarQuantidadeItem(ItemCarrinho item) {
        if (item == null) {
            return;
        }
        TextInputDialog dialog = new TextInputDialog(Dinheiro.formatarQuantidade(item.getQuantidade()));
        dialog.setTitle("Alterar quantidade");
        dialog.setHeaderText("Nova quantidade para \"" + item.getNome() + "\"");
        dialog.setContentText("Quantidade:");
        if (janela() != null) {
            dialog.initOwner(janela());
        }
        Optional<String> resposta = dialog.showAndWait();
        if (resposta.isEmpty()) {
            return;
        }
        BigDecimal nova;
        try {
            nova = parseQuantidade(resposta.get());
        } catch (NumberFormatException e) {
            mostrarAviso("Quantidade inválida");
            return;
        }
        comEstoque(item, disponivel -> {
            try {
                carrinho.alterarQuantidade(item.getProdutoId(), nova, disponivel);
                atualizarTabela();
                atualizarResumo();
            } catch (IllegalArgumentException e) {
                mostrarAviso(e.getMessage());
            }
        });
    }

    private void removerSelecionado(boolean confirmar) {
        ItemCarrinho item = selecionado();
        if (item == null) {
            if (confirmar) {
                mostrarAviso("Selecione um item para remover.");
            }
            return;
        }
        if (confirmar && !confirmar("Remover item",
                "Remover \"" + item.getNome() + "\" do carrinho?")) {
            return;
        }
        carrinho.remover(item.getProdutoId());
        atualizarTabela();
        atualizarResumo();
    }

    private void limparVenda() {
        if (carrinho.isVazio()) {
            return;
        }
        if (!confirmar("Limpar venda", "Deseja remover todos os itens do carrinho?")) {
            return;
        }
        carrinho.limpar();
        atualizarTabela();
        atualizarResumo();
        definirMensagem("Venda atual limpa.");
    }

    // ------------------------------------------------------------------
    // Pagamento e finalização
    // ------------------------------------------------------------------

    private void abrirPagamento() {
        if (finalizando) {
            return;
        }
        if (carrinho.isVazio()) {
            mostrarAviso("Adicione produtos antes de finalizar a venda.");
            return;
        }
        List<PagamentoDTO> pagamentos = PagamentoDialog.mostrar(janela(), carrinho);
        if (pagamentos == null) {
            return;
        }
        finalizar(pagamentos);
    }

    private void finalizar(List<PagamentoDTO> pagamentos) {
        finalizando = true;
        btnPagamento.setDisable(true);
        definirMensagem("Finalizando venda...");
        String operador = txtOperador.getText();

        CashRegister caixa = cmbCaixa.getValue();
        if (caixa == null) {
            mostrarAviso("Selecione um caixa antes de finalizar a venda.");
            finalizando = false;
            btnPagamento.setDisable(false);
            return;
        }

        Long caixaId = caixa.getId();
        TarefaAssincrona.executar(
                () -> cashSessionService.buscarSessaoAberta(caixaId),
                sessaoOpt -> {
                    if (sessaoOpt.isEmpty()) {
                        Platform.runLater(() -> {
                            mostrarAviso("Não há sessão aberta no caixa " + caixa.getNome() + ". Abra o caixa antes de finalizar a venda.");
                            finalizando = false;
                            btnPagamento.setDisable(false);
                        });
                        return;
                    }
                    CashSession sessao = sessaoOpt.get();
                    TarefaAssincrona.executar(
                            () -> saleService.finalizarVenda(carrinho, pagamentos, operador, sessao),
                            detalhe -> {
                                finalizando = false;
                                btnPagamento.setDisable(false);
                                carrinho.limpar();
                                atualizarTabela();
                                atualizarResumo();
                                definirMensagem("Venda " + detalhe.numero() + " finalizada — Total "
                                        + Dinheiro.formatar(detalhe.valorTotal()));
                                ComprovanteDialog.mostrar(janela(), detalhe);
                                atualizarSessao();
                                Platform.runLater(txtCodigo::requestFocus);
                            },
                            e -> {
                                finalizando = false;
                                btnPagamento.setDisable(false);
                                definirMensagem("Falha ao finalizar a venda.");
                                tratarErro("Não foi possível finalizar a venda", e);
                            });
                },
                e -> {
                    Platform.runLater(() -> {
                        finalizando = false;
                        btnPagamento.setDisable(false);
                        definirMensagem("Falha ao verificar sessão de caixa.");
                        tratarErro("Erro ao buscar sessão de caixa", e);
                    });
                });
    }

    // ------------------------------------------------------------------
    // Atualização de estado
    // ------------------------------------------------------------------

    private void atualizarTabela() {
        dados.setAll(carrinho.getItens());
    }

    private void atualizarResumo() {
        lblQtdItens.setText(Dinheiro.formatarQuantidade(carrinho.getQuantidadeTotalItens()));
        lblSubtotal.setText(Dinheiro.formatar(carrinho.getSubtotal()));
        lblDescontos.setText(Dinheiro.formatar(carrinho.getDescontoTotal()));
        lblTotal.setText(Dinheiro.formatar(carrinho.getTotal()));
    }

    private void limparCampoCodigo() {
        txtCodigo.clear();
        Platform.runLater(txtCodigo::requestFocus);
    }

    private void selecionar(ItemCarrinho item) {
        if (item != null) {
            tabela.getSelectionModel().select(item);
        }
    }

    private ItemCarrinho selecionado() {
        return tabela.getSelectionModel().getSelectedItem();
    }

    private void comItemSelecionado(Consumer<ItemCarrinho> acao) {
        ItemCarrinho item = selecionado();
        if (item == null) {
            mostrarAviso("Selecione um item.");
            return;
        }
        acao.accept(item);
    }

    private void comEstoque(ItemCarrinho item, Consumer<BigDecimal> acao) {
        if (item == null) {
            return;
        }
        TarefaAssincrona.executar(
                () -> productService.buscarPorId(item.getProdutoId()),
                opt -> acao.accept(opt.map(p -> Dinheiro.ouZero(p.getEstoqueAtual())).orElse(BigDecimal.ZERO)),
                e -> tratarErro("Erro ao consultar estoque", e));
    }

    private BigDecimal estoqueDisponivel(Product produto) {
        return produto.getEstoqueAtual() != null ? produto.getEstoqueAtual() : BigDecimal.ZERO;
    }

    private BigDecimal parseQuantidade(String texto) {
        return new BigDecimal(texto.trim().replace(",", "."));
    }

    private String texto(String valor) {
        return valor == null ? "" : valor;
    }

    private javafx.stage.Window janela() {
        return getScene() == null ? null : getScene().getWindow();
    }

    private void definirMensagem(String mensagem) {
        lblMensagem.setText(mensagem);
    }

    // ------------------------------------------------------------------
    // Diálogos
    // ------------------------------------------------------------------

    private void mostrarAviso(String mensagem) {
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
            mostrarAviso(e.getMessage());
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
}
