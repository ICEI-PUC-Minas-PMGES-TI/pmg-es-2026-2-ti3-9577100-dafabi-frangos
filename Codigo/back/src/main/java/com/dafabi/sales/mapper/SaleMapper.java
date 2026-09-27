package com.dafabi.sales.mapper;

import com.dafabi.sales.domain.Sale;
import com.dafabi.sales.domain.SaleItem;
import com.dafabi.sales.dto.SaleItemResponse;
import com.dafabi.sales.dto.SaleResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class SaleMapper {

    public SaleResponse toResponse(Sale sale) {
        if (sale == null) return null;

        return new SaleResponse(
                sale.getId(),
                sale.getSaleNumber(),
                sale.getCashRegisterId(),
                sale.getOperatorId(),
                sale.getDateTime(),
                sale.getOrigin(),
                sale.getPaymentMethod(),
                normalize(sale.getSubtotal()),
                normalize(sale.getTotal()),
                sale.getStatus(),
                sale.getItems().stream()
                        .map(this::toItemResponse)
                        .toList()
        );
    }

    public SaleItemResponse toItemResponse(SaleItem item) {
        if (item == null) return null;

        return new SaleItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                normalize(item.getUnitPrice()),
                normalize(item.getSubtotal())
        );
    }

    private BigDecimal normalize(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }
}
