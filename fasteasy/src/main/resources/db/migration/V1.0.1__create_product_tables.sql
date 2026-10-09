-- Flyway Migration: V1.0.1__create_product_tables.sql
-- Criação das tabelas de produtos, categorias e fornecedores
-- Data: 2026-10-09

-- Tabela de Categorias
CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP
);

-- Índices para Categorias
CREATE INDEX IF NOT EXISTS idx_categories_nome ON categories(nome);
CREATE INDEX IF NOT EXISTS idx_categories_status ON categories(status);

-- Tabela de Fornecedores
CREATE TABLE IF NOT EXISTS suppliers (
    id BIGSERIAL PRIMARY KEY,
    nome_razao_social VARCHAR(150) NOT NULL,
    nome_fantasia VARCHAR(100),
    cpf_cnpj VARCHAR(20),
    telefone VARCHAR(20),
    email VARCHAR(100),
    endereco VARCHAR(200),
    observacoes VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    data_cadastro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP
);

-- Índices para Fornecedores
CREATE INDEX IF NOT EXISTS idx_suppliers_nome_razao_social ON suppliers(nome_razao_social);
CREATE INDEX IF NOT EXISTS idx_suppliers_cpf_cnpj ON suppliers(cpf_cnpj);
CREATE INDEX IF NOT EXISTS idx_suppliers_status ON suppliers(status);

-- Tabela de Produtos
CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    descricao VARCHAR(500),
    codigo_barras VARCHAR(50),
    sku VARCHAR(50),
    categoria_id BIGINT NOT NULL,
    marca VARCHAR(100),
    unidade_medida VARCHAR(10) NOT NULL DEFAULT 'UN',
    custo_aquisicao NUMERIC(19,4),
    preco_venda NUMERIC(19,4) NOT NULL,
    estoque_minimo INTEGER NOT NULL DEFAULT 0,
    estoque_maximo NUMERIC(19,4),
    fornecedor_principal_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP
);

-- Foreign Keys
ALTER TABLE products
    ADD CONSTRAINT fk_products_categoria
    FOREIGN KEY (categoria_id) REFERENCES categories(id);

ALTER TABLE products
    ADD CONSTRAINT fk_products_fornecedor_principal
    FOREIGN KEY (fornecedor_principal_id) REFERENCES suppliers(id);

-- Índices para Produtos
CREATE INDEX IF NOT EXISTS idx_products_nome ON products(nome);
CREATE UNIQUE INDEX IF NOT EXISTS idx_products_sku ON products(sku) WHERE sku IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_products_codigo_barras ON products(codigo_barras) WHERE codigo_barras IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_products_categoria_id ON products(categoria_id);
CREATE INDEX IF NOT EXISTS idx_products_status ON products(status);
CREATE INDEX IF NOT EXISTS idx_products_fornecedor_principal_id ON products(fornecedor_principal_id);

-- Constraints de validação
ALTER TABLE products
    ADD CONSTRAINT chk_products_preco_venda_positive
    CHECK (preco_venda > 0);

ALTER TABLE products
    ADD CONSTRAINT chk_products_custo_non_negative
    CHECK (custo_aquisicao IS NULL OR custo_aquisicao >= 0);

ALTER TABLE products
    ADD CONSTRAINT chk_products_estoque_minimo_non_negative
    CHECK (estoque_minimo >= 0);

ALTER TABLE products
    ADD CONSTRAINT chk_products_estoque_maximo_maior_minimo
    CHECK (estoque_maximo IS NULL OR estoque_maximo >= estoque_minimo);

ALTER TABLE products
    ADD CONSTRAINT chk_products_status_valid
    CHECK (status IN ('ATIVO', 'INATIVO'));

-- Constraints para Categorias
ALTER TABLE categories
    ADD CONSTRAINT chk_categories_status_valid
    CHECK (status IN ('ATIVO', 'INATIVO'));

-- Constraints para Fornecedores
ALTER TABLE suppliers
    ADD CONSTRAINT chk_suppliers_status_valid
    CHECK (status IN ('ATIVO', 'INATIVO'));

-- Comentários nas tabelas
COMMENT ON TABLE categories IS 'Categorias de produtos do supermercado';
COMMENT ON TABLE suppliers IS 'Fornecedores de produtos';
COMMENT ON TABLE products IS 'Produtos do supermercado';

COMMENT ON COLUMN products.unidade_medida IS 'Unidades: UN, KG, G, L, ML, PCT, CX';
COMMENT ON COLUMN products.custo_aquisicao IS 'Custo de aquisição do produto (4 casas decimais)';
COMMENT ON COLUMN products.preco_venda IS 'Preço de venda do produto (4 casas decimais)';
COMMENT ON COLUMN products.estoque_minimo IS 'Quantidade mínima em estoque';
COMMENT ON COLUMN products.estoque_maximo IS 'Quantidade máxima em estoque (opcional)';