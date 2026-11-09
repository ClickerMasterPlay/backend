-- Лидерборд: снимки рейтинга для истории и отладки
CREATE TABLE leaderboard_snapshots (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    score BIGINT NOT NULL,
    rank INTEGER NOT NULL,
    credits BIGINT NOT NULL,
    total_resources_value BIGINT NOT NULL,
    click_power DOUBLE PRECISION NOT NULL,
    snapshot_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_leaderboard_snapshots_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    CONSTRAINT chk_leaderboard_score CHECK (score >= 0),
    CONSTRAINT chk_leaderboard_rank CHECK (rank > 0)
);

CREATE INDEX idx_leaderboard_snapshots_snapshot_at
    ON leaderboard_snapshots (snapshot_at DESC);

CREATE INDEX idx_leaderboard_snapshots_user_snapshot
    ON leaderboard_snapshots (user_id, snapshot_at DESC);

-- Материализованное представление текущего рейтинга (для быстрых чтений без Redis)
CREATE TABLE current_leaderboard (
    user_id UUID PRIMARY KEY,
    score BIGINT NOT NULL,
    rank INTEGER,
    credits BIGINT NOT NULL,
    total_resources_value BIGINT NOT NULL,
    click_power DOUBLE PRECISION NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_current_leaderboard_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    CONSTRAINT chk_current_leaderboard_score CHECK (score >= 0)
);

CREATE INDEX idx_current_leaderboard_rank
    ON current_leaderboard (rank ASC);