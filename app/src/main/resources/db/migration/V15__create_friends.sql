-- Friends table: bilateral friendship with invitation flow
CREATE TABLE friends (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    friend_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    invited_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_friend_pair UNIQUE (LEAST(user_id, friend_id), GREATEST(user_id, friend_id)),
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'ACCEPTED', 'BLOCKED')),
    CONSTRAINT chk_not_self CHECK (user_id != friend_id)
);

CREATE INDEX idx_friends_user ON friends(user_id, status);
CREATE INDEX idx_friends_friend ON friends(friend_id, status);

-- Trigger to prevent duplicate invitations
CREATE OR REPLACE FUNCTION prevent_duplicate_friend()
RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM friends
        WHERE LEAST(user_id, friend_id) = LEAST(NEW.user_id, NEW.friend_id)
          AND GREATEST(user_id, friend_id) = GREATEST(NEW.user_id, NEW.friend_id)
    ) THEN
        RAISE EXCEPTION 'Friend relationship already exists';
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trg_prevent_duplicate_friend
    BEFORE INSERT ON friends
    FOR EACH ROW EXECUTE FUNCTION prevent_duplicate_friend();