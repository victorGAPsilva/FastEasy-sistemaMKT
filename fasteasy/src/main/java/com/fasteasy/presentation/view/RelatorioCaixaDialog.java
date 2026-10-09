package com.fasteasy.presentation.view;

import com.fasteasy.domain.Dinheiro;
import com.fasteasy.domain.FormaPagamento;
import com.fasteasy.domain.StatusSessaoCaixa;
import com.fasteasy.service.dto.MovimentacaoCaixaDTO;
import com.fasteasy.service.dto.ResumoCaixaDTO;
import com.fasteasy.service.dto.SessaoCaixaDetalhe;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Relatório de abertura/fechamento de caixa (não fiscal).
 *
 * <p>Permite visualizar e imprimir o resumo da sessão e as movimentações. Se a
 * impressão falhar, os dados permanecem preservados no banco.</p>
 */
public final class RelatorioCaixaDialog {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private RelatorioCaixaDialog() {
    }

    /** Exibe o relatório de fechamento de uma sessão. */
    public static void mostrar(Window owner, SessaoCaixaDetalhe detalhe) {
        ResumoCaixaDTO resumo = detalhe.resumo();
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Relatório de caixa — " + texto(resumo.caixaNome()));
        if (owner != null) {
            dialog.initOwner(owner);
        }

        ButtonType imprimir = new ButtonType("Imprimir", ButtonBar.ButtonData.LEFT);
        ButtonType fechar = new ButtonType("Fechar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(imprimir, fechar);

        Node relatorio = construir(resumo, detalhe);
        ScrollPane scroll = new ScrollPane(relatorio);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(520);
        dialog.getDialogPane().setContent(scroll);

        Node botaoImprimir = dialog.getDialogPane().lookupButton(imprimir);
        botaoImprimir.addEventFilter(javafx.event.ActionEvent.ACTION, evento -> {
            evento.consume();
            imprimir(dialog, relatorio);
        });

        dialog.showAndWait();
    }

    private static Node construir(ResumoCaixaDTO r, SessaoCaixaDetalhe detalhe) {
        VBox box = new VBox(3);
        box.setPrefWidth(440);
        box.setPadding(new Insets(14));
        box.setStyle("-fx-background-color: white;");

        Label titulo = new Label("FastEasy");
        titulo.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        Label subtitulo = new Label("Relatório de caixa — documento não fiscal");
        subtitulo.setStyle("-fx-font-size: 10;");
        box.getChildren().addAll(titulo, subtitulo, new Separator());

        box.getChildren().add(linha("Caixa:", texto(r.caixaNome())));
        box.getChildren().add(linha("Operador (abertura):", texto(r.operador())));
        box.getChildren().add(linha("Operador (fechamento):", texto(r.operadorFechamento())));
        box.getChildren().add(linha("Status:", r.status() == null ? "—" : r.status().getDescricao()));
        box.getChildren().add(linha("Abertura:", formatar(r.dataAbertura())));
        box.getChildren().add(linha("Fechamento:", formatar(r.dataFechamento())));

        box.getChildren().add(new Separator());
        box.getChildren().add(secao("Dinheiro"));
        box.getChildren().add(linha("Saldo inicial:", Dinheiro.formatar(r.saldoInicial())));
        box.getChildren().add(linha("Pagamentos em dinheiro (entregue):", Dinheiro.formatar(r.totalDinheiroRecebido())));
        box.getChildren().add(linha("Trocos devolvidos:", Dinheiro.formatar(r.totalTroco())));
        box.getChildren().add(linha("Suprimentos:", Dinheiro.formatar(r.totalSuprimentos())));
        box.getChildren().add(linha("Outras entradas:", Dinheiro.formatar(r.totalOutrasEntradas())));
        box.getChildren().add(linha("Sangrias:", Dinheiro.formatar(r.totalSangrias())));
        box.getChildren().add(linha("Outras saídas:", Dinheiro.formatar(r.totalOutrasSaidas())));
        Label esperado = linha("Saldo esperado em dinheiro:", Dinheiro.formatar(r.saldoEsperado()));
        esperado.setStyle("-fx-font-weight: bold;");
        box.getChildren().add(esperado);

        box.getChildren().add(new Separator());
        box.getChildren().add(secao("Vendas por forma de pagamento"));
        for (FormaPagamento forma : FormaPagamento.values()) {
            box.getChildren().add(linha(forma.getDescricao() + ":", Dinheiro.formatar(r.totalPorForma(forma))));
        }
        box.getChildren().add(linha("Quantidade de vendas:", String.valueOf(r.quantidadeVendas())));
        box.getChildren().add(linha("Faturamento total:", Dinheiro.formatar(r.totalPorForma(FormaPagamento.DINHEIRO)
                .add(r.totalPorForma(FormaPagamento.PIX))
                .add(r.totalPorForma(FormaPagamento.DEBITO))
                .add(r.totalPorForma(FormaPagamento.CREDITO)))));

        box.getChildren().add(new Separator());
        box.getChildren().add(secao("Conferência"));
        box.getChildren().add(linha("Total contado:", Dinheiro.formatar(r.totalContado())));
        box.getChildren().add(linha("Diferença:", Dinheiro.formatar(r.diferenca())));
        box.getChildren().add(linha("Situação:", situacao(r.diferenca())));

        box.getChildren().add(new Separator());
        box.getChildren().add(secao("Movimentações"));
        if (detalhe.movimentacoes().isEmpty()) {
            box.getChildren().add(new Label("   Nenhuma movimentação registrada."));
        } else {
            for (MovimentacaoCaixaDTO m : detalhe.movimentacoes()) {
                box.getChildren().add(new Label(String.format("   %s  %s  %s  (%s)",
                        formatar(m.dataHora()),
                        m.tipo().getDescricao(),
                        Dinheiro.formatar(m.valor()),
                        m.motivo())));
            }
        }

        box.getChildren().add(new Separator());
        box.getChildren().add(linha("Obs. abertura:", texto(r.observacoesAbertura())));
        box.getChildren().add(linha("Obs. fechamento:", texto(r.observacoesFechamento())));
        return box;
    }

    private static Label secao(String titulo) {
        Label label = new Label(titulo);
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #1b5e20; -fx-padding: 4 0 2 0;");
        return label;
    }

    private static Label linha(String rotulo, String valor) {
        return new Label(rotulo + " " + valor);
    }

    private static String situacao(BigDecimal diferenca) {
        if (diferenca == null) {
            return "não conferido";
        }
        int sinal = diferenca.signum();
        if (sinal == 0) {
            return "caixa conferido";
        }
        return sinal > 0 ? "sobra de caixa" : "falta de caixa";
    }

    private static String formatar(java.time.LocalDateTime data) {
        return data == null ? "—" : data.format(FORMATO);
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? "—" : valor;
    }

    private static void imprimir(Dialog<Void> dialog, Node relatorio) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            aviso(dialog, "Nenhuma impressora disponível. O relatório permanece disponível.");
            return;
        }
        Window owner = dialog.getDialogPane().getScene() == null
                ? null : dialog.getDialogPane().getScene().getWindow();
        if (!job.showPrintDialog(owner)) {
            job.endJob();
            return;
        }
        boolean impresso = job.printPage(relatorio);
        job.endJob();
        if (!impresso) {
            aviso(dialog, "Não foi possível imprimir o relatório. Os dados permanecem registrados.");
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

    /** Constrói apenas o texto de situação para reuso (testável). */
    public static String descricaoSituacao(BigDecimal diferenca) {
        return situacao(diferenca);
    }

    /** Expõe o status para consumidores que queiram rotular. */
    public static boolean fechada(ResumoCaixaDTO resumo) {
        return resumo.status() == StatusSessaoCaixa.CLOSED;
    }
}
