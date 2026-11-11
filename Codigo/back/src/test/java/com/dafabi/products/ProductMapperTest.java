package com.dafabi.products;

import com.dafabi.products.domain.Category;
import com.dafabi.products.domain.Product;
import com.dafabi.products.domain.ProductStatus;
import com.dafabi.products.dto.ProductListResponse;
import com.dafabi.products.mapper.ProductMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMapperTest {

    @Test
    void shouldIncludeCurrentCostInProductListResponse() {
        Category category = new Category(UUID.randomUUID(), "Frangos", true);
        Product product = new Product(UUID.randomUUID(), category, "Frango assado",
                new BigDecimal("42.90"), new BigDecimal("27.50"), "UN", null,
                false, true, ProductStatus.ACTIVE, null, null);

        ProductListResponse response = new ProductMapper().toListResponse(product);

        assertThat(response.currentCost()).isEqualByComparingTo("27.50");
    }
}
