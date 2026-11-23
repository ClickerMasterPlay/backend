ALTER TABLE ledger DROP CONSTRAINT chk_ledger_resource_reward;

ALTER TABLE ledger ADD CONSTRAINT chk_ledger_resource_amount
    CHECK (
        (resource_code IS NULL AND resource_amount IS NULL)
        OR
        (resource_code IS NOT NULL AND resource_amount IS NOT NULL
            AND resource_amount <> 0)
    );
