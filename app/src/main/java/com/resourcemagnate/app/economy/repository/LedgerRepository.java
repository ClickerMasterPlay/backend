package com.resourcemagnate.app.economy.repository;

import com.resourcemagnate.app.economy.model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LedgerRepository extends JpaRepository<LedgerEntry, UUID> {
}
