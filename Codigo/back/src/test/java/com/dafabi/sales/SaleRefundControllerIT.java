package com.dafabi.sales;

import com.dafabi.cash.domain.CashMovementDirection;
import com.dafabi.cash.domain.CashRegister;
import com.dafabi.cash.repository.CashMovementRepository;
import com.dafabi.cash.repository.CashRegisterRepository;
import com.dafabi.inventory.domain.StockMovementType;
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
import com.dafabi.sales.domain.Sale;
import com.dafabi.sales.domain.SaleStatus;
import com.dafabi.sales.dto.CreateSaleRefundRequest;
import com.dafabi.sales.dto.CreateSaleRequest;
import com.dafabi.sales.dto.SaleItemRequest;
import com.dafabi.sales.repository.SaleRefundRepository;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

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
@WithMockUser(roles = "ADMIN")
class SaleRefundControllerIT {

    private static final String ADMIN_ID = "00000000-0000-0000-0000-000000000101";
    private static final String CASHIER_ID = "00000000-0000-0000-0000-000000000102";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SaleRepository saleRepository;
    @Autowired private SaleRefundRepository saleRefundRepository;
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
    private CashRegister register;

    @BeforeEach
    void setUp() {
        cleanUp();

        Category category = categoryRepository.save(new Category(UUID.randomUUID(), "Frangos", true));
        chicken = saveProduct(category, "Frango assado", "42.90", 5);
        farofa = saveProduct(category, "Farofa", "8.50", 10);
        register = cashRegisterRepository.save(new CashRegister(
                null, null, operatorId, "Fabiana", new BigDecimal("100.00"), OffsetDateTime.now()));
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    @DisplayName("POST /sales/{id}/refund deve estornar a venda revertendo caixa e estoque na mesma operação")
    void shouldRefundSaleReversingCashAndStock() throws Exception {
        String saleId = createSale(PaymentMethod.CASH);
        assertThat(productStockRepository.findById(chicken.getId()).orElseThrow().getQuantity()).isEqualTo(3);

        mockMvc.perform(asAdmin(post("/sales/{id}/refund", saleId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateSaleRefundRequest("Cliente desistiu da compra"))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.saleId", is(saleId)))
                .andExpect(jsonPath("$.saleStatus", is("REFUNDED")))
                .andExpect(jsonPath("$.amount", is(94.30)))
                .andExpect(jsonPath("$.cashAmount", is(94.30)))
                .andExpect(jsonPath("$.cashRegisterId", is(register.getId().toString())))
                .andExpect(jsonPath("$.reason", is("Cliente desistiu da compra")))
                .andExpect(jsonPath("$.refundedById", is(ADMIN_ID)))
                .andExpect(jsonPath("$.refundedBy", is("Fabiana Nogueira")));

        Sale sale = saleRepository.findById(UUID.fromString(saleId)).orElseThrow();
        assertThat(sale.getStatus()).isEqualTo(SaleStatus.REFUNDED);
        assertThat(productStockRepository.findById(chicken.getId()).orElseThrow().getQuantity()).isEqualTo(5);
        assertThat(productStockRepository.findById(farofa.getId()).orElseThrow().getQuantity()).isEqualTo(10);
        assertThat(stockMovementRepository.findAll())
                .filteredOn(movement -> movement.getMovementType() == StockMovementType.REFUND)
                .hasSize(2);

        mockMvc.perform(get("/cash-registers/{id}", register.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expectedBalance", is(100.00)));
        mockMvc.perform(get("/cash-registers/{id}/summary", register.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashRefunds", is(94.30)));

        mockMvc.perform(get("/sales/{id}/refund", saleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason", is("Cliente desistiu da compra")));
    }

    @Test
    @DisplayName("POST /sales/{id}/refund deve ser idempotente e não duplicar reversões")
    void shouldBeIdempotent() throws Exception {
        String saleId = createSale(PaymentMethod.CASH);
        String body = objectMapper.writeValueAsString(new CreateSaleRefundRequest("Produto com defeito"));

        String firstId = objectMapper.readTree(mockMvc.perform(asAdmin(post("/sales/{id}/refund", saleId))
                        .header("Idempotency-Key", "estorno-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(asAdmin(post("/sales/{id}/refund", saleId))
                        .header("Idempotency-Key", "estorno-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(firstId)));

        mockMvc.perform(asAdmin(post("/sales/{id}/refund", saleId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("SALE_ALREADY_REFUNDED")));

        assertThat(saleRefundRepository.count()).isEqualTo(1);
        assertThat(productStockRepository.findById(chicken.getId()).orElseThrow().getQuantity()).isEqualTo(5);
        assertThat(productStockRepository.findById(farofa.getId()).orElseThrow().getQuantity()).isEqualTo(10);
        assertThat(stockMovementRepository.findAll())
                .filteredOn(movement -> movement.getMovementType() == StockMovementType.REFUND)
                .hasSize(2);
        assertThat(cashMovementRepository.findAll())
                .filteredOn(movement -> movement.getDirection() == CashMovementDirection.OUT)
                .hasSize(1);
    }

    @Test
    @DisplayName("POST /sales/{id}/refund deve bloquear venda cancelada")
    void shouldRejectCancelledSale() throws Exception {
        String saleId = createSale(PaymentMethod.PIX);
        Sale sale = saleRepository.findById(UUID.fromString(saleId)).orElseThrow();
        sale.setStatus(SaleStatus.CANCELLED);
        saleRepository.save(sale);

        mockMvc.perform(asAdmin(post("/sales/{id}/refund", saleId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateSaleRefundRequest("Cliente desistiu da compra"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("SALE_CANCELLED")));

        assertThat(saleRefundRepository.count()).isZero();
        assertThat(productStockRepository.findById(chicken.getId()).orElseThrow().getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("POST /sales/{id}/refund deve permitir somente usuários ADMIN")
    void shouldAllowOnlyAdmin() throws Exception {
        String saleId = createSale(PaymentMethod.CASH);
        String body = objectMapper.writeValueAsString(new CreateSaleRefundRequest("Cliente desistiu da compra"));

        mockMvc.perform(post("/sales/{id}/refund", saleId)
                        .header("X-User-Id", CASHIER_ID)
                        .header("X-User-Name", "Carlos Oliveira")
                        .header("X-User-Role", "CASHIER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("FORBIDDEN")));

        mockMvc.perform(post("/sales/{id}/refund", saleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHENTICATED")));

        assertThat(saleRefundRepository.count()).isZero();
        assertThat(saleRepository.findById(UUID.fromString(saleId)).orElseThrow().getStatus())
                .isEqualTo(SaleStatus.COMPLETED);
        assertThat(productStockRepository.findById(chicken.getId()).orElseThrow().getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("POST /sales/{id}/refund deve exigir o motivo do estorno")
    void shouldRequireReason() throws Exception {
        String saleId = createSale(PaymentMethod.PIX);

        mockMvc.perform(asAdmin(post("/sales/{id}/refund", saleId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_DATA")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("reason")));

        assertThat(saleRefundRepository.count()).isZero();
    }

    @Test
    @DisplayName("GET /sales/{id}/refund-eligibility deve informar se a venda pode ser estornada")
    void shouldReturnEligibility() throws Exception {
        String saleId = createSale(PaymentMethod.CASH);

        mockMvc.perform(get("/sales/{id}/refund-eligibility", saleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible", is(true)))
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.total", is(94.30)))
                .andExpect(jsonPath("$.cashAmountToReverse", is(94.30)))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.refund").value(nullValue()));

        mockMvc.perform(asAdmin(post("/sales/{id}/refund", saleId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateSaleRefundRequest("Cliente desistiu da compra"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/sales/{id}/refund-eligibility", saleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible", is(false)))
                .andExpect(jsonPath("$.ineligibilityCode", is("SALE_ALREADY_REFUNDED")))
                .andExpect(jsonPath("$.refund.reason", is("Cliente desistiu da compra")));

        mockMvc.perform(get("/sales/{id}/refund-eligibility", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder builder) {
        return builder
                .header("X-User-Id", ADMIN_ID)
                .header("X-User-Name", "Fabiana Nogueira")
                .header("X-User-Role", "ADMIN");
    }

    private String createSale(PaymentMethod paymentMethod) throws Exception {
        CreateSaleRequest request = new CreateSaleRequest(List.of(
                new SaleItemRequest(chicken.getId(), 2),
                new SaleItemRequest(farofa.getId(), 1)
        ), paymentMethod);

        String body = mockMvc.perform(post("/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private Product saveProduct(Category category, String name, String price, int quantity) {
        Product product = new Product(UUID.randomUUID(), category, name,
                new BigDecimal(price), new BigDecimal("1.00"), "UN", null,
                false, true, ProductStatus.ACTIVE, null, null);
        product.setStock(new ProductStock(product, quantity));
        return productRepository.save(product);
    }

    private void cleanUp() {
        saleRefundRepository.deleteAll();
        saleRepository.deleteAll();
        cashMovementRepository.deleteAll();
        cashRegisterRepository.deleteAll();
        stockMovementRepository.deleteAll();
        inventoryLotRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }
}
