-- Update block rewards: base = 1 credit per click (reward_credits = max_hp)
UPDATE block_types SET reward_credits = max_hp WHERE code IN ('stone', 'iron', 'gold', 'diamond', 'netherite');