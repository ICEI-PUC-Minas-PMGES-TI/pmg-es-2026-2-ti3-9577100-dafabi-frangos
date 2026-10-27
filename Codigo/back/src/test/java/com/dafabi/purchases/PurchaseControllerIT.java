package com.dafabi.purchases;

import com.dafabi.inventory.repository.InventoryLotRepository;
import com.dafabi.inventory.repository.StockMovementRepository;
import com.dafabi.products.domain.Category;
import com.dafabi.products.domain.Product;
import com.dafabi.products.domain.ProductStatus;
import com.dafabi.products.domain.ProductStock;
import com.dafabi.products.repository.CategoryRepository;
import com.dafabi.products.repository.ProductRepository;
import com.dafabi.products.repository.ProductStockRepository;
import com.dafabi.purchases.dto.CreatePurchaseRequest;
import com.dafabi.purchases.dto.PurchaseItemRequest;
import com.dafabi.purchases.repository.PurchaseRepository;
import com.dafabi.suppliers.domain.Supplier;
import com.dafabi.suppliers.repository.SupplierRepository;
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
import java.time.LocalDate;
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
class PurchaseControllerIT {

    // Maior UUID em ordem textual: garante que o item inválido seja processado depois dos válidos.
    private static final UUID LAST_PRODUCT_ID = UUID.fromString("ffffffff-ffff-4fff-bfff-ffffffffffff");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PurchaseRepository purchaseRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductStockRepository productStockRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private InventoryLotRepository inventoryLotRepository;
    @Autowired private StockMovementRepository stockMovementRepository;

    private Category category;
    private Supplier supplier;
    private Product chicken;
    private Product halfChicken;

