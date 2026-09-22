package com.dafabi.inventory;

import com.dafabi.inventory.dto.CreateInventoryLotRequest;
import com.dafabi.inventory.dto.StockAdjustmentRequest;
import com.dafabi.inventory.repository.InventoryLotRepository;
import com.dafabi.inventory.repository.StockMovementRepository;
import com.dafabi.products.domain.Category;
import com.dafabi.products.domain.Product;
import com.dafabi.products.domain.ProductStatus;
import com.dafabi.products.domain.ProductStock;
import com.dafabi.products.repository.CategoryRepository;
import com.dafabi.products.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private InventoryLotRepository inventoryLotRepository;
    @Autowired private StockMovementRepository stockMovementRepository;

    private Product product;

    @BeforeEach
    void setUp() {
        stockMovementRepository.deleteAll();
        inventoryLotRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        Category category = categoryRepository.save(new Category(UUID.randomUUID(), "Frangos", true));
        product = new Product(UUID.randomUUID(), category, "Frango assado tradicional",
                new BigDecimal("42.90"), new BigDecimal("24.50"), "UN", null,
                true, true, ProductStatus.ACTIVE, null, null);
        product.setStock(new ProductStock(product, 5));
        product = productRepository.save(product);
    }

    @Test
    @DisplayName("POST /inventory/adjustments deve atualizar o saldo e registrar o histórico")
    void shouldAdjustStockAndCreateMovement() throws Exception {
        StockAdjustmentRequest request = new StockAdjustmentRequest(
                product.getId(), 9, "Contagem física do estoque", "Fabiana Nogueira");

        mockMvc.perform(post("/inventory/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.previousQuantity", is(5)))
                .andExpect(jsonPath("$.newQuantity", is(9)))
                .andExpect(jsonPath("$.difference", is(4)))
                .andExpect(jsonPath("$.origin", is("Ajuste manual")));

        mockMvc.perform(get("/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId", is(product.getId().toString())))
                .andExpect(jsonPath("$[0].stockQuantity", is(9)))
                .andExpect(jsonPath("$[0].lastMovement.origin", is("Ajuste manual")));
    }

    @Test
    @DisplayName("POST /inventory/lots deve registrar lote perecível e somar a quantidade ao estoque")
    void shouldCreateLotAndIncreaseStock() throws Exception {
        CreateInventoryLotRequest request = new CreateInventoryLotRequest(
                product.getId(), "L-2026-001", LocalDate.now(), LocalDate.now().plusDays(7), 12, "Fabiana Nogueira");

        mockMvc.perform(post("/inventory/lots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId", is(product.getId().toString())))
                .andExpect(jsonPath("$.batch", is("L-2026-001")))
                .andExpect(jsonPath("$.quantity", is(12)))
                .andExpect(jsonPath("$.status", is("VALID")));

        mockMvc.perform(get("/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stockQuantity", is(17)))
                .andExpect(jsonPath("$[0].lastMovement.origin", is("Registro de lote")));
    }
}
