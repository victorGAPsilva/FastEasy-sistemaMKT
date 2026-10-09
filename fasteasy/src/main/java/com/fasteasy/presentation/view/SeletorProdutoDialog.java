package com.fasteasy.presentation.view;

import com.fasteasy.domain.Dinheiro;
import com.fasteasy.domain.Product;
import com.fasteasy.presentation.util.TarefaAssincrona;
import com.fasteasy.service.ProductService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Diálogo de busca de produtos por nome, SKU ou código de barras.
 *
 * <p>Retorna o produto escolhido para ser adicionado ao carrinho do PDV.
 * As consultas são executadas fora da thread da interface.</p>
 */
public final class SeletorProdutoDialog {

    private SeletorProdutoDialog() {
    }

    /**
     * Exibe o seletor de produtos.
     *
     * @return o produto escolhido ou {@code null} se o operador cancelar
     */
    public static Product mostrar(Window owner, ProductService productService, String termoInicial) {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("Buscar produto");
        dialog.setHeaderText("Pesquise por nome, SKU ou código de barras");
        if (owner != null) {
            dialog.initOwner(owner);
        }

        ButtonType selecionar = new ButtonType("Adicionar ao carrinho", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(selecionar, ButtonType.CANCEL);

        TextField campo = new TextField(termoInicial == null ? "" : termoInicial);
        campo.setPromptText("Nome, SKU ou código de barras");

        ObservableList<Product> dados = FXCollections.observableArrayList();
        TableView<Product> tabela = new TableView<>(dados);

        TableColumn<Product, String> colCodigo = new TableColumn<>("Código/SKU");
        colCodigo.setCellValueFactory(c -> new SimpleStringProperty(codigoDe(c.getValue())));
        colCodigo.setMinWidth(120);

        TableColumn<Product, String> colNome = new TableColumn<>("Produto");
        colNome.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNome()));
        colNome.setMinWidth(220);

        TableColumn<Product, String> colPreco = new TableColumn<>("Preço");
        colPreco.setCellValueFactory(c -> new SimpleStringProperty(Dinheiro.formatar(c.getValue().getPrecoVenda())));
        colPreco.setMinWidth(90);

        TableColumn<Product, String> colEstoque = new TableColumn<>("Estoque");
        colEstoque.setCellValueFactory(c -> new SimpleStringProperty(
                Dinheiro.formatarQuantidade(c.getValue().getEstoqueAtual())));
        colEstoque.setMinWidth(80);

        tabela.getColumns().addAll(colCodigo, colNome, colPreco, colEstoque);
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabela.setPrefHeight(320);
        tabela.setPlaceholder(new Label("Nenhum produto encontrado"));

        Runnable pesquisar = () -> {
            String termo = campo.getText() == null ? "" : campo.getText().trim();
            TarefaAssincrona.executar(
                    () -> productService.pesquisar(termo.isEmpty() ? null : termo, "ATIVO", null),
                    dados::setAll,
                    e -> dados.clear());
        };
        campo.setOnAction(e -> pesquisar.run());
        campo.textProperty().addListener((obs, antigo, novo) -> pesquisar.run());

        VBox conteudo = new VBox(10, campo, tabela);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        conteudo.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(conteudo);

        Node botaoSelecionar = dialog.getDialogPane().lookupButton(selecionar);
        botaoSelecionar.setDisable(true);
        tabela.getSelectionModel().selectedItemProperty().addListener(
                (obs, antigo, novo) -> botaoSelecionar.setDisable(novo == null));

        dialog.setResultConverter(botao -> botao == selecionar
                ? tabela.getSelectionModel().getSelectedItem()
                : null);

        tabela.setRowFactory(tv -> {
            TableRow<Product> linha = new TableRow<>();
            linha.setOnMouseClicked(evento -> {
                if (evento.getClickCount() == 2 && !linha.isEmpty()) {
                    dialog.setResult(linha.getItem());
                    dialog.close();
                }
            });
            return linha;
        });

        dialog.setOnShown(e -> {
            pesquisar.run();
            campo.requestFocus();
        });

        return dialog.showAndWait().orElse(null);
    }

    private static String codigoDe(Product produto) {
        if (produto.getCodigoBarras() != null && !produto.getCodigoBarras().isBlank()) {
            return produto.getCodigoBarras();
        }
        return produto.getSku() == null ? "" : produto.getSku();
    }
}
