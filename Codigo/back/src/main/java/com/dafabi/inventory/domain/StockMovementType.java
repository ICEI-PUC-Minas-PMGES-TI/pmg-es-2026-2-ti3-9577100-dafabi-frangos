package com.dafabi.inventory.domain;

public enum StockMovementType {
    INITIAL_BALANCE,
    MANUAL_ADJUSTMENT,
    LOT_RECEIPT,
    PURCHASE,
    COUNTER_SALE,
    INTEGRATED_SALE,
    INTEGRATED_CANCELLATION,
    REFUND
}
