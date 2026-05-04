-- =============================================
-- Schema: orchestrator
-- =============================================
CREATE SCHEMA IF NOT EXISTS orchestrator;

-- Stores each saga instance
CREATE TABLE orchestrator.saga_instance (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    saga_type       VARCHAR(100) NOT NULL,
    payload         JSONB NOT NULL,
    current_step    INT NOT NULL DEFAULT 0,
    status          VARCHAR(30) NOT NULL DEFAULT 'STARTED',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    version         INT NOT NULL DEFAULT 0
);

-- Audit log for every step action
CREATE TABLE orchestrator.saga_step_log (
    id              BIGSERIAL PRIMARY KEY,
    saga_id         UUID NOT NULL REFERENCES orchestrator.saga_instance(id),
    step_index      INT NOT NULL,
    step_name       VARCHAR(100) NOT NULL,
    action_type     VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    payload         JSONB,
    idempotency_key UUID NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_saga_step_log_saga_id ON orchestrator.saga_step_log(saga_id);
CREATE INDEX idx_saga_instance_status ON orchestrator.saga_instance(status);
CREATE INDEX idx_saga_instance_updated ON orchestrator.saga_instance(updated_at);

-- =============================================
-- Schema: inventory
-- =============================================
CREATE SCHEMA IF NOT EXISTS inventory;

CREATE TABLE inventory.product (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    sku             VARCHAR(100) UNIQUE NOT NULL,
    available_qty   INT NOT NULL DEFAULT 0
);

CREATE TABLE inventory.inventory_reservation (
    id                  BIGSERIAL PRIMARY KEY,
    reservation_id      UUID UNIQUE NOT NULL,
    product_id          BIGINT NOT NULL REFERENCES inventory.product(id),
    quantity            INT NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'RESERVED',
    idempotency_key     UUID UNIQUE NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Seed sample products
INSERT INTO inventory.product (name, sku, available_qty) VALUES
    ('Wireless Mouse', 'SKU-MOUSE-001', 100),
    ('Mechanical Keyboard', 'SKU-KB-002', 50),
    ('USB-C Hub', 'SKU-HUB-003', 200);

-- =============================================
-- Schema: payment
-- =============================================
CREATE SCHEMA IF NOT EXISTS payment;

CREATE TABLE payment.payment_transaction (
    id              BIGSERIAL PRIMARY KEY,
    txn_id          UUID UNIQUE NOT NULL,
    saga_id         UUID NOT NULL,
    user_id         VARCHAR(100) NOT NULL,
    amount          DECIMAL(12,2) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'CHARGED',
    idempotency_key UUID UNIQUE NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
