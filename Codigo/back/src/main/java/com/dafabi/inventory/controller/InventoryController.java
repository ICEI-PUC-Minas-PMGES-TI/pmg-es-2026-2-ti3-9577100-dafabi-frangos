package com.dafabi.inventory.controller;

import com.dafabi.inventory.application.InventoryService;
import com.dafabi.inventory.dto.CreateInventoryLotRequest;
import com.dafabi.inventory.dto.InventoryLotResponse;
import com.dafabi.inventory.dto.InventoryPositionResponse;
import com.dafabi.inventory.dto.StockAdjustmentRequest;
import com.dafabi.inventory.dto.StockMovementResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<InventoryPositionResponse> findPositions(@RequestParam(required = false) String query) {
        return inventoryService.findPositions(query);
    }

    @GetMapping("/movements")
    public List<StockMovementResponse> findMovements(@RequestParam(required = false) UUID productId) {
        return inventoryService.findMovements(productId);
    }

    @PostMapping("/adjustments")
    public ResponseEntity<StockMovementResponse> adjust(@Valid @RequestBody StockAdjustmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.adjust(request));
    }

    @GetMapping("/lots")
    public List<InventoryLotResponse> findLots() {
        return inventoryService.findLots();
    }

    @PostMapping("/lots")
    public ResponseEntity<InventoryLotResponse> createLot(@Valid @RequestBody CreateInventoryLotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.createLot(request));
    }
}
