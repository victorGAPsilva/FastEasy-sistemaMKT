-- Flyway Migration: V1.0.4__create_cash_tables.sql
-- Módulo de gestão de caixa: cadastro de caixas, sessões e movimentações.
-- Data: 2026-10-09

-- ------------------------------------------------------------------
-- Cadastro de caixas (pontos de atendimento, ex.: "Caixa 1")
-- ------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cash_registers (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_cash_registers_nome ON cash_registers(nome);
CREATE INDEX IF NOT EXISTS idx_cash_registers_status ON cash_registers(status);

ALTER TABLE cash_registers
    ADD CONSTRAINT chk_cash_registers_status_valid
    CHECK (status IN ('ATIVO', 'INATIVO'));

-- ------------------------------------------------------------------
-- Sessões de caixa (abertura, movimentação e fechamento)
-- ------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cash_sessions (
    id BIGSERIAL PRIMARY KEY,
    caixa_id BIGINT NOT NULL,
    operador VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    data_abertura TIMESTAMP NOT NULL,
    data_fechamento TIMESTAMP,
    saldo_inicial NUMERIC(19, 4) NOT NULL,
    saldo_esperado NUMERIC(19, 4),
    total_contado NUMERIC(19, 4),
    diferenca NUMERIC(19, 4),
    observacoes_abertura VARCHAR(255),
    observacoes_fechamento VARCHAR(500),
    operador_fechamento VARCHAR(100),
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE cash_sessions
    ADD CONSTRAINT fk_cash_sessions_caixa
    FOREIGN KEY (caixa_id) REFERENCES cash_registers(id);

CREATE INDEX IF NOT EXISTS idx_cash_sessions_caixa ON cash_sessions(caixa_id);
CREATE INDEX IF NOT EXISTS idx_cash_sessions_status ON cash_sessions(status);
CREATE INDEX IF NOT EXISTS idx_cash_sessions_abertura ON cash_sessions(data_abertura);
CREATE INDEX IF NOT EXISTS idx_cash_sessions_fechamento ON cash_sessions(data_fechamento);

-- Garante no banco, no PostgreSQL, no máximo uma sessão aberta por caixa.
-- Complementada pela validação transacional com bloqueio na aplicação.
CREATE UNIQUE INDEX IF NOT EXISTS uq_cash_sessions_caixa_aberta
    ON cash_sessions(caixa_id) WHERE status = 'OPEN';

ALTER TABLE cash_sessions
    ADD CONSTRAINT chk_cash_sessions_status_valid
    CHECK (status IN ('OPEN', 'CLOSED'));

ALTER TABLE cash_sessions
    ADD CONSTRAINT chk_cash_sessions_saldo_inicial_non_negative
    CHECK (saldo_inicial >= 0);

ALTER TABLE cash_sessions
    ADD CONSTRAINT chk_cash_sessions_saldo_esperado_non_negative
    CHECK (saldo_esperado IS NULL OR saldo_esperado >= 0);

ALTER TABLE cash_sessions
    ADD CONSTRAINT chk_cash_sessions_total_contado_non_negative
    CHECK (total_contado IS NULL OR total_contado >= 0);

ALTER TABLE cash_sessions
    ADD CONSTRAINT chk_cash_sessions_fechamento_consistente
    CHECK ((status = 'OPEN' AND data_fechamento IS NULL)
           OR (status = 'CLOSED' AND data_fechamento IS NOT NULL));

-- ------------------------------------------------------------------
-- Movimentações financeiras manuais (suprimentos, sangrias e ajustes)
-- ------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cash_movements (
    id BIGSERIAL PRIMARY KEY,
    sessao_id BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    valor NUMERIC(19, 4) NOT NULL,
    motivo VARCHAR(255) NOT NULL,
    operador VARCHAR(100),
    data_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE cash_movements
    ADD CONSTRAINT fk_cash_movements_sessao
    FOREIGN KEY (sessao_id) REFERENCES cash_sessions(id);

CREATE INDEX IF NOT EXISTS idx_cash_movements_sessao ON cash_movements(sessao_id);
CREATE INDEX IF NOT EXISTS idx_cash_movements_tipo ON cash_movements(tipo);
CREATE INDEX IF NOT EXISTS idx_cash_movements_data ON cash_movements(data_hora);

ALTER TABLE cash_movements
    ADD CONSTRAINT chk_cash_movements_tipo_valid
    CHECK (tipo IN ('SUPPLY', 'WITHDRAWAL', 'OTHER_IN', 'OTHER_OUT'));

ALTER TABLE cash_movements
    ADD CONSTRAINT chk_cash_movements_valor_positive
    CHECK (valor > 0);

-- ------------------------------------------------------------------
-- Associação entre vendas e sessões de caixa
-- ------------------------------------------------------------------
ALTER TABLE sales
    ADD COLUMN IF NOT EXISTS cash_session_id BIGINT;

ALTER TABLE sales
    ADD CONSTRAINT fk_sales_cash_session
    FOREIGN KEY (cash_session_id) REFERENCES cash_sessions(id);

CREATE INDEX IF NOT EXISTS idx_sales_cash_session ON sales(cash_session_id);
