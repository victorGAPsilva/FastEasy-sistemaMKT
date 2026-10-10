-- Flyway Migration: V1.0.5__create_financial_tables.sql
-- Módulo financeiro: categorias de despesa, contas a pagar, parcelas, pagamentos e compras.
-- Data: 2026-10-09

-- ------------------------------------------------------------------
-- Categorias de despesa operacional
-- ------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS expense_categories (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_expense_categories_nome ON expense_categories(nome);
CREATE INDEX IF NOT EXISTS idx_expense_categories_status ON expense_categories(status);

ALTER TABLE expense_categories
    ADD CONSTRAINT chk_expense_categories_status_valid
    CHECK (status IN ('ATIVO', 'INATIVO'));

COMMENT ON TABLE expense_categories IS 'Categorias de despesas operacionais (aluguel, energia, salários, etc.)';

-- Inserir categorias padrão
INSERT INTO expense_categories (nome, descricao, status) VALUES
    ('ALUGUEL', 'Aluguel do imóvel', 'ATIVO'),
    ('ENERGIA', 'Energia elétrica', 'ATIVO'),
    ('AGUA', 'Água e esgoto', 'ATIVO'),
    ('INTERNET', 'Internet e telefonia', 'ATIVO'),
    ('TELEFONE', 'Telefone fixo e móvel', 'ATIVO'),
    ('SALARIOS', 'Salários e encargos', 'ATIVO'),
    ('MANUTENCAO', 'Manutenção de equipamentos e instalações', 'ATIVO'),
    ('TRANSPORTE', 'Transporte e fretes', 'ATIVO'),
    ('MATERIAL_DE_LIMPEZA', 'Material de limpeza e higiene', 'ATIVO'),
    ('SERVICOS', 'Serviços terceirizados', 'ATIVO'),
    ('OUTRAS_DESPESAS', 'Outras despesas operacionais', 'ATIVO')
ON CONFLICT (nome) DO NOTHING;

-- ------------------------------------------------------------------
-- Compras (base para integração financeira)
-- ------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS purchases (
    id BIGSERIAL PRIMARY KEY,
    numero VARCHAR(20),
    fornecedor_id BIGINT NOT NULL,
    data_emissao TIMESTAMP NOT NULL,
    data_prevista_entrega TIMESTAMP,
    valor_total NUMERIC(19, 4) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    observacoes VARCHAR(500),
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP
);

ALTER TABLE purchases
    ADD CONSTRAINT fk_purchases_fornecedor
    FOREIGN KEY (fornecedor_id) REFERENCES suppliers(id);

CREATE INDEX IF NOT EXISTS idx_purchases_fornecedor ON purchases(fornecedor_id);
CREATE INDEX IF NOT EXISTS idx_purchases_status ON purchases(status);
CREATE INDEX IF NOT EXISTS idx_purchases_data_emissao ON purchases(data_emissao);

ALTER TABLE purchases
    ADD CONSTRAINT chk_purchases_valor_non_negative
    CHECK (valor_total >= 0);

ALTER TABLE purchases
    ADD CONSTRAINT chk_purchases_status_valid
    CHECK (status IN ('PENDING', 'RECEIVED', 'CANCELLED'));

-- Itens da compra
CREATE TABLE IF NOT EXISTS purchase_items (
    id BIGSERIAL PRIMARY KEY,
    compra_id BIGINT NOT NULL,
    produto_id BIGINT NOT NULL,
    quantidade NUMERIC(19, 4) NOT NULL,
    preco_unitario NUMERIC(19, 4) NOT NULL,
    total NUMERIC(19, 4) NOT NULL
);

ALTER TABLE purchase_items
    ADD CONSTRAINT fk_purchase_items_compra
    FOREIGN KEY (compra_id) REFERENCES purchases(id);

ALTER TABLE purchase_items
    ADD CONSTRAINT fk_purchase_items_produto
    FOREIGN KEY (produto_id) REFERENCES products(id);

CREATE INDEX IF NOT EXISTS idx_purchase_items_compra ON purchase_items(compra_id);
CREATE INDEX IF NOT EXISTS idx_purchase_items_produto ON purchase_items(produto_id);

ALTER TABLE purchase_items
    ADD CONSTRAINT chk_purchase_items_quantidade_positive
    CHECK (quantidade > 0);

ALTER TABLE purchase_items
    ADD CONSTRAINT chk_purchase_items_preco_non_negative
    CHECK (preco_unitario >= 0);

ALTER TABLE purchase_items
    ADD CONSTRAINT chk_purchase_items_total_non_negative
    CHECK (total >= 0);

-- ------------------------------------------------------------------
-- Contas a pagar
-- ------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS payable_accounts (
    id BIGSERIAL PRIMARY KEY,
    descricao VARCHAR(255) NOT NULL,
    fornecedor_id BIGINT,
    compra_id BIGINT,
    categoria_id BIGINT NOT NULL,
    data_emissao DATE NOT NULL,
    data_competencia DATE,
    valor_original NUMERIC(19, 4) NOT NULL,
    valor_pago NUMERIC(19, 4) NOT NULL DEFAULT 0,
    saldo_pendente NUMERIC(19, 4) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    observacoes VARCHAR(500),
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP
);

ALTER TABLE payable_accounts
    ADD CONSTRAINT fk_payable_accounts_fornecedor
    FOREIGN KEY (fornecedor_id) REFERENCES suppliers(id);

ALTER TABLE payable_accounts
    ADD CONSTRAINT fk_payable_accounts_compra
    FOREIGN KEY (compra_id) REFERENCES purchases(id);

ALTER TABLE payable_accounts
    ADD CONSTRAINT fk_payable_accounts_categoria
    FOREIGN KEY (categoria_id) REFERENCES expense_categories(id);

CREATE INDEX IF NOT EXISTS idx_payable_accounts_fornecedor ON payable_accounts(fornecedor_id);
CREATE INDEX IF NOT EXISTS idx_payable_accounts_compra ON payable_accounts(compra_id);
CREATE INDEX IF NOT EXISTS idx_payable_accounts_categoria ON payable_accounts(categoria_id);
CREATE INDEX IF NOT EXISTS idx_payable_accounts_status ON payable_accounts(status);
CREATE INDEX IF NOT EXISTS idx_payable_accounts_data_emissao ON payable_accounts(data_emissao);
CREATE INDEX IF NOT EXISTS idx_payable_accounts_data_competencia ON payable_accounts(data_competencia);

-- Índice para busca de contas com parcelas vencidas
CREATE INDEX IF NOT EXISTS idx_payable_accounts_vencimento ON payable_accounts(id) WHERE status IN ('OPEN', 'PARTIALLY_PAID');

ALTER TABLE payable_accounts
    ADD CONSTRAINT chk_payable_accounts_status_valid
    CHECK (status IN ('OPEN', 'PARTIALLY_PAID', 'PAID', 'CANCELLED'));

ALTER TABLE payable_accounts
    ADD CONSTRAINT chk_payable_accounts_valor_positive
    CHECK (valor_original > 0);

ALTER TABLE payable_accounts
    ADD CONSTRAINT chk_payable_accounts_valor_pago_non_negative
    CHECK (valor_pago >= 0);

ALTER TABLE payable_accounts
    ADD CONSTRAINT chk_payable_accounts_saldo_non_negative
    CHECK (saldo_pendente >= 0);

COMMENT ON TABLE payable_accounts IS 'Contas a pagar (obrigações financeiras com fornecedores e despesas)';

-- ------------------------------------------------------------------
-- Parcelas das contas a pagar
-- ------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS payable_installments (
    id BIGSERIAL PRIMARY KEY,
    conta_id BIGINT NOT NULL,
    numero_parcela INTEGER NOT NULL,
    data_vencimento DATE NOT NULL,
    valor_original NUMERIC(19, 4) NOT NULL,
    valor_pago NUMERIC(19, 4) NOT NULL DEFAULT 0,
    saldo_pendente NUMERIC(19, 4) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE payable_installments
    ADD CONSTRAINT fk_payable_installments_conta
    FOREIGN KEY (conta_id) REFERENCES payable_accounts(id);

CREATE INDEX IF NOT EXISTS idx_payable_installments_conta ON payable_installments(conta_id);
CREATE INDEX IF NOT EXISTS idx_payable_installments_vencimento ON payable_installments(data_vencimento);
CREATE INDEX IF NOT EXISTS idx_payable_installments_status ON payable_installments(status);

-- Garante que não haja duas parcelas com o mesmo número na mesma conta
CREATE UNIQUE INDEX IF NOT EXISTS uq_payable_installments_conta_numero
    ON payable_installments(conta_id, numero_parcela);

ALTER TABLE payable_installments
    ADD CONSTRAINT chk_payable_installments_status_valid
    CHECK (status IN ('OPEN', 'PARTIALLY_PAID', 'PAID', 'CANCELLED'));

ALTER TABLE payable_installments
    ADD CONSTRAINT chk_payable_installments_numero_positive
    CHECK (numero_parcela > 0);

ALTER TABLE payable_installments
    ADD CONSTRAINT chk_payable_installments_valor_positive
    CHECK (valor_original > 0);

ALTER TABLE payable_installments
    ADD CONSTRAINT chk_payable_installments_valor_pago_non_negative
    CHECK (valor_pago >= 0);

ALTER TABLE payable_installments
    ADD CONSTRAINT chk_payable_installments_saldo_non_negative
    CHECK (saldo_pendente >= 0);

COMMENT ON TABLE payable_installments IS 'Parcelas das contas a pagar';

-- ------------------------------------------------------------------
-- Pagamentos financeiros (saídas para quitar contas a pagar)
-- ------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS financial_payments (
    id BIGSERIAL PRIMARY KEY,
    conta_id BIGINT,
    parcela_id BIGINT,
    valor NUMERIC(19, 4) NOT NULL,
    data_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metodo_pagamento VARCHAR(20) NOT NULL,
    referencia VARCHAR(100),
    observacoes VARCHAR(500),
    operador VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE financial_payments
    ADD CONSTRAINT fk_financial_payments_conta
    FOREIGN KEY (conta_id) REFERENCES payable_accounts(id);

ALTER TABLE financial_payments
    ADD CONSTRAINT fk_financial_payments_parcela
    FOREIGN KEY (parcela_id) REFERENCES payable_installments(id);

CREATE INDEX IF NOT EXISTS idx_financial_payments_conta ON financial_payments(conta_id);
CREATE INDEX IF NOT EXISTS idx_financial_payments_parcela ON financial_payments(parcela_id);
CREATE INDEX IF NOT EXISTS idx_financial_payments_data ON financial_payments(data_hora);
CREATE INDEX IF NOT EXISTS idx_financial_payments_metodo ON financial_payments(metodo_pagamento);

ALTER TABLE financial_payments
    ADD CONSTRAINT chk_financial_payments_metodo_valid
    CHECK (metodo_pagamento IN ('DINHEIRO', 'PIX', 'TRANSFERENCIA', 'BOLETO', 'CARTAO', 'OUTRO'));

ALTER TABLE financial_payments
    ADD CONSTRAINT chk_financial_payments_valor_positive
    CHECK (valor > 0);

ALTER TABLE financial_payments
    ADD CONSTRAINT chk_financial_payments_status_valid
    CHECK (status IN ('CONFIRMED', 'REVERSED', 'PENDING'));

COMMENT ON TABLE financial_payments IS 'Pagamentos financeiros registrados para quitar contas a pagar';

COMMENT ON COLUMN financial_payments.metodo_pagamento IS 'DINHEIRO, PIX, TRANSFERENCIA, BOLETO, CARTAO, OUTRO';
COMMENT ON COLUMN financial_payments.status IS 'CONFIRMED, REVERSED, PENDING';