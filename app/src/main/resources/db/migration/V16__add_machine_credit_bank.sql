ALTER TABLE inventory_machines
    ADD COLUMN banked_credits BIGINT NOT NULL DEFAULT 0;

ALTER TABLE inventory_machines
    ADD CONSTRAINT chk_inventory_machines_banked_credits
        CHECK (banked_credits >= 0);
