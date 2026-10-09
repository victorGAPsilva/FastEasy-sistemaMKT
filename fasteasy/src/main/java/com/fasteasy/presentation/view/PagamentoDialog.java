package com.fasteasy.presentation.view;

import com.fasteasy.domain.CarrinhoVenda;
import com.fasteasy.domain.Dinheiro;
import com.fasteasy.domain.FormaPagamento;
import com.fasteasy.service.dto.PagamentoDTO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Diálogo de pagamento do PDV.
 *
 * <p>Permite registrar uma ou mais formas de pagamento (pagamento combinado).
 * A soma dos valores aplicados deve ser exatamente igual ao total da venda.
 * Em dinheiro, o valor recebido pode ser maior que o aplicado, gerando
 * troco. Não há confirmação externa de transação.</p>
 */
public final class PagamentoDialog {

    private PagamentoDialog() {
    }

    /**
     * Exibe o diálogo de pagamento.
     *
     * @return a lista de pagamentos informada ou {@code null} se o operador voltar
     */
    public static List<PagamentoDTO> mostrar(Window owner, CarrinhoVenda carrinho) {
        BigDecimal total = carrinho.getTotal();

        Dialog<List<PagamentoDTO>> dialog = new Dialog<>();
        dialog.setTitle("Pagamento");
        dialog.setHeaderText("Pagamento da venda");
        if (owner != null) {
            dialog.initOwner(owner);
        }

        ButtonType confirmar = new ButtonType("Confirmar venda", ButtonBar.ButtonData.OK_DONE);
        ButtonType voltar = new ButtonType("Voltar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmar, voltar);

        ObservableList<PagamentoDTO> pagamentos = FXCollections.observableArrayList();

        Label lblTotal = new Label("Total a pagar: " + Dinheiro.formatar(total));
        lblTotal.getStyleClass().add("pdv-total");

        TableView<PagamentoDTO> tabela = new TableView<>(pagamentos);
        tabela.setPrefHeight(140);
        tabela.setPlaceholder(new Label("Nenhum pagamento informado"));

        TableColumn<PagamentoDTO, String> colForma = new TableColumn<>("Forma");
        colForma.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().formaPagamento().getDescricao()));
        colForma.setMinWidth(90);

        TableColumn<PagamentoDTO, String> colValor = new TableColumn<>("Aplicado");
        colValor.setCellValueFactory(c -> new SimpleStringProperty(Dinheiro.formatar(c.getValue().valor())));
        colValor.setMinWidth(90);

        TableColumn<PagamentoDTO, String> colRecebido = new TableColumn<>("Recebido");
        colRecebido.setCellValueFactory(c -> new SimpleStringProperty(Dinheiro.formatar(c.getValue().valorRecebido())));
        colRecebido.setMinWidth(90);

        TableColumn<PagamentoDTO, String> colTroco = new TableColumn<>("Troco");
        colTroco.setCellValueFactory(c -> new SimpleStringProperty(trocoDe(c.getValue())));
        colTroco.setMinWidth(80);

        tabela.getColumns().addAll(colForma, colValor, colRecebido, colTroco);
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        ComboBox<FormaPagamento> forma = new ComboBox<>(FXCollections.observableArrayList(FormaPagamento.values()));
        forma.setValue(FormaPagamento.DINHEIRO);
        forma.setMaxWidth(Double.MAX_VALUE);

        TextField txtValor = new TextField();
        txtValor.setPromptText("0,00");

        TextField txtRecebido = new TextField();
        txtRecebido.setPromptText("0,00");

        Button btnAdicionar = new Button("Adicionar pagamento");
        btnAdicionar.getStyleClass().add("button-secondary");

        Button btnRemoverPagamento = new Button("Remover selecionado");
        btnRemoverPagamento.getStyleClass().add("button-danger");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.add(new Label("Forma:"), 0, 0);
        grid.add(forma, 1, 0);
        grid.add(new Label("Valor aplicado:"), 0, 1);
        grid.add(txtValor, 1, 1);
        grid.add(new Label("Valor recebido:"), 0, 2);
        grid.add(txtRecebido, 1, 2);
        GridPane.setColumnSpan(btnAdicionar, 2);
        grid.add(btnAdicionar, 0, 3);

        Label lblRestante = new Label();
        Label lblTroco = new Label();
        lblRestante.getStyleClass().add("pdv-total");
        lblTroco.getStyleClass().add("pdv-total");

        HBox rodape = new HBox(30,
                new Label("Falta:"), lblRestante,
                new Label("Troco:"), lblTroco);
        rodape.setAlignment(Pos.CENTER_LEFT);

