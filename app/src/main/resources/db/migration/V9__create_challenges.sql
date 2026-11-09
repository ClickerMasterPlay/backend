CREATE TABLE challenge_progress (
    user_id UUID PRIMARY KEY,
    total_clicks BIGINT NOT NULL DEFAULT 0,
    blocks_broken BIGINT NOT NULL DEFAULT 0,
    credits_earned BIGINT NOT NULL DEFAULT 0,
    credits_spent BIGINT NOT NULL DEFAULT 0,
    purchases_count BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_challenge_progress_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_challenge_progress_non_negative
        CHECK (
            total_clicks >= 0
            AND blocks_broken >= 0
            AND credits_earned >= 0
            AND credits_spent >= 0
            AND purchases_count >= 0
        )
);

CREATE TABLE claimed_challenges (
    user_id UUID NOT NULL,
    challenge_index INTEGER NOT NULL,
    reward BIGINT NOT NULL,
    claimed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, challenge_index),
    CONSTRAINT fk_claimed_challenges_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_claimed_challenge_index
        CHECK (challenge_index >= 0 AND challenge_index < 60),
    CONSTRAINT chk_claimed_challenge_reward CHECK (reward >= 0)
);

INSERT INTO challenge_progress (user_id)
SELECT id FROM users;

-- Preserve progress already accumulated before challenges were introduced.
UPDATE challenge_progress progress
SET total_clicks = source.total_clicks,
    blocks_broken = source.blocks_broken
FROM (
    SELECT user_id,
           COALESCE(SUM(clicks), 0) AS total_clicks,
           COUNT(*) FILTER (
               WHERE COALESCE((response_json ->> 'destroyed')::boolean, false)
           ) AS blocks_broken
    FROM processed_click_batches
    GROUP BY user_id
) source
WHERE progress.user_id = source.user_id;

UPDATE challenge_progress progress
SET credits_earned = source.credits_earned
FROM (
    SELECT user_id, COALESCE(SUM(amount), 0) AS credits_earned
    FROM ledger
    WHERE operation_type = 'CLICK_REWARD' AND amount > 0
    GROUP BY user_id
) source
WHERE progress.user_id = source.user_id;

UPDATE challenge_progress progress
SET credits_spent = source.credits_spent,
    purchases_count = source.purchases_count
FROM (
    SELECT user_id,
           COALESCE(SUM(price), 0) AS credits_spent,
           COUNT(*) AS purchases_count
    FROM purchases
    WHERE status = 'COMPLETED'
    GROUP BY user_id
) source
WHERE progress.user_id = source.user_id;

ALTER TABLE ledger DROP CONSTRAINT chk_ledger_operation_type;
ALTER TABLE ledger ADD CONSTRAINT chk_ledger_operation_type
    CHECK (operation_type IN (
        'PURCHASE', 'CLICK_REWARD', 'OFFLINE_REWARD', 'DEPOSIT',
        'CHALLENGE_REWARD'
    ));
