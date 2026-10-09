package com.fasteasy.presentation.view;

import com.fasteasy.domain.Dinheiro;
import com.fasteasy.service.dto.ItemVendaDTO;
import com.fasteasy.service.dto.PagamentoVendaDTO;
import com.fasteasy.service.dto.VendaDetalhe;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.time.format.DateTimeFormatter;

/**
 * Comprovante de venda (não fiscal).
 *
 * <p>Permite visualizar e imprimir o comprovante pela impressora padrão do
 * sistema JavaFX. Se a impressão falhar, a venda permanece registrada e o
 * comprovante pode ser reaberto pelo histórico.</p>
 */
public final class ComprovanteDialog {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private ComprovanteDialog() {
    }

    /** Exibe o comprovante de uma venda. */
    public static void mostrar(Window owner, VendaDetalhe venda) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Comprovante da venda " + venda.numero());
        if (owner != null) {
            dialog.initOwner(owner);
        }

        ButtonType imprimir = new ButtonType("Imprimir", ButtonBar.ButtonData.LEFT);
        ButtonType fechar = new ButtonType("Fechar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(imprimir, fechar);

        Node comprovante = construirComprovante(venda);
        VBox conteudo = new VBox(comprovante);
        conteudo.setPadding(new Insets(14));
        conteudo.setAlignment(Pos.TOP_CENTER);
        dialog.getDialogPane().setContent(conteudo);

        Node botaoImprimir = dialog.getDialogPane().lookupButton(imprimir);
        botaoImprimir.addEventFilter(javafx.event.ActionEvent.ACTION, evento -> {
            evento.consume();
            imprimir(dialog, comprovante, venda);
        });

        dialog.showAndWait();
    }

    private static Node construirComprovante(VendaDetalhe venda) {
        VBox box = new VBox(2);
        box.setPrefWidth(360);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: white; -fx-border-color: #cccccc;");

        Label titulo = new Label("FastEasy");
        titulo.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        Label subtitulo = new Label("Comprovante de venda — documento não fiscal");
        subtitulo.setStyle("-fx-font-size: 10;");
        Label numero = new Label("Venda nº: " + venda.numero());
        Label data = new Label("Data/Hora: " + (venda.dataHora() == null ? "" : venda.dataHora().format(FORMATO)));
        String operador = venda.operador() == null ? "Não identificado" : venda.operador();
        Label lblOperador = new Label("Operador: " + operador);

        box.getChildren().addAll(titulo, subtitulo, new Separator(), numero, data, lblOperador, new Separator());

        for (ItemVendaDTO item : venda.itens()) {
            Label linhaItem = new Label(String.format("%s x %s",
                    Dinheiro.formatarQuantidade(item.quantidade()), item.nomeProduto()));
            Label linhaValores = new Label(String.format("   Unit.: %s   Total: %s",
                    Dinheiro.formatar(item.precoUnitario()), Dinheiro.formatar(item.total())));
            box.getChildren().addAll(linhaItem, linhaValores);
            if (item.desconto() != null && item.desconto().signum() > 0) {
                box.getChildren().add(new Label("   Desconto: " + Dinheiro.formatar(item.desconto())));
            }
        }

        box.getChildren().add(new Separator());
        box.getChildren().add(new Label("Subtotal: " + Dinheiro.formatar(venda.subtotal())));
        box.getChildren().add(new Label("Descontos: " + Dinheiro.formatar(venda.descontoTotal())));
        Label total = new Label("TOTAL: " + Dinheiro.formatar(venda.valorTotal()));
        total.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
        box.getChildren().add(total);

        box.getChildren().add(new Separator());
        box.getChildren().add(new Label("Pagamentos:"));
        for (PagamentoVendaDTO pagamento : venda.pagamentos()) {
            String texto = pagamento.formaPagamento().getDescricao() + ": " + Dinheiro.formatar(pagamento.valor());
            box.getChildren().add(new Label("   " + texto));
            if (pagamento.troco() != null && pagamento.troco().signum() > 0) {
                box.getChildren().add(new Label("   Recebido: " + Dinheiro.formatar(pagamento.valorRecebido())
                        + "   Troco: " + Dinheiro.formatar(pagamento.troco())));
            }
        }

        box.getChildren().add(new Separator());
        box.getChildren().add(new Label("Valor recebido: " + Dinheiro.formatar(venda.valorRecebido())));
        box.getChildren().add(new Label("Troco: " + Dinheiro.formatar(venda.troco())));
        box.getChildren().add(new Separator());
        Label obrigado = new Label("Obrigado e volte sempre!");
        obrigado.setAlignment(Pos.CENTER);
        box.getChildren().add(obrigado);
        return box;
    }

    private static void imprimir(Dialog<Void> dialog, Node comprovante, VendaDetalhe venda) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            aviso(dialog, "Nenhuma impressora disponível. A venda permanece registrada.");
            return;
        }
        Window owner = dialog.getDialogPane().getScene() == null
                ? null : dialog.getDialogPane().getScene().getWindow();
        if (!job.showPrintDialog(owner)) {
            job.endJob();
            return;
        }
        boolean impresso = job.printPage(comprovante);
        if (impresso) {
            job.endJob();
        } else {
            job.endJob();
            aviso(dialog, "Não foi possível imprimir o comprovante. A venda permanece registrada.");
        }
    }

    private static void aviso(Dialog<?> dialog, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Impressão");
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        if (dialog.getDialogPane().getScene() != null) {
            alert.initOwner(dialog.getDialogPane().getScene().getWindow());
        }
        alert.showAndWait();
    }
}
