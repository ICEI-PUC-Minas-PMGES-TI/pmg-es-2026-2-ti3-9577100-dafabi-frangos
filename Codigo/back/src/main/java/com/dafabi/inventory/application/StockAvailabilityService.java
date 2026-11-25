package com.dafabi.inventory.application;

import com.dafabi.inventory.repository.InventoryLotRepository;
import com.dafabi.products.domain.Product;
import com.dafabi.shared.Quantities;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Calcula, em uma única consulta, o saldo vendável, vencido e sem lote dos produtos. */
@Service
public class StockAvailabilityService {

    private static final ZoneId STORE_ZONE = ZoneId.of("America/Sao_Paulo");

    private final InventoryLotRepository inventoryLotRepository;

    public StockAvailabilityService(InventoryLotRepository inventoryLotRepository) {
        this.inventoryLotRepository = inventoryLotRepository;
    }

    @Transactional(readOnly = true)
    public Map<UUID, StockAvailability> forProducts(Collection<Product> products) {
        List<UUID> perishableIds = products.stream().filter(Product::isPerishable).map(Product::getId).toList();
        Map<UUID, BigDecimal[]> lotTotals = new HashMap<>();
        if (!perishableIds.isEmpty()) {
            for (Object[] row : inventoryLotRepository.summarizeAvailableByProduct(perishableIds, today())) {
                lotTotals.put((UUID) row[0], new BigDecimal[] {toDecimal(row[1]), toDecimal(row[2])});
            }
        }
        Map<UUID, StockAvailability> result = new HashMap<>();
        for (Product product : products) {
            BigDecimal stock = product.getStock() == null ? Quantities.ZERO : product.getStock().getQuantity();
            if (!product.isPerishable()) {
                result.put(product.getId(), StockAvailability.simple(stock));
                continue;
            }
            BigDecimal[] totals = lotTotals.getOrDefault(product.getId(), new BigDecimal[] {Quantities.ZERO, Quantities.ZERO});
            result.put(product.getId(), StockAvailability.perishable(stock, totals[0], totals[1]));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public StockAvailability forProduct(Product product) {
        return forProducts(List.of(product)).get(product.getId());
    }

    private static LocalDate today() {
        return LocalDate.now(STORE_ZONE);
    }

    private static BigDecimal toDecimal(Object value) {
        if (value == null) return Quantities.ZERO;
        if (value instanceof BigDecimal decimal) return Quantities.of(decimal);
        return Quantities.of(new BigDecimal(value.toString()));
    }
}
