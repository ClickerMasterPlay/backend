ALTER TABLE inventory_machines
    ADD COLUMN is_installed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN installed_at TIMESTAMPTZ;

-- Machines bought before installation mechanics existed were producing already.
UPDATE inventory_machines
SET is_installed = TRUE,
    installed_at = acquired_at;
