package com.fasteasy.presentation.view;

import com.fasteasy.domain.Dinheiro;
import com.fasteasy.domain.TipoMovimentacaoCaixa;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.math.BigDecimal;

/**
 * Diálogo para registrar uma movimentação financeira manual do caixa
 * (suprimento, sangria ou ajuste autorizado).
 *
 * <p>Apenas coleta os dados; a validação de negócio e a persistência ficam no
 * {@code CashSessionService}.</p>
 */
public final class MovimentoCaixaDialog {

    /** Dados coletados no diálogo. */
    public record MovimentoInput(TipoMovimentacaoCaixa tipo, BigDecimal valor, String motivo) {
    }

    private MovimentoCaixaDialog() {
    }

    /**
     * Exibe o diálogo para o tipo informado.
     *
     * @param caixaNome nome do caixa, exibido no cabeçalho
     * @return os dados informados ou {@code null} se cancelado
     */
    public static MovimentoInput mostrar(Window owner, TipoMovimentacaoCaixa tipo, String caixaNome) {
        Dialog<MovimentoInput> dialog = new Dialog<>();
        dialog.setTitle(tipo.getDescricao());
        dialog.setHeaderText(tipo.getDescricao() + " — " + caixaNome);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        ButtonType confirmar = new ButtonType("Registrar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmar, cancelar);

        TextField campoValor = new TextField();
        campoValor.setPromptText("0,00");
        campoValor.setPrefWidth(160);

        TextField campoMotivo = new TextField();
        campoMotivo.setPromptText("Motivo (obrigatório)");
        campoMotivo.setPrefWidth(280);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.add(new Label("Tipo:"), 0, 0);
        form.add(new Label(tipo.getDescricao()), 1, 0);
        form.add(new Label("Valor:"), 0, 1);
        form.add(campoValor, 1, 1);
        form.add(new Label("Motivo:"), 0, 2);
        form.add(campoMotivo, 1, 2);

        Label aviso = new Label("O motivo é obrigatório e a movimentação não poderá ser alterada depois.");
        aviso.setWrapText(true);

        VBox conteudo = new VBox(10, form, aviso);
        conteudo.setPadding(new Insets(12));
        conteudo.setPrefWidth(420);
        dialog.getDialogPane().setContent(conteudo);

        Node botaoConfirmar = dialog.getDialogPane().lookupButton(confirmar);
        botaoConfirmar.addEventFilter(ActionEvent.ACTION, evento -> {
            BigDecimal valor = parseMoeda(campoValor.getText());
            String motivo = campoMotivo.getText() == null ? "" : campoMotivo.getText().trim();
            if (valor == null || valor.signum() <= 0) {
                aviso(dialog, "Informe um valor maior que zero.");
                evento.consume();
                return;
            }
            if (motivo.isEmpty()) {
                aviso(dialog, "O motivo é obrigatório.");
                evento.consume();
            }
        });

        dialog.setResultConverter(botao -> {
            if (botao != confirmar) {
                return null;
            }
            BigDecimal valor = parseMoeda(campoValor.getText());
            String motivo = campoMotivo.getText() == null ? "" : campoMotivo.getText().trim();
            return new MovimentoInput(tipo, valor, motivo);
        });

        Platform.runLater(campoValor::requestFocus);
        return dialog.showAndWait().orElse(null);
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

    private static void aviso(Dialog<?> dialog, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Atenção");
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        if (dialog.getDialogPane().getScene() != null) {
            alert.initOwner(dialog.getDialogPane().getScene().getWindow());
        }
        alert.showAndWait();
    }
}
