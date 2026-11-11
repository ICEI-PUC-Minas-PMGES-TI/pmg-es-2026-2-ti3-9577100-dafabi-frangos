package com.dafabi.integrations.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

public record ParsedOrderRow(
        String externalStoreId, String storeName, String externalOrderId, OffsetDateTime orderAt,
        String rawStatus, String normalizedStatus, String shiftName, BigDecimal itemsValue,
        BigDecimal customerPaid, BigDecimal customerDeliveryFee, BigDecimal ifoodPromotion,
        BigDecimal storePromotion, BigDecimal networkPromotion, BigDecimal serviceFee,
        BigDecimal feesAndCommissions, BigDecimal netReported, String paymentMethodRaw,
        String deliveryType, String logisticsProduct, String salesChannel, Map<String, String> sourceValues) {
}
