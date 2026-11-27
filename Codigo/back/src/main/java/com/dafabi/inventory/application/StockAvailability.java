package com.dafabi.inventory.application;

import com.dafabi.shared.Quantities;

import java.math.BigDecimal;

/**
 * Disponibilidade real de um produto. Para perecíveis, só lotes dentro da validade podem
 * ser vendidos; saldo em lote vencido ou sem lote aparece separado para regularização.
 */
public record StockAvailability(BigDecimal sellable, BigDecimal expired, BigDecimal untracked) {

    public static StockAvailability simple(BigDecimal stock) {
        return new StockAvailability(Quantities.of(stock), Quantities.ZERO, Quantities.ZERO);
    }

    public static StockAvailability perishable(BigDecimal stock, BigDecimal validInLots, BigDecimal expiredInLots) {
        BigDecimal total = Quantities.of(stock);
        BigDecimal valid = Quantities.min(Quantities.of(validInLots), total.max(Quantities.ZERO));
        BigDecimal expired = Quantities.of(expiredInLots);
        BigDecimal untracked = total.subtract(Quantities.of(validInLots)).subtract(expired).max(Quantities.ZERO);
        return new StockAvailability(valid, expired, untracked);
    }
}
