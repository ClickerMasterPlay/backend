ALTER TABLE ledger
    ADD COLUMN resource_code VARCHAR(32),
    ADD COLUMN resource_amount BIGINT;

ALTER TABLE ledger ADD CONSTRAINT chk_ledger_resource_reward
    CHECK (
        (resource_code IS NULL AND resource_amount IS NULL)
        OR
        (resource_code IS NOT NULL AND resource_amount IS NOT NULL
            AND resource_amount > 0)
    );

ALTER TABLE purchases DROP CONSTRAINT chk_purchases_status;
ALTER TABLE purchases ADD CONSTRAINT chk_purchases_status
    CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED'));
