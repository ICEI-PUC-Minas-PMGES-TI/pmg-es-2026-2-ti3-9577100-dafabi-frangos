package com.dafabi.inventory.domain;

public enum StockMovementType {
    INITIAL_BALANCE,
    MANUAL_ADJUSTMENT,
    LOT_RECEIPT,
    PURCHASE,
    PURCHASE_CANCELLATION,
    COUNTER_SALE,
    INTEGRATED_SALE,
    INTEGRATED_CANCELLATION,
    REFUND
}
