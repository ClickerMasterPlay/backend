package com.resourcemagnate.app.economy.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class InventoryMachineId implements Serializable {
    private UUID userId;
    private String machineCode;
}
