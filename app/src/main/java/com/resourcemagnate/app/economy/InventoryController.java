package com.resourcemagnate.app.economy;

import com.resourcemagnate.app.economy.dto.InventoryResponse;
import com.resourcemagnate.app.economy.dto.MachineCollectionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final MachineCollectionService machineCollectionService;

    @GetMapping
    public ResponseEntity<InventoryResponse> inventory(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId
    ) {
        return ResponseEntity.ok(inventoryService.getInventory(userId));
    }

    @PostMapping("/machines/collect")
    public ResponseEntity<MachineCollectionResponse> collectMachines(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId
    ) {
        return ResponseEntity.ok(machineCollectionService.collect(userId));
    }
}
