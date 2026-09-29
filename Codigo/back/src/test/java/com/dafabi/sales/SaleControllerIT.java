package com.dafabi.sales;

import com.dafabi.cash.domain.CashRegister;
import com.dafabi.cash.repository.CashMovementRepository;
import com.dafabi.cash.repository.CashRegisterRepository;
import com.dafabi.inventory.repository.InventoryLotRepository;
import com.dafabi.inventory.repository.StockMovementRepository;
import com.dafabi.products.domain.Category;
import com.dafabi.products.domain.Product;
import com.dafabi.products.domain.ProductStatus;
import com.dafabi.products.domain.ProductStock;
import com.dafabi.products.repository.CategoryRepository;
import com.dafabi.products.repository.ProductRepository;
import com.dafabi.products.repository.ProductStockRepository;
import com.dafabi.sales.domain.PaymentMethod;
import com.dafabi.sales.dto.CreateSaleRequest;
import com.dafabi.sales.dto.SaleItemRequest;
import com.dafabi.sales.repository.SaleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SaleControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SaleRepository saleRepository;
    @Autowired private CashRegisterRepository cashRegisterRepository;
    @Autowired private CashMovementRepository cashMovementRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductStockRepository productStockRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private InventoryLotRepository inventoryLotRepository;
    @Autowired private StockMovementRepository stockMovementRepository;

    private final UUID operatorId = UUID.randomUUID();
    private Product chicken;
    private Product farofa;

    @BeforeEach
    void setUp() {
        cleanUp();

        Category category = categoryRepository.save(new Category(UUID.randomUUID(), "Frangos", true));
        chicken = saveProduct(category, "Frango assado", "42.90", 5);
        farofa = saveProduct(category, "Farofa", "8.50", 10);
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    @DisplayName("POST /sales deve registrar venda, calcular total e baixar estoque")
    void shouldCreateSaleAndDecreaseStock() throws Exception {
        openCashRegister();
        CreateSaleRequest request = new CreateSaleRequest(List.of(
                new SaleItemRequest(chicken.getId(), 2),
                new SaleItemRequest(farofa.getId(), 1),
                new SaleItemRequest(farofa.getId(), 2)
        ), PaymentMethod.PIX);

        String body = mockMvc.perform(post("/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.origin", is("COUNTER")))
                .andExpect(jsonPath("$.operatorId", is(operatorId.toString())))
                .andExpect(jsonPath("$.saleNumber", is(1)))
                .andExpect(jsonPath("$.dateTime").isNotEmpty())
                .andExpect(jsonPath("$.total", is(111.30)))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andReturn().getResponse().getContentAsString();

        assertThat(productStockRepository.findById(chicken.getId()).orElseThrow().getQuantity()).isEqualTo(3);
        assertThat(productStockRepository.findById(farofa.getId()).orElseThrow().getQuantity()).isEqualTo(7);

        String id = objectMapper.readTree(body).get("id").asText();
        mockMvc.perform(get("/sales/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)));
    }

    @Test
    @DisplayName("POST /sales deve impedir venda com quantidade superior ao estoque")
    void shouldRejectSaleWhenStockIsInsufficient() throws Exception {
        openCashRegister();
        CreateSaleRequest request = new CreateSaleRequest(List.of(
                new SaleItemRequest(farofa.getId(), 1),
                new SaleItemRequest(chicken.getId(), 6)
        ), PaymentMethod.CASH);

        mockMvc.perform(post("/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("INSUFFICIENT_STOCK")));

        assertThat(saleRepository.count()).isZero();
        assertThat(productStockRepository.findById(chicken.getId()).orElseThrow().getQuantity()).isEqualTo(5);
        assertThat(productStockRepository.findById(farofa.getId()).orElseThrow().getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("POST /sales deve retornar erro quando não houver caixa aberto")
    void shouldRejectSaleWhenCashRegisterIsClosed() throws Exception {
        CreateSaleRequest request = new CreateSaleRequest(List.of(
                new SaleItemRequest(chicken.getId(), 1)
        ), PaymentMethod.CASH);

        mockMvc.perform(post("/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("CASH_CLOSED")));

        assertThat(saleRepository.count()).isZero();
        assertThat(productStockRepository.findById(chicken.getId()).orElseThrow().getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("POST /sales deve registrar entrada no caixa quando o pagamento for em dinheiro")
    void shouldRecordCashMovementForCashPayment() throws Exception {
        CashRegister register = openCashRegister();
        CreateSaleRequest request = new CreateSaleRequest(List.of(
                new SaleItemRequest(chicken.getId(), 1)
        ), PaymentMethod.CASH);

        mockMvc.perform(post("/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/cash-registers/{id}", register.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expectedBalance", is(142.90)));
    }

    @Test
    @DisplayName("POST /sales deve validar carrinho vazio e forma de pagamento")
    void shouldValidateRequest() throws Exception {
        mockMvc.perform(post("/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_DATA")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItems("items", "paymentMethod")));
    }

    @Test
    @DisplayName("GET /sales deve listar vendas com paginação e filtros")
    void shouldListSalesWithPaginationAndFilters() throws Exception {
        openCashRegister();
        CreateSaleRequest request = new CreateSaleRequest(List.of(
                new SaleItemRequest(chicken.getId(), 1)
        ), PaymentMethod.PIX);

        mockMvc.perform(post("/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/sales")
                        .param("paymentMethod", "PIX")
                        .param("origin", "COUNTER")
                        .param("status", "COMPLETED")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].paymentMethod", is("PIX")));
    }

    private CashRegister openCashRegister() {
        return cashRegisterRepository.save(new CashRegister(
                null, null, operatorId, "Fabiana", new BigDecimal("100.00"), OffsetDateTime.now()));
    }

    private Product saveProduct(Category category, String name, String price, int quantity) {
        Product product = new Product(UUID.randomUUID(), category, name,
                new BigDecimal(price), new BigDecimal("1.00"), "UN", null,
                false, true, ProductStatus.ACTIVE, null, null);
        product.setStock(new ProductStock(product, quantity));
        return productRepository.save(product);
    }

    private void cleanUp() {
        saleRepository.deleteAll();
        cashMovementRepository.deleteAll();
        cashRegisterRepository.deleteAll();
        stockMovementRepository.deleteAll();
        inventoryLotRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }
}
