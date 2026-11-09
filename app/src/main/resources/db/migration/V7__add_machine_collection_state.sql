ALTER TABLE inventory_machines
    ADD COLUMN last_collected_at TIMESTAMPTZ;

UPDATE inventory_machines
SET last_collected_at = acquired_at;

ALTER TABLE inventory_machines
    ALTER COLUMN last_collected_at SET NOT NULL,
    ALTER COLUMN last_collected_at SET DEFAULT CURRENT_TIMESTAMP;
