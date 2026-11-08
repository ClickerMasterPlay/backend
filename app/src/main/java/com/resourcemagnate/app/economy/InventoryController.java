package com.resourcemagnate.app.economy;

import com.resourcemagnate.app.economy.dto.InventoryResponse;
import com.resourcemagnate.app.economy.dto.MachineCollectionResponse;
import com.resourcemagnate.app.economy.dto.MachineStateResponse;
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

    @PostMapping("/machines/{machineCode}/install")
    public ResponseEntity<MachineStateResponse> installMachine(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable String machineCode
    ) {
        return ResponseEntity.ok(
                machineCollectionService.install(userId, machineCode)
        );
    }

    @PostMapping("/machines/{machineCode}/uninstall")
    public ResponseEntity<MachineStateResponse> uninstallMachine(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable String machineCode
    ) {
        return ResponseEntity.ok(
                machineCollectionService.uninstall(userId, machineCode)
        );
    }
}
