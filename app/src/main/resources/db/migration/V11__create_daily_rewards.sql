CREATE TABLE daily_reward_progress (
    user_id UUID PRIMARY KEY,
    cycle_day INTEGER NOT NULL DEFAULT 1,
    last_claimed_date DATE,
    total_claims BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_daily_reward_progress_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_daily_reward_cycle_day CHECK (cycle_day BETWEEN 1 AND 7),
    CONSTRAINT chk_daily_reward_total_claims CHECK (total_claims >= 0)
);

CREATE TABLE daily_reward_claims (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    reward_date DATE NOT NULL,
    cycle_day INTEGER NOT NULL,
    reward_json JSONB NOT NULL,
    response_json JSONB NOT NULL,
    idempotency_key UUID NOT NULL,
    claimed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_daily_reward_claim_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_daily_reward_user_date UNIQUE (user_id, reward_date),
    CONSTRAINT uq_daily_reward_idempotency UNIQUE (idempotency_key),
    CONSTRAINT chk_daily_reward_claim_cycle_day CHECK (cycle_day BETWEEN 1 AND 7)
);

CREATE INDEX idx_daily_reward_claims_user_date
    ON daily_reward_claims (user_id, reward_date DESC);

INSERT INTO daily_reward_progress (user_id)
SELECT id FROM users;

ALTER TABLE ledger DROP CONSTRAINT chk_ledger_operation_type;
ALTER TABLE ledger ADD CONSTRAINT chk_ledger_operation_type
    CHECK (operation_type IN (
        'PURCHASE', 'CLICK_REWARD', 'OFFLINE_REWARD', 'DEPOSIT',
        'CHALLENGE_REWARD', 'DAILY_REWARD'
    ));
