package com.dafabi.inventory.mapper;

import com.dafabi.inventory.application.StockAvailability;
import com.dafabi.shared.Quantities;
import java.math.BigDecimal;
import com.dafabi.inventory.domain.InventoryLot;
import com.dafabi.inventory.domain.LotStatus;
import com.dafabi.inventory.domain.StockMovement;
import com.dafabi.inventory.dto.InventoryLotResponse;
import com.dafabi.inventory.dto.InventoryPositionResponse;
import com.dafabi.inventory.dto.StockMovementResponse;
import com.dafabi.products.domain.Product;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

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
                Quantities.forJson(movement.getPreviousQuantity()),
                Quantities.forJson(movement.getNewQuantity()),
                Quantities.forJson(movement.getQuantityDelta()),
                movement.getOrigin(),
                movement.getPerformedBy(),
                movement.getReason());
    }

    public InventoryPositionResponse toPositionResponse(Product product, StockMovement lastMovement,
                                                        StockAvailability availability) {
        BigDecimal stockQuantity = product.getStock() == null ? Quantities.ZERO : product.getStock().getQuantity();
        String categoryName = product.getCategory() == null ? "Sem categoria" : product.getCategory().getName();
        return new InventoryPositionResponse(
                product.getId(),
                product.getName(),
                categoryName,
                Quantities.forJson(stockQuantity),
                product.getUnit(),
                product.getCurrentCost(),
                product.isPerishable(),
                product.getStatus(),
                toMovementResponse(lastMovement),
                Quantities.forJson(availability.sellable()),
                Quantities.forJson(availability.expired()),
                Quantities.forJson(availability.untracked()));
    }

    public InventoryLotResponse toLotResponse(InventoryLot lot) {
        return new InventoryLotResponse(
                lot.getId(),
                lot.getProduct().getId(),
                lot.getProduct().getName(),
                lot.getBatch(),
                lot.getReceivedAt(),
                lot.getExpiry(),
                Quantities.forJson(lot.getQuantity()),
                resolveStatus(lot));
    }

    private LotStatus resolveStatus(InventoryLot lot) {
        if (lot.getQuantity().signum() == 0) return LotStatus.USED;
        return lot.getExpiry().isBefore(LocalDate.now(ZoneId.of("America/Sao_Paulo"))) ? LotStatus.EXPIRED : LotStatus.VALID;
    }
}
