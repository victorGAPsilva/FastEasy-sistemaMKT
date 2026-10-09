package com.fasteasy.presentation.view;

import com.fasteasy.domain.Category;
import com.fasteasy.presentation.util.TarefaAssincrona;
import com.fasteasy.service.CategoryService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Formulário para cadastro e edição de categorias.
 * As operações de banco são executadas fora da thread da interface.
 */
@Component
public class FormCategoria extends VBox {

    private final CategoryService categoryService;

    private final TextField txtNome = new TextField();
    private final TextArea txtDescricao = new TextArea();
    private Category categoriaEditando = null;

    @Autowired
    public FormCategoria(CategoryService categoryService) {
        this.categoryService = categoryService;
        inicializarComponentes();
    }

    private void inicializarComponentes() {
        setSpacing(15);
        setPadding(new Insets(15));
        setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 5; -fx-background-radius: 5;");

        Label lblTitulo = new Label("Nova Categoria");
        lblTitulo.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 0, 10, 0));

        // Nome
        grid.add(criarLabel("Nome *:"), 0, 0);
        txtNome.setPromptText("Nome da categoria");
        txtNome.setPrefWidth(400);
        grid.add(txtNome, 1, 0);

        // Descrição
        grid.add(criarLabel("Descrição:"), 0, 1);
        txtDescricao.setPromptText("Descrição opcional");
        txtDescricao.setPrefRowCount(3);
        txtDescricao.setPrefWidth(400);
        grid.add(txtDescricao, 1, 1);

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
        label.setMinWidth(120);
        return label;
    }

    private void salvar() {
        try {
            if (txtNome.getText().trim().isEmpty()) {
                mostrarErro("Nome da categoria é obrigatório");
                txtNome.requestFocus();
                return;
            }

            Category categoria = categoriaEditando != null ? categoriaEditando : new Category();
            categoria.setNome(txtNome.getText().trim());
            categoria.setDescricao(txtDescricao.getText().trim().isEmpty() ? null : txtDescricao.getText().trim());

            final boolean editando = categoriaEditando != null;
            final Long id = editando ? categoriaEditando.getId() : null;

            TarefaAssincrona.executar(
                    () -> editando ? categoryService.editar(id, categoria) : categoryService.cadastrar(categoria),
                    salvo -> {
                        mostrarSucesso(editando
                                ? "Categoria atualizada com sucesso!"
                                : "Categoria cadastrada com sucesso!");
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
        categoriaEditando = null;
        txtNome.clear();
        txtDescricao.clear();

        for (javafx.scene.Node node : getChildren()) {
            if (node instanceof Label label && label.getText().contains("Categoria")) {
                label.setText("Nova Categoria");
                break;
            }
        }
    }

    public void editar(Category categoria) {
        this.categoriaEditando = categoria;
        txtNome.setText(categoria.getNome());
        txtDescricao.setText(categoria.getDescricao() != null ? categoria.getDescricao() : "");

        for (javafx.scene.Node node : getChildren()) {
            if (node instanceof Label label && label.getText().contains("Categoria")) {
                label.setText("Editar Categoria");
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