-- Каталог типов блоков
CREATE TABLE block_types (
    code VARCHAR(32) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    rarity VARCHAR(16) NOT NULL,
    max_hp INTEGER NOT NULL,
    reward_credits BIGINT NOT NULL DEFAULT 0,
    reward_resource VARCHAR(32),
    reward_amount INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT chk_block_types_rarity
        CHECK (rarity IN (
            'COMMON',
            'UNCOMMON',
            'RARE',
            'EPIC',
            'LEGENDARY'
        )),

    CONSTRAINT chk_block_types_max_hp
        CHECK (max_hp > 0),

    CONSTRAINT chk_block_types_reward_credits
        CHECK (reward_credits >= 0),

    CONSTRAINT chk_block_types_reward_amount
        CHECK (reward_amount >= 0)

);

-- Единственный текущий блок игрока
CREATE TABLE current_blocks (
    user_id UUID PRIMARY KEY,
    block_type VARCHAR(32) NOT NULL,
    current_hp INTEGER NOT NULL,
    max_hp INTEGER NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_current_blocks_block_type
        FOREIGN KEY (block_type)
        REFERENCES block_types (code)
        ON DELETE RESTRICT,

    CONSTRAINT chk_current_blocks_hp
        CHECK (
            max_hp > 0
            AND current_hp >= 0
            AND current_hp <= max_hp
        ),

    CONSTRAINT chk_current_blocks_max_hp
        CHECK (max_hp > 0),

    CONSTRAINT fk_current_blocks_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE
);

-- Начальные типы блоков
INSERT INTO block_types (
    code,
    name,
    rarity,
    max_hp,
    reward_credits,
    reward_resource,
    reward_amount
)
VALUES
    ('stone', 'Камень', 'COMMON', 10, 1, 'stone', 1),
    ('iron', 'Железо', 'UNCOMMON', 25, 3, 'iron', 1),
    ('gold', 'Золото', 'RARE', 50, 10, 'gold', 1),
    ('diamond', 'Алмаз', 'EPIC', 100, 25, 'diamond', 1),
    ('netherite', 'Незерит', 'LEGENDARY', 250, 100, 'netherite', 1);