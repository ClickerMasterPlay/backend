-- Добавляем новые инструменты (gold_pickaxe) и апгрейды
INSERT INTO tools (code, name, power_multiplier)
VALUES
    ('wooden_pickaxe', 'Wooden Pickaxe', 1.2),
    ('iron_pickaxe', 'Iron Pickaxe', 1.8),
    ('gold_pickaxe', 'Gold Pickaxe', 3.0)
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    power_multiplier = EXCLUDED.power_multiplier;