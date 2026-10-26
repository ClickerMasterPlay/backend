CREATE TABLE processed_click_batches (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id UUID NOT NULL,
    client_batch_id UUID NOT NULL,
    clicks INTEGER NOT NULL,
    response_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_click_batch_user_id
        UNIQUE (user_id, client_batch_id),

    CONSTRAINT fk_click_batch_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_processed_click_batches_clicks
        CHECK (clicks BETWEEN 1 AND 50)
);

CREATE INDEX idx_click_batches_created_at
    ON processed_click_batches (created_at);


CREATE TABLE tools (
    code VARCHAR(32) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    power_multiplier DOUBLE PRECISION NOT NULL DEFAULT 1.0,

    CONSTRAINT chk_tools_power_multiplier
        CHECK (
            power_multiplier > 0
            AND power_multiplier < 'Infinity'::DOUBLE PRECISION
        )
);

-- Начальный инструмент без бонуса к урону.
INSERT INTO tools (code, name, power_multiplier)
VALUES ('basic_pickaxe', 'Basic Pickaxe', 1.0);

SELECT DISTINCT equipped_tool_code
FROM player_progress
WHERE equipped_tool_code IS NOT NULL;

ALTER TABLE player_progress
    ADD CONSTRAINT fk_player_progress_equipped_tool
    FOREIGN KEY (equipped_tool_code)
    REFERENCES tools(code);