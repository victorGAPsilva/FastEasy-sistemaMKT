package com.fasteasy.presentation.view;

import com.fasteasy.domain.Category;
import com.fasteasy.domain.Product;
import com.fasteasy.domain.Supplier;
import com.fasteasy.domain.UnidadeMedida;
import com.fasteasy.presentation.util.TarefaAssincrona;
import com.fasteasy.service.CategoryService;
import com.fasteasy.service.ProductService;
import com.fasteasy.service.SupplierService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Formulário para cadastro e edição de produtos.
 * Componentes organizados por seções com validação.
 * As operações de banco são executadas fora da thread da interface.
 */
@Component
public class FormProduto extends VBox {

    private static final Logger log = LoggerFactory.getLogger(FormProduto.class);

    private final ProductService productService;
    private final CategoryService categoryService;
    private final SupplierService supplierService;

    // Campos do formulário
    private final TextField txtNome = new TextField();
    private final TextArea txtDescricao = new TextArea();
    private final TextField txtCodigoBarras = new TextField();
    private final TextField txtSku = new TextField();
    private final ComboBox<Category> cmbCategoria = new ComboBox<>();
    private final TextField txtMarca = new TextField();
    private final ComboBox<UnidadeMedida> cmbUnidadeMedida = new ComboBox<>();
    private final TextField txtCusto = new TextField();
    private final TextField txtPrecoVenda = new TextField();
    private final TextField txtEstoqueMinimo = new TextField();
    private final TextField txtEstoqueMaximo = new TextField();
    private final ComboBox<Supplier> cmbFornecedor = new ComboBox<>();

    private Product produtoEditando = null;

    @Autowired
    public FormProduto(ProductService productService,
                       CategoryService categoryService,
                       SupplierService supplierService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.supplierService = supplierService;

        inicializarComponentes();
        carregarCombos();
    }

