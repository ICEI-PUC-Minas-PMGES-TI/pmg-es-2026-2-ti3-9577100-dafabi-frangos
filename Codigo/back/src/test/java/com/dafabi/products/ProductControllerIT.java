package com.dafabi.products;

import com.dafabi.products.domain.Category;
import com.dafabi.products.dto.CreateProductRequest;
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
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category savedCategory;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        savedCategory = new Category(UUID.randomUUID(), "Cortes de Frango", true);
        categoryRepository.save(savedCategory);
    }

    @Test
    @DisplayName("POST /products - Deve cadastrar produto e retornar 201 com cabeçalho Location")
    void shouldCreateProductSuccessfully() throws Exception {
        CreateProductRequest request = new CreateProductRequest(
                "Coxa e Sobrecoxa",
                savedCategory.getId(),
                new BigDecimal("14.90"),
                new BigDecimal("9.50"),
                "KG",
                "7891234560001",
                true,
                true,
                100
        );

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name", is("Coxa e Sobrecoxa")))
                .andExpect(jsonPath("$.salePrice", is(14.90)))
                .andExpect(jsonPath("$.currentCost", is(9.50)))
                .andExpect(jsonPath("$.unit", is("KG")))
                .andExpect(jsonPath("$.barcode", is("7891234560001")))
                .andExpect(jsonPath("$.perishable", is(true)))
                .andExpect(jsonPath("$.frequent", is(true)))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.stockQuantity", is(100)))
                .andExpect(jsonPath("$.category.id", is(savedCategory.getId().toString())))
                .andExpect(jsonPath("$.category.name", is("Cortes de Frango")));
    }

    @Test
    @DisplayName("POST /products - Deve retornar 400 com RFC 7807 ao enviar dados inválidos")
    void shouldReturnBadRequestWhenValidationFails() throws Exception {
        CreateProductRequest request = new CreateProductRequest(
                "", // Nome em branco
                null,
                new BigDecimal("-5.00"), // Preço negativo
                null, // Custo nulo
                "UN",
                null,
                false,
                false,
                -1 // Estoque negativo
        );

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Erro de validação")))
                .andExpect(jsonPath("$.code", is("INVALID_DATA")))
                .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("POST /products - Deve retornar 400 ao tentar cadastrar código de barras duplicado")
    void shouldReturnBadRequestWhenDuplicateBarcode() throws Exception {
        CreateProductRequest firstRequest = new CreateProductRequest(
                "Asa de Frango",
                savedCategory.getId(),
                new BigDecimal("16.00"),
                new BigDecimal("11.00"),
                "KG",
                "7899999999999",
                true,
                false,
                20
        );

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        CreateProductRequest duplicateRequest = new CreateProductRequest(
                "Outra Asa de Frango",
                savedCategory.getId(),
                new BigDecimal("17.00"),
                new BigDecimal("12.00"),
                "KG",
                "7899999999999",
                true,
                false,
                10
        );

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("DUPLICATE_BARCODE")))
                .andExpect(jsonPath("$.detail", containsString("Já existe um produto cadastrado com este código de barras.")));
    }

    @Test
    @DisplayName("GET /products/{id} - Deve retornar 200 ao buscar produto existente")
    void shouldGetProductById() throws Exception {
        CreateProductRequest request = new CreateProductRequest(
                "Moela de Frango",
                savedCategory.getId(),
                new BigDecimal("11.00"),
                new BigDecimal("7.00"),
                "KG",
                null,
                true,
                false,
                15
        );

        String responseContent = mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(responseContent).get("id").asText();

        mockMvc.perform(get("/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id)))
                .andExpect(jsonPath("$.name", is("Moela de Frango")));
    }
}
