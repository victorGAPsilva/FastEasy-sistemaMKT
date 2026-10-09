package com.fasteasy.presentation.view;

import com.fasteasy.domain.Supplier;
import com.fasteasy.presentation.util.TarefaAssincrona;
import com.fasteasy.service.SupplierService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Formulário para cadastro e edição de fornecedores.
 * As operações de banco são executadas fora da thread da interface.
 */
@Component
public class FormFornecedor extends VBox {

    private final SupplierService supplierService;

    private final TextField txtNomeRazaoSocial = new TextField();
    private final TextField txtNomeFantasia = new TextField();
    private final TextField txtCpfCnpj = new TextField();
    private final TextField txtTelefone = new TextField();
    private final TextField txtEmail = new TextField();
    private final TextField txtEndereco = new TextField();
    private final TextArea txtObservacoes = new TextArea();

    private Supplier fornecedorEditando = null;

    @Autowired
    public FormFornecedor(SupplierService supplierService) {
        this.supplierService = supplierService;
        inicializarComponentes();
    }

    private void inicializarComponentes() {
        setSpacing(15);
        setPadding(new Insets(15));
        setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 5; -fx-background-radius: 5;");

        Label lblTitulo = new Label("Novo Fornecedor");
        lblTitulo.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 0, 10, 0));

        // Nome/Razão Social
        grid.add(criarLabel("Nome/Razão Social *:"), 0, 0);
        txtNomeRazaoSocial.setPromptText("Nome ou razão social");
        txtNomeRazaoSocial.setPrefWidth(400);
        grid.add(txtNomeRazaoSocial, 1, 0);

        // Nome Fantasia
        grid.add(criarLabel("Nome Fantasia:"), 2, 0);
        txtNomeFantasia.setPromptText("Nome fantasia (opcional)");
        grid.add(txtNomeFantasia, 3, 0);

        // CPF/CNPJ
        grid.add(criarLabel("CPF/CNPJ:"), 0, 1);
        txtCpfCnpj.setPromptText("Apenas números (opcional)");
        grid.add(txtCpfCnpj, 1, 1);

        // Telefone
        grid.add(criarLabel("Telefone:"), 2, 1);
        txtTelefone.setPromptText("(XX) XXXXX-XXXX");
        grid.add(txtTelefone, 3, 1);

        // E-mail
        grid.add(criarLabel("E-mail:"), 0, 2);
        txtEmail.setPromptText("contato@fornecedor.com");
        grid.add(txtEmail, 1, 2);

        // Endereço
        grid.add(criarLabel("Endereço:"), 0, 3);
        txtEndereco.setPromptText("Endereço completo");
        txtEndereco.setPrefWidth(600);
        GridPane.setColumnSpan(txtEndereco, 3);
        grid.add(txtEndereco, 1, 3);

        // Observações
        grid.add(criarLabel("Observações:"), 0, 4);
        txtObservacoes.setPromptText("Observações gerais");
        txtObservacoes.setPrefRowCount(3);
        txtObservacoes.setPrefWidth(600);
        GridPane.setColumnSpan(txtObservacoes, 3);
        grid.add(txtObservacoes, 1, 4);

        // Botões
        HBox botoes = new HBox(10);
        botoes.setAlignment(Pos.CENTER_RIGHT);
        botoes.setPadding(new Insets(10, 0, 0, 0));

        Button btnSalvar = new Button("Salvar");
        btnSalvar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btnSalvar.setOnAction(e -> salvar());

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
        btnCancelar.setOnAction(e -> limpar());

        botoes.getChildren().addAll(btnCancelar, btnSalvar);

        getChildren().addAll(lblTitulo, new Separator(), grid, botoes);
    }

    private Label criarLabel(String texto) {
        Label label = new Label(texto);
        label.setStyle("-fx-font-size: 13; -fx-text-fill: #2c3e50;");
        label.setMinWidth(150);
        return label;
    }

    private void salvar() {
        try {
            if (txtNomeRazaoSocial.getText().trim().isEmpty()) {
                mostrarErro("Nome ou razão social é obrigatório");
                txtNomeRazaoSocial.requestFocus();
                return;
            }

            Supplier fornecedor = fornecedorEditando != null ? fornecedorEditando : new Supplier();
            fornecedor.setNomeRazaoSocial(txtNomeRazaoSocial.getText().trim());
            fornecedor.setNomeFantasia(txtNomeFantasia.getText().trim().isEmpty() ? null : txtNomeFantasia.getText().trim());
            fornecedor.setCpfCnpj(txtCpfCnpj.getText().trim().isEmpty() ? null : txtCpfCnpj.getText().trim());
            fornecedor.setTelefone(txtTelefone.getText().trim().isEmpty() ? null : txtTelefone.getText().trim());
            fornecedor.setEmail(txtEmail.getText().trim().isEmpty() ? null : txtEmail.getText().trim());
            fornecedor.setEndereco(txtEndereco.getText().trim().isEmpty() ? null : txtEndereco.getText().trim());
            fornecedor.setObservacoes(txtObservacoes.getText().trim().isEmpty() ? null : txtObservacoes.getText().trim());

            final boolean editando = fornecedorEditando != null;
            final Long id = editando ? fornecedorEditando.getId() : null;

            TarefaAssincrona.executar(
                    () -> editando ? supplierService.editar(id, fornecedor) : supplierService.cadastrar(fornecedor),
                    salvo -> {
                        mostrarSucesso(editando
                                ? "Fornecedor atualizado com sucesso!"
                                : "Fornecedor cadastrado com sucesso!");
                        limpar();
                    },
                    e -> mostrarErro(e instanceof IllegalArgumentException
                            ? e.getMessage()
                            : "Erro ao salvar: " + e.getMessage()));

        } catch (IllegalArgumentException e) {
            mostrarErro(e.getMessage());
        } catch (Exception e) {
            mostrarErro("Erro ao salvar: " + e.getMessage());
        }
    }

    private void limpar() {
        fornecedorEditando = null;
        txtNomeRazaoSocial.clear();
        txtNomeFantasia.clear();
        txtCpfCnpj.clear();
        txtTelefone.clear();
        txtEmail.clear();
        txtEndereco.clear();
        txtObservacoes.clear();

        for (javafx.scene.Node node : getChildren()) {
            if (node instanceof Label label && label.getText().contains("Fornecedor")) {
                label.setText("Novo Fornecedor");
                break;
            }
        }
    }

    public void editar(Supplier fornecedor) {
        this.fornecedorEditando = fornecedor;
        txtNomeRazaoSocial.setText(fornecedor.getNomeRazaoSocial());
        txtNomeFantasia.setText(fornecedor.getNomeFantasia() != null ? fornecedor.getNomeFantasia() : "");
        txtCpfCnpj.setText(fornecedor.getCpfCnpj() != null ? fornecedor.getCpfCnpj() : "");
        txtTelefone.setText(fornecedor.getTelefone() != null ? fornecedor.getTelefone() : "");
        txtEmail.setText(fornecedor.getEmail() != null ? fornecedor.getEmail() : "");
        txtEndereco.setText(fornecedor.getEndereco() != null ? fornecedor.getEndereco() : "");
        txtObservacoes.setText(fornecedor.getObservacoes() != null ? fornecedor.getObservacoes() : "");

        for (javafx.scene.Node node : getChildren()) {
            if (node instanceof Label label && label.getText().contains("Fornecedor")) {
                label.setText("Editar Fornecedor");
                break;
            }
        }
    }

    private void mostrarErro(String mensagem) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro de Validação");
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

    private void mostrarSucesso(String mensagem) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Sucesso");
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}