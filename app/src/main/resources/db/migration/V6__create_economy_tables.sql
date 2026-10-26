CREATE TABLE purchases (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    product_type VARCHAR(32) NOT NULL,
    product_id VARCHAR(100) NOT NULL,
    price BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    idempotency_key UUID NOT NULL,
    response_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_purchases_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_purchases_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT chk_purchases_price CHECK (price >= 0),
    CONSTRAINT chk_purchases_product_type
        CHECK (product_type IN ('CLICK_UPGRADE', 'TOOL', 'MACHINE')),
    CONSTRAINT chk_purchases_status
        CHECK (status IN ('COMPLETED', 'FAILED'))
);

CREATE INDEX idx_purchases_user_created_at
    ON purchases (user_id, created_at DESC);

CREATE TABLE ledger (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    operation_type VARCHAR(32) NOT NULL,
    amount BIGINT NOT NULL,
    product_type VARCHAR(32),
    product_id VARCHAR(100),
    purchase_id UUID,
    balance_after BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ledger_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ledger_purchase
        FOREIGN KEY (purchase_id) REFERENCES purchases(id) ON DELETE SET NULL,
    CONSTRAINT chk_ledger_operation_type
        CHECK (operation_type IN (
            'PURCHASE', 'CLICK_REWARD', 'OFFLINE_REWARD', 'DEPOSIT'
        )),
    CONSTRAINT chk_ledger_product_type
        CHECK (
            product_type IS NULL OR
            product_type IN ('CLICK_UPGRADE', 'TOOL', 'MACHINE')
        ),
    CONSTRAINT chk_ledger_balance_after CHECK (balance_after >= 0)
);

CREATE INDEX idx_ledger_user_created_at
    ON ledger (user_id, created_at DESC);

CREATE TABLE inventory_tools (
    user_id UUID NOT NULL,
    tool_code VARCHAR(32) NOT NULL,
    acquired_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, tool_code),
    CONSTRAINT fk_inventory_tools_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_tools_tool
        FOREIGN KEY (tool_code) REFERENCES tools(code) ON DELETE RESTRICT
);

CREATE TABLE inventory_machines (
    user_id UUID NOT NULL,
    machine_code VARCHAR(32) NOT NULL,
    acquired_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, machine_code),
    CONSTRAINT fk_inventory_machines_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

INSERT INTO tools (code, name, power_multiplier)
VALUES
    ('wooden_pickaxe', 'Wooden Pickaxe', 1.2),
    ('iron_pickaxe', 'Iron Pickaxe', 1.8);
