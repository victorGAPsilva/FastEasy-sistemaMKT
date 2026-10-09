-- Flyway Migration: V1.0.3__create_sales_tables.sql
-- Módulo de vendas (PDV): vendas, itens e pagamentos.
-- Data: 2026-10-09

-- Vendas
CREATE TABLE IF NOT EXISTS sales (
    id BIGSERIAL PRIMARY KEY,
    numero VARCHAR(20),
    data_hora TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    subtotal NUMERIC(19, 4) NOT NULL,
    desconto_total NUMERIC(19, 4) NOT NULL DEFAULT 0,
    valor_total NUMERIC(19, 4) NOT NULL,
    valor_recebido NUMERIC(19, 4) NOT NULL DEFAULT 0,
    troco NUMERIC(19, 4) NOT NULL DEFAULT 0,
    operador VARCHAR(100),
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_sales_numero ON sales(numero);
CREATE INDEX IF NOT EXISTS idx_sales_data_hora ON sales(data_hora);
CREATE INDEX IF NOT EXISTS idx_sales_status ON sales(status);

ALTER TABLE sales
    ADD CONSTRAINT chk_sales_status_valid
    CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'CANCELLED'));

ALTER TABLE sales
    ADD CONSTRAINT chk_sales_subtotal_non_negative CHECK (subtotal >= 0);

ALTER TABLE sales
    ADD CONSTRAINT chk_sales_desconto_non_negative CHECK (desconto_total >= 0);

ALTER TABLE sales
    ADD CONSTRAINT chk_sales_valor_total_non_negative CHECK (valor_total >= 0);

ALTER TABLE sales
    ADD CONSTRAINT chk_sales_troco_non_negative CHECK (troco >= 0);

-- Itens da venda (com dados históricos do produto)
CREATE TABLE IF NOT EXISTS sale_items (
    id BIGSERIAL PRIMARY KEY,
    venda_id BIGINT NOT NULL,
    produto_id BIGINT NOT NULL,
    nome_produto VARCHAR(150) NOT NULL,
    codigo_produto VARCHAR(50),
    quantidade NUMERIC(19, 4) NOT NULL,
    preco_unitario NUMERIC(19, 4) NOT NULL,
    desconto NUMERIC(19, 4) NOT NULL DEFAULT 0,
    total NUMERIC(19, 4) NOT NULL
);

ALTER TABLE sale_items
    ADD CONSTRAINT fk_sale_items_venda
    FOREIGN KEY (venda_id) REFERENCES sales(id);

ALTER TABLE sale_items
    ADD CONSTRAINT fk_sale_items_produto
    FOREIGN KEY (produto_id) REFERENCES products(id);

CREATE INDEX IF NOT EXISTS idx_sale_items_venda_id ON sale_items(venda_id);
CREATE INDEX IF NOT EXISTS idx_sale_items_produto_id ON sale_items(produto_id);

ALTER TABLE sale_items
    ADD CONSTRAINT chk_sale_items_quantidade_positive CHECK (quantidade > 0);

ALTER TABLE sale_items
    ADD CONSTRAINT chk_sale_items_preco_non_negative CHECK (preco_unitario >= 0);

ALTER TABLE sale_items
    ADD CONSTRAINT chk_sale_items_desconto_non_negative CHECK (desconto >= 0);

ALTER TABLE sale_items
    ADD CONSTRAINT chk_sale_items_total_non_negative CHECK (total >= 0);

-- Pagamentos da venda
CREATE TABLE IF NOT EXISTS sale_payments (
    id BIGSERIAL PRIMARY KEY,
    venda_id BIGINT NOT NULL,
    forma_pagamento VARCHAR(20) NOT NULL,
    valor NUMERIC(19, 4) NOT NULL,
    valor_recebido NUMERIC(19, 4) NOT NULL,
    data_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE sale_payments
    ADD CONSTRAINT fk_sale_payments_venda
    FOREIGN KEY (venda_id) REFERENCES sales(id);

CREATE INDEX IF NOT EXISTS idx_sale_payments_venda_id ON sale_payments(venda_id);

ALTER TABLE sale_payments
    ADD CONSTRAINT chk_sale_payments_forma_valid
    CHECK (forma_pagamento IN ('DINHEIRO', 'PIX', 'DEBITO', 'CREDITO'));

ALTER TABLE sale_payments
    ADD CONSTRAINT chk_sale_payments_valor_positive CHECK (valor > 0);

ALTER TABLE sale_payments
    ADD CONSTRAINT chk_sale_payments_recebido_non_negative CHECK (valor_recebido >= 0);

-- Associação das movimentações de estoque com a venda
ALTER TABLE stock_movements
    ADD CONSTRAINT fk_stock_movements_venda
    FOREIGN KEY (venda_id) REFERENCES sales(id);

COMMENT ON TABLE sales IS 'Vendas concluídas no PDV';
COMMENT ON TABLE sale_items IS 'Itens das vendas (dados históricos do produto)';
COMMENT ON TABLE sale_payments IS 'Pagamentos das vendas';