        VBox conteudo = new VBox(12, lblTotal, tabela, btnRemoverPagamento, grid, rodape);
        conteudo.setPadding(new Insets(12));
        conteudo.setPrefWidth(520);
        dialog.getDialogPane().setContent(conteudo);

        Node botaoConfirmar = dialog.getDialogPane().lookupButton(confirmar);

        Runnable atualizar = () -> {
            BigDecimal soma = somar(pagamentos);
            BigDecimal restante = total.subtract(soma);
            if (restante.signum() < 0) {
                restante = BigDecimal.ZERO;
            }
            lblRestante.setText(Dinheiro.formatar(restante));
            lblTroco.setText(Dinheiro.formatar(trocoTotal(pagamentos)));
            botaoConfirmar.setDisable(restante.signum() != 0 || pagamentos.isEmpty());
        };

        forma.valueProperty().addListener((obs, antigo, novo) -> {
            boolean dinheiro = novo == FormaPagamento.DINHEIRO;
            txtRecebido.setDisable(!dinheiro);
            if (!dinheiro) {
                txtRecebido.clear();
            }
        });
        txtRecebido.setDisable(false);

        btnAdicionar.setOnAction(e -> {
            FormaPagamento formaEscolhida = forma.getValue();
            if (formaEscolhida == null) {
                aviso(dialog, "Selecione a forma de pagamento.");
                return;
            }
            BigDecimal valor;
            try {
                valor = parseMoeda(txtValor.getText());
            } catch (NumberFormatException ex) {
                valor = null;
            }
            if (valor == null || valor.signum() <= 0) {
                aviso(dialog, "Informe um valor aplicado maior que zero.");
                return;
            }
            BigDecimal restante = total.subtract(somar(pagamentos));
            if (valor.compareTo(restante) > 0) {
                aviso(dialog, "O valor aplicado é maior que o valor restante ("
                        + Dinheiro.formatar(restante) + ").");
                return;
            }

            BigDecimal recebido;
            if (formaEscolhida == FormaPagamento.DINHEIRO) {
                try {
                    recebido = parseMoeda(txtRecebido.getText());
                } catch (NumberFormatException ex) {
                    recebido = null;
                }
                if (recebido == null || recebido.compareTo(valor) < 0) {
                    aviso(dialog, "O valor recebido em dinheiro não pode ser menor que o valor aplicado.");
                    return;
                }
            } else {
                recebido = valor;
            }

            pagamentos.add(new PagamentoDTO(formaEscolhida, Dinheiro.arredondar(valor), Dinheiro.arredondar(recebido)));
            txtValor.clear();
            txtRecebido.clear();
            forma.setValue(FormaPagamento.DINHEIRO);
            atualizar.run();
        });

        btnRemoverPagamento.setOnAction(e -> {
            PagamentoDTO selecionado = tabela.getSelectionModel().getSelectedItem();
            if (selecionado != null) {
                pagamentos.remove(selecionado);
                atualizar.run();
            }
        });

        dialog.setResultConverter(botao -> botao == confirmar ? new ArrayList<>(pagamentos) : null);
        atualizar.run();

        dialog.setOnShown(e -> txtValor.requestFocus());

        return dialog.showAndWait().orElse(null);
    }

    private static BigDecimal somar(List<PagamentoDTO> pagamentos) {
        return Dinheiro.arredondar(pagamentos.stream()
                .map(PagamentoDTO::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private static BigDecimal trocoTotal(List<PagamentoDTO> pagamentos) {
        BigDecimal troco = pagamentos.stream()
                .filter(p -> p.formaPagamento() == FormaPagamento.DINHEIRO)
                .map(p -> p.valorRecebido().subtract(p.valor()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Dinheiro.arredondar(troco);
    }

    private static String trocoDe(PagamentoDTO pagamento) {
        if (pagamento.formaPagamento() != FormaPagamento.DINHEIRO) {
            return Dinheiro.formatar(BigDecimal.ZERO);
        }
        return Dinheiro.formatar(pagamento.valorRecebido().subtract(pagamento.valor()));
    }

    private static BigDecimal parseMoeda(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String valor = texto.trim().replace("R$", "").replaceAll("\\s", "");
        if (valor.contains(",")) {
            valor = valor.replace(".", "").replace(",", ".");
        }
        return new BigDecimal(valor);
    }

    private static void aviso(Dialog<?> dialog, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Atenção");
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.initOwner(dialog.getDialogPane().getScene().getWindow());
        alert.showAndWait();
    }
}
