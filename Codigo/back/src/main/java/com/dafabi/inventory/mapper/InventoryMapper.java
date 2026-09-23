package com.dafabi.inventory.mapper;

import com.dafabi.inventory.domain.InventoryLot;
import com.dafabi.inventory.domain.LotStatus;
import com.dafabi.inventory.domain.StockMovement;
import com.dafabi.inventory.dto.InventoryLotResponse;
import com.dafabi.inventory.dto.InventoryPositionResponse;
import com.dafabi.inventory.dto.StockMovementResponse;
import com.dafabi.products.domain.Product;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class InventoryMapper {

    public StockMovementResponse toMovementResponse(StockMovement movement) {
        if (movement == null) return null;
        return new StockMovementResponse(
                movement.getId(),
                movement.getProduct().getId(),
                movement.getProduct().getName(),
                movement.getOccurredAt(),
                movement.getMovementType(),
                movement.getPreviousQuantity(),
                movement.getNewQuantity(),
                movement.getQuantityDelta(),
                movement.getOrigin(),
                movement.getPerformedBy(),
                movement.getReason());
    }

    public InventoryPositionResponse toPositionResponse(Product product, StockMovement lastMovement) {
        int stockQuantity = product.getStock() == null ? 0 : product.getStock().getQuantity();
        String categoryName = product.getCategory() == null ? "Sem categoria" : product.getCategory().getName();
        return new InventoryPositionResponse(
                product.getId(),
                product.getName(),
                categoryName,
                stockQuantity,
                product.getUnit(),
                product.getCurrentCost(),
                product.isPerishable(),
                product.getStatus(),
                toMovementResponse(lastMovement));
    }

    public InventoryLotResponse toLotResponse(InventoryLot lot) {
        return new InventoryLotResponse(
                lot.getId(),
                lot.getProduct().getId(),
                lot.getProduct().getName(),
                lot.getBatch(),
                lot.getReceivedAt(),
                lot.getExpiry(),
                lot.getQuantity(),
                resolveStatus(lot));
    }

    private LotStatus resolveStatus(InventoryLot lot) {
        if (lot.getQuantity() == 0) return LotStatus.USED;
        return lot.getExpiry().isBefore(LocalDate.now()) ? LotStatus.EXPIRED : LotStatus.VALID;
    }
}
