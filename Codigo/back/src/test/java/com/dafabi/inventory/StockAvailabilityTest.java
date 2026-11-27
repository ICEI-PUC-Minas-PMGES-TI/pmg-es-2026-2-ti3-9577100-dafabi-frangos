package com.dafabi.inventory;

import com.dafabi.inventory.application.StockAvailability;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class StockAvailabilityTest {

    @Test
    void perishableSellsOnlyValidLotsAndSeparatesExpiredAndUntrackedStock() {
        StockAvailability availability = StockAvailability.perishable(
                new BigDecimal("176"), BigDecimal.ZERO, new BigDecimal("30"));

        assertThat(availability.sellable()).isEqualByComparingTo("0");
        assertThat(availability.expired()).isEqualByComparingTo("30");
        assertThat(availability.untracked()).isEqualByComparingTo("146");
    }

    @Test
    void perishableWithValidLotsCoveringStockHasNothingToRegularize() {
        StockAvailability availability = StockAvailability.perishable(
                new BigDecimal("4"), new BigDecimal("4"), BigDecimal.ZERO);

        assertThat(availability.sellable()).isEqualByComparingTo("4");
        assertThat(availability.untracked()).isEqualByComparingTo("0");
    }

    @Test
    void nonPerishableSellsTheWholeBalance() {
        StockAvailability availability = StockAvailability.simple(new BigDecimal("12"));

        assertThat(availability.sellable()).isEqualByComparingTo("12");
        assertThat(availability.expired()).isEqualByComparingTo("0");
    }
}