    @BeforeEach
    void setUp() {
        cleanUp();

        supplier = saveSupplier("Granja Santa Clara", "contato@granjasantaclara.com.br", true);
        category = categoryRepository.save(new Category(UUID.randomUUID(), "Frangos", true));
        chicken = saveProduct(UUID.randomUUID(), "Frango assado tradicional", ProductStatus.ACTIVE, 10);
        halfChicken = saveProduct(UUID.randomUUID(), "Meio frango assado", ProductStatus.ACTIVE, 5);
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    @DisplayName("POST /purchases deve registrar compra, calcular totais no back-end e somar ao estoque")
    void shouldCreatePurchaseAndIncreaseStock() throws Exception {
        CreatePurchaseRequest request = new CreatePurchaseRequest(supplier.getId(), LocalDate.now(), List.of(
                new PurchaseItemRequest(chicken.getId(), 40, new BigDecimal("24.50")),
                new PurchaseItemRequest(halfChicken.getId(), 20, new BigDecimal("13.00"))
        ), "Fabiana Nogueira");

        String body = mockMvc.perform(post("/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.supplierId", is(supplier.getId().toString())))
                .andExpect(jsonPath("$.supplierName", is("Granja Santa Clara")))
                .andExpect(jsonPath("$.date", is(LocalDate.now().toString())))
                .andExpect(jsonPath("$.status", is("REGISTERED")))
                .andExpect(jsonPath("$.createdBy", is("Fabiana Nogueira")))
                .andExpect(jsonPath("$.itemCount", is(2)))
                .andExpect(jsonPath("$.total", is(1240.00)))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].productName", is("Frango assado tradicional")))
                .andExpect(jsonPath("$.items[0].total", is(980.00)))
                .andExpect(jsonPath("$.items[1].total", is(260.00)))
                .andReturn().getResponse().getContentAsString();

        assertThat(productStockRepository.findById(chicken.getId()).orElseThrow().getQuantity()).isEqualTo(50);
        assertThat(productStockRepository.findById(halfChicken.getId()).orElseThrow().getQuantity()).isEqualTo(25);

        String id = objectMapper.readTree(body).get("id").asText();
        mockMvc.perform(get("/purchases/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id)))
                .andExpect(jsonPath("$.total", is(1240.00)))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[*].unitCost", containsInAnyOrder(24.50, 13.00)));
    }

    @Test
    @DisplayName("POST /purchases deve registrar movimentações PURCHASE visíveis na posição de estoque")
    void shouldRecordPurchaseStockMovements() throws Exception {
        CreatePurchaseRequest request = new CreatePurchaseRequest(supplier.getId(), LocalDate.now(), List.of(
                new PurchaseItemRequest(chicken.getId(), 15, new BigDecimal("24.50")),
                new PurchaseItemRequest(chicken.getId(), 5, new BigDecimal("25.00"))
        ));

        mockMvc.perform(post("/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itemCount", is(2)))
                .andExpect(jsonPath("$.total", is(492.50)));

        mockMvc.perform(get("/inventory/movements").param("productId", chicken.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type", is("PURCHASE")))
                .andExpect(jsonPath("$[0].origin", is("Compra")))
                .andExpect(jsonPath("$[0].previousQuantity", is(10)))
                .andExpect(jsonPath("$[0].newQuantity", is(30)))
                .andExpect(jsonPath("$[0].difference", is(20)))
                .andExpect(jsonPath("$[0].reason", is("Compra registrada de Granja Santa Clara")));

        mockMvc.perform(get("/inventory").param("query", "Frango assado tradicional"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stockQuantity", is(30)))
                .andExpect(jsonPath("$[0].lastMovement.origin", is("Compra")));
    }

    @Test
    @DisplayName("POST /purchases deve rejeitar compra sem itens e campos obrigatórios")
    void shouldRejectPurchaseWithoutItems() throws Exception {
        mockMvc.perform(post("/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_DATA")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItems("items", "supplierId", "date")));

        assertThat(purchaseRepository.count()).isZero();
        assertThat(stockMovementRepository.count()).isZero();
    }

    @Test
    @DisplayName("POST /purchases deve rejeitar item com quantidade ou custo inválidos sem alterar o estoque")
    void shouldRejectInvalidItemWithoutChangingStock() throws Exception {
        CreatePurchaseRequest request = new CreatePurchaseRequest(supplier.getId(), LocalDate.now(), List.of(
                new PurchaseItemRequest(chicken.getId(), 10, new BigDecimal("24.50")),
                new PurchaseItemRequest(halfChicken.getId(), 0, new BigDecimal("-1.00"))
        ));

        mockMvc.perform(post("/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_DATA")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItems("items[1].quantity", "items[1].unitCost")));

        assertThat(purchaseRepository.count()).isZero();
        assertThat(stockMovementRepository.count()).isZero();
        assertThat(productStockRepository.findById(chicken.getId()).orElseThrow().getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("POST /purchases deve rejeitar fornecedor inativo ou data futura")
    void shouldRejectInactiveSupplierAndFutureDate() throws Exception {
        Supplier inactive = saveSupplier("Distribuidora Bom Sabor", "vendas@bomsabor.com.br", false);
        CreatePurchaseRequest inactiveSupplierRequest = new CreatePurchaseRequest(inactive.getId(), LocalDate.now(), List.of(
                new PurchaseItemRequest(chicken.getId(), 10, new BigDecimal("24.50"))
        ));

        mockMvc.perform(post("/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inactiveSupplierRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("SUPPLIER_INACTIVE")));

        CreatePurchaseRequest futureRequest = new CreatePurchaseRequest(supplier.getId(), LocalDate.now().plusDays(1), List.of(
                new PurchaseItemRequest(chicken.getId(), 10, new BigDecimal("24.50"))
        ));

        mockMvc.perform(post("/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(futureRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("date")));

        assertThat(purchaseRepository.count()).isZero();
        assertThat(productStockRepository.findById(chicken.getId()).orElseThrow().getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("POST /purchases deve desfazer a atualização de estoque quando um item falha no meio da transação")
    void shouldRollbackStockWhenProductIsNotFound() throws Exception {
        CreatePurchaseRequest request = new CreatePurchaseRequest(supplier.getId(), LocalDate.now(), List.of(
                new PurchaseItemRequest(chicken.getId(), 40, new BigDecimal("24.50")),
                new PurchaseItemRequest(halfChicken.getId(), 20, new BigDecimal("13.00")),
                new PurchaseItemRequest(LAST_PRODUCT_ID, 1, new BigDecimal("5.00"))
        ));

        mockMvc.perform(post("/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_PRODUCT")));

        assertThat(purchaseRepository.count()).isZero();
        assertThat(stockMovementRepository.count()).isZero();
        assertThat(productStockRepository.findById(chicken.getId()).orElseThrow().getQuantity()).isEqualTo(10);
        assertThat(productStockRepository.findById(halfChicken.getId()).orElseThrow().getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("POST /purchases deve desfazer a atualização de estoque quando um produto está inativo")
    void shouldRollbackStockWhenProductIsInactive() throws Exception {
        Product inactive = saveProduct(LAST_PRODUCT_ID, "Frango desossado", ProductStatus.INACTIVE, 3);
        CreatePurchaseRequest request = new CreatePurchaseRequest(supplier.getId(), LocalDate.now(), List.of(
                new PurchaseItemRequest(chicken.getId(), 40, new BigDecimal("24.50")),
                new PurchaseItemRequest(inactive.getId(), 2, new BigDecimal("30.00"))
        ));

        mockMvc.perform(post("/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("PRODUCT_INACTIVE")));

        assertThat(purchaseRepository.count()).isZero();
        assertThat(stockMovementRepository.count()).isZero();
        assertThat(productStockRepository.findById(chicken.getId()).orElseThrow().getQuantity()).isEqualTo(10);
        assertThat(productStockRepository.findById(inactive.getId()).orElseThrow().getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("GET /purchases deve listar fornecedor, data, quantidade de itens, valor total e status")
    void shouldListPurchases() throws Exception {
        Supplier otherSupplier = saveSupplier("Distribuidora Bom Sabor", "vendas@bomsabor.com.br", true);
        createPurchase(supplier, LocalDate.now().minusDays(2), List.of(
                new PurchaseItemRequest(chicken.getId(), 40, new BigDecimal("24.50")),
                new PurchaseItemRequest(halfChicken.getId(), 20, new BigDecimal("13.00"))));
        createPurchase(otherSupplier, LocalDate.now(), List.of(
                new PurchaseItemRequest(halfChicken.getId(), 10, new BigDecimal("12.90"))));

        mockMvc.perform(get("/purchases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].supplierName", is("Distribuidora Bom Sabor")))
                .andExpect(jsonPath("$[0].date", is(LocalDate.now().toString())))
                .andExpect(jsonPath("$[0].itemCount", is(1)))
                .andExpect(jsonPath("$[0].total", is(129.00)))
                .andExpect(jsonPath("$[0].status", is("REGISTERED")))
                .andExpect(jsonPath("$[1].supplierName", is("Granja Santa Clara")))
                .andExpect(jsonPath("$[1].itemCount", is(2)))
                .andExpect(jsonPath("$[1].total", is(1240.00)));

        mockMvc.perform(get("/purchases").param("supplierId", supplier.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].supplierId", is(supplier.getId().toString())));
    }

    @Test
    @DisplayName("GET /purchases/{id} deve retornar 404 quando a compra não existir")
    void shouldReturnNotFoundForUnknownPurchase() throws Exception {
        mockMvc.perform(get("/purchases/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")));
    }

    private void createPurchase(Supplier purchaseSupplier, LocalDate date, List<PurchaseItemRequest> items) throws Exception {
        mockMvc.perform(post("/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreatePurchaseRequest(purchaseSupplier.getId(), date, items))))
                .andExpect(status().isCreated());
    }

    private Supplier saveSupplier(String name, String email, boolean active) {
        return supplierRepository.save(new Supplier(UUID.randomUUID(), Supplier.DEFAULT_STORE_ID,
                name, "(31) 99812-4400", email, null, active, null, null));
    }

    private Product saveProduct(UUID id, String name, ProductStatus status, int quantity) {
        Product product = new Product(id, category, name,
                new BigDecimal("42.90"), new BigDecimal("24.50"), "UN", null,
                false, true, status, null, null);
        product.setStock(new ProductStock(product, quantity));
        return productRepository.save(product);
    }

    private void cleanUp() {
        purchaseRepository.deleteAll();
        stockMovementRepository.deleteAll();
        inventoryLotRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        supplierRepository.deleteAll();
    }
}
