package com.dafabi.sales.mapper;

import com.dafabi.sales.domain.Sale;
import com.dafabi.sales.domain.SaleRefund;
import com.dafabi.sales.dto.SaleRefundEligibilityResponse;
import com.dafabi.sales.dto.SaleRefundResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class SaleRefundMapper {

    private final SaleMapper saleMapper;

    public SaleRefundMapper(SaleMapper saleMapper) {
        this.saleMapper = saleMapper;
    }

    public SaleRefundResponse toResponse(SaleRefund refund) {
        if (refund == null) return null;

        Sale sale = refund.getSale();
        return new SaleRefundResponse(
                refund.getId(),
                sale.getId(),
                sale.getSaleNumber(),
                sale.getStatus(),
                sale.getPaymentMethod(),
                normalize(refund.getAmount()),
                normalize(refund.getCashAmount()),
                refund.getCashRegisterId(),
                refund.getReason(),
                refund.getRefundedById(),
                refund.getRefundedByName(),
                refund.getRefundedAt()
        );
    }

    public SaleRefundEligibilityResponse toEligibilityResponse(Sale sale,
                                                               BigDecimal cashAmountToReverse,
                                                               String ineligibilityCode,
                                                               String ineligibilityReason,
                                                               SaleRefund refund) {
        if (sale == null) return null;

        return new SaleRefundEligibilityResponse(
                sale.getId(),
                sale.getSaleNumber(),
                sale.getDateTime(),
                sale.getOrigin(),
                sale.getPaymentMethod(),
                sale.getStatus(),
                normalize(sale.getTotal()),
                normalize(cashAmountToReverse),
                ineligibilityCode == null,
                ineligibilityCode,
                ineligibilityReason,
                sale.getItems().stream()
                        .map(saleMapper::toItemResponse)
                        .toList(),
                toResponse(refund)
        );
    }

    private BigDecimal normalize(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }
}