    private void inicializarComponentes() {
        setSpacing(15);
        setPadding(new Insets(15));
        setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 5; -fx-background-radius: 5;");

        // Título
        Label lblTitulo = new Label("Novo Produto");
        lblTitulo.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Grid principal
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 0, 10, 0));

        // Linha 1: Nome (obrigatório)
        grid.add(criarLabel("Nome *:"), 0, 0);
        txtNome.setPromptText("Nome do produto");
        txtNome.setPrefWidth(400);
        grid.add(txtNome, 1, 0);

        // Linha 2: Descrição
        grid.add(criarLabel("Descrição:"), 0, 1);
        txtDescricao.setPromptText("Descrição opcional");
        txtDescricao.setPrefRowCount(3);
        txtDescricao.setPrefWidth(400);
        grid.add(txtDescricao, 1, 1);

        // Linha 3: Código de barras | SKU
        grid.add(criarLabel("Código de Barras:"), 0, 2);
        txtCodigoBarras.setPromptText("EAN-8, EAN-13 ou código interno");
        grid.add(txtCodigoBarras, 1, 2);

        grid.add(criarLabel("SKU:"), 2, 2);
        txtSku.setPromptText("Código interno (opcional)");
        grid.add(txtSku, 3, 2);

        // Linha 4: Categoria | Unidade de medida
        grid.add(criarLabel("Categoria *:"), 0, 3);
        cmbCategoria.setPromptText("Selecione a categoria");
        cmbCategoria.setPrefWidth(300);
        grid.add(cmbCategoria, 1, 3);

        grid.add(criarLabel("Unidade de Medida *:"), 2, 3);
        cmbUnidadeMedida.setPromptText("Selecione a unidade");
        grid.add(cmbUnidadeMedida, 3, 3);

        // Linha 5: Marca | Fornecedor
        grid.add(criarLabel("Marca:"), 0, 4);
        txtMarca.setPromptText("Marca do produto");
        grid.add(txtMarca, 1, 4);

        grid.add(criarLabel("Fornecedor Principal:"), 2, 4);
        cmbFornecedor.setPromptText("Selecione o fornecedor");
        cmbFornecedor.setPrefWidth(300);
        grid.add(cmbFornecedor, 3, 4);

        // Linha 6: Custo | Preço de venda
        grid.add(criarLabel("Custo de Aquisição:"), 0, 5);
        txtCusto.setPromptText("0,00");
        grid.add(txtCusto, 1, 5);

        grid.add(criarLabel("Preço de Venda *:"), 2, 5);
        txtPrecoVenda.setPromptText("0,00");
        grid.add(txtPrecoVenda, 3, 5);

        // Linha 7: Estoque mínimo | Estoque máximo
        grid.add(criarLabel("Estoque Mínimo:"), 0, 6);
        txtEstoqueMinimo.setPromptText("0");
        grid.add(txtEstoqueMinimo, 1, 6);

        grid.add(criarLabel("Estoque Máximo:"), 2, 6);
        txtEstoqueMaximo.setPromptText("Opcional");
        grid.add(txtEstoqueMaximo, 3, 6);

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

        // Adicionar tudo ao container
        getChildren().addAll(lblTitulo, new Separator(), grid, botoes);
    }

    private Label criarLabel(String texto) {
        Label label = new Label(texto);
        label.setStyle("-fx-font-size: 13; -fx-text-fill: #2c3e50;");
        label.setMinWidth(150);
        return label;
    }

    private void carregarCombos() {
        // Unidades de medida
        cmbUnidadeMedida.setItems(FXCollections.observableArrayList(UnidadeMedida.values()));

        // Categorias ativas
        TarefaAssincrona.executar(
                categoryService::listarAtivos,
                categorias -> cmbCategoria.setItems(FXCollections.observableArrayList(categorias)),
                e -> log.error("Erro ao carregar categorias: {}", e.getMessage(), e));

        // Fornecedores ativos
        TarefaAssincrona.executar(
                supplierService::listarAtivos,
                fornecedores -> cmbFornecedor.setItems(FXCollections.observableArrayList(fornecedores)),
                e -> log.error("Erro ao carregar fornecedores: {}", e.getMessage(), e));
    }

    private void salvar() {
        try {
            // Validar campos obrigatórios
            if (!validarCampos()) {
                return;
            }

            Product produto = produtoEditando != null ? produtoEditando : new Product();

            produto.setNome(txtNome.getText().trim());
            produto.setDescricao(txtDescricao.getText().trim().isEmpty() ? null : txtDescricao.getText().trim());
            produto.setCodigoBarras(txtCodigoBarras.getText().trim().isEmpty() ? null : txtCodigoBarras.getText().trim());
            produto.setSku(txtSku.getText().trim().isEmpty() ? null : txtSku.getText().trim());

            Category categoria = cmbCategoria.getValue();
            if (categoria == null) {
                mostrarErro("Selecione uma categoria");
                return;
            }
            produto.setCategoria(categoria);

            produto.setMarca(txtMarca.getText().trim().isEmpty() ? null : txtMarca.getText().trim());
            produto.setUnidadeMedida(cmbUnidadeMedida.getValue() != null ? cmbUnidadeMedida.getValue() : UnidadeMedida.UN);

            // Valores monetários
            try {
                String custoStr = txtCusto.getText().replace(",", ".");
                if (!custoStr.isEmpty()) {
                    produto.setCustoAquisicao(new BigDecimal(custoStr));
                }
            } catch (NumberFormatException e) {
                mostrarErro("Custo de aquisição inválido");
                return;
            }

            try {
                String precoStr = txtPrecoVenda.getText().replace(",", ".");
                produto.setPrecoVenda(new BigDecimal(precoStr));
            } catch (NumberFormatException e) {
                mostrarErro("Preço de venda inválido");
                return;
            }

            // Estoque
            try {
                if (!txtEstoqueMinimo.getText().isEmpty()) {
                    produto.setEstoqueMinimo(Integer.parseInt(txtEstoqueMinimo.getText()));
                }
            } catch (NumberFormatException e) {
                mostrarErro("Estoque mínimo inválido");
                return;
            }

            try {
                if (!txtEstoqueMaximo.getText().isEmpty()) {
                    produto.setEstoqueMaximo(new BigDecimal(txtEstoqueMaximo.getText()));
                }
            } catch (NumberFormatException e) {
                mostrarErro("Estoque máximo inválido");
                return;
            }

            Supplier fornecedor = cmbFornecedor.getValue();
            if (fornecedor != null) {
                produto.setFornecedorPrincipal(fornecedor);
            }

            final boolean editando = produtoEditando != null;
            final Long id = editando ? produtoEditando.getId() : null;

            TarefaAssincrona.executar(
                    () -> editando ? productService.editar(id, produto) : productService.cadastrar(produto),
                    salvo -> {
                        mostrarSucesso(editando
                                ? "Produto atualizado com sucesso!"
                                : "Produto cadastrado com sucesso!");
                        limpar();
                    },
                    this::mostrarErroDeOperacao);

        } catch (IllegalArgumentException e) {
            mostrarErro(e.getMessage());
        } catch (Exception e) {
            mostrarErro("Erro ao salvar: " + e.getMessage());
        }
    }

    private void mostrarErroDeOperacao(Throwable e) {
        if (e instanceof IllegalArgumentException) {
            mostrarErro(e.getMessage());
        } else {
            mostrarErro("Erro ao salvar: " + e.getMessage());
        }
    }

    private boolean validarCampos() {
        if (txtNome.getText().trim().isEmpty()) {
            mostrarErro("Nome do produto é obrigatório");
            txtNome.requestFocus();
            return false;
        }

        if (cmbCategoria.getValue() == null) {
            mostrarErro("Categoria é obrigatória");
            cmbCategoria.requestFocus();
            return false;
        }

        if (cmbUnidadeMedida.getValue() == null) {
            mostrarErro("Unidade de medida é obrigatória");
            cmbUnidadeMedida.requestFocus();
            return false;
        }

        if (txtPrecoVenda.getText().trim().isEmpty()) {
            mostrarErro("Preço de venda é obrigatório");
            txtPrecoVenda.requestFocus();
            return false;
        }

        return true;
    }

    private void limpar() {
        produtoEditando = null;
        txtNome.clear();
        txtDescricao.clear();
        txtCodigoBarras.clear();
        txtSku.clear();
        cmbCategoria.getSelectionModel().clearSelection();
        txtMarca.clear();
        cmbUnidadeMedida.getSelectionModel().selectFirst();
        txtCusto.clear();
        txtPrecoVenda.clear();
        txtEstoqueMinimo.clear();
        txtEstoqueMaximo.clear();
        cmbFornecedor.getSelectionModel().clearSelection();

        // Atualizar título do formulário
        for (javafx.scene.Node node : getChildren()) {
            if (node instanceof Label label && label.getText().contains("Produto")) {
                label.setText("Novo Produto");
                break;
            }
        }
    }

    public void editar(Product produto) {
        this.produtoEditando = produto;

        txtNome.setText(produto.getNome());
        txtDescricao.setText(produto.getDescricao() != null ? produto.getDescricao() : "");
        txtCodigoBarras.setText(produto.getCodigoBarras() != null ? produto.getCodigoBarras() : "");
        txtSku.setText(produto.getSku() != null ? produto.getSku() : "");
        cmbCategoria.setValue(produto.getCategoria());
        txtMarca.setText(produto.getMarca() != null ? produto.getMarca() : "");
        cmbUnidadeMedida.setValue(produto.getUnidadeMedida());

        if (produto.getCustoAquisicao() != null) {
            txtCusto.setText(produto.getCustoAquisicao().toString());
        }
        if (produto.getPrecoVenda() != null) {
            txtPrecoVenda.setText(produto.getPrecoVenda().toString());
        }
        if (produto.getEstoqueMinimo() != null) {
            txtEstoqueMinimo.setText(produto.getEstoqueMinimo().toString());
        }
        if (produto.getEstoqueMaximo() != null) {
            txtEstoqueMaximo.setText(produto.getEstoqueMaximo().toString());
        }
        cmbFornecedor.setValue(produto.getFornecedorPrincipal());

        // Atualizar título
        for (javafx.scene.Node node : getChildren()) {
            if (node instanceof Label label && label.getText().contains("Produto")) {
                label.setText("Editar Produto");
                break;
            }
        }
    }

    public void recarregarDados() {
        carregarCombos();
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