-- Flyway Migration: V1.0.2__create_stock_tables.sql
-- Base de estoque: quantidade atual em produtos e histórico de movimentações.
-- Data: 2026-10-09

-- Quantidade atual em estoque (controlada pelo StockService)
ALTER TABLE products
    ADD COLUMN estoque_atual NUMERIC(19, 4) NOT NULL DEFAULT 0;

ALTER TABLE products
    ADD CONSTRAINT chk_products_estoque_atual_non_negative
    CHECK (estoque_atual >= 0);

-- Histórico auditável de movimentações de estoque
CREATE TABLE IF NOT EXISTS stock_movements (
    id BIGSERIAL PRIMARY KEY,
    produto_id BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    quantidade NUMERIC(19, 4) NOT NULL,
    quantidade_anterior NUMERIC(19, 4) NOT NULL,
    quantidade_posterior NUMERIC(19, 4) NOT NULL,
    motivo VARCHAR(255),
    venda_id BIGINT,
    data_movimentacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE stock_movements
    ADD CONSTRAINT fk_stock_movements_produto
    FOREIGN KEY (produto_id) REFERENCES products(id);

CREATE INDEX IF NOT EXISTS idx_stock_movements_produto_id ON stock_movements(produto_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_venda_id ON stock_movements(venda_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_data ON stock_movements(data_movimentacao);

ALTER TABLE stock_movements
    ADD CONSTRAINT chk_stock_movements_tipo_valid
    CHECK (tipo IN ('ENTRADA', 'SAIDA', 'AJUSTE', 'SALE_EXIT'));

ALTER TABLE stock_movements
    ADD CONSTRAINT chk_stock_movements_quantidade_positive
    CHECK (quantidade > 0);

ALTER TABLE stock_movements
    ADD CONSTRAINT chk_stock_movements_qtd_posterior_non_negative
    CHECK (quantidade_posterior >= 0);

COMMENT ON TABLE stock_movements IS 'Histórico auditável de movimentações de estoque';
COMMENT ON COLUMN stock_movements.tipo IS 'ENTRADA, SAIDA, AJUSTE ou SALE_EXIT (saída por venda)';
COMMENT ON COLUMN stock_movements.venda_id IS 'Venda associada (apenas para SALE_EXIT)';
