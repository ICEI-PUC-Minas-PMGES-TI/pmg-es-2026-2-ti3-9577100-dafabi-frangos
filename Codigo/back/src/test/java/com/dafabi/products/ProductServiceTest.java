package com.dafabi.products;

import com.dafabi.products.application.ProductService;
import com.dafabi.products.domain.Category;
import com.dafabi.products.domain.Product;
import com.dafabi.products.domain.ProductStatus;
import com.dafabi.products.dto.CreateProductRequest;
import com.dafabi.products.dto.ProductResponse;
import com.dafabi.products.mapper.ProductMapper;
import com.dafabi.products.repository.CategoryRepository;
import com.dafabi.products.repository.ProductRepository;
import com.dafabi.shared.exception.BusinessException;
import com.dafabi.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Spy
    private ProductMapper productMapper = new ProductMapper();

    @InjectMocks
    private ProductService productService;

    private Category testCategory;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        testCategory = new Category(categoryId, "Frango Congelado", true);
    }

    @Test
    @DisplayName("Deve cadastrar produto com sucesso com categoria e estoque inicial")
    void shouldCreateProductSuccessfully() {
        CreateProductRequest request = new CreateProductRequest(
                "Frango Inteiro Resfriado",
                categoryId,
                new BigDecimal("18.90"),
                new BigDecimal("12.50"),
                "KG",
                "7891234567890",
                true,
                true,
                ProductStatus.ACTIVE,
                50
        );

        when(productRepository.existsByBarcode("7891234567890")).thenReturn(false);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo("Frango Inteiro Resfriado");
        assertThat(response.salePrice()).isEqualTo(new BigDecimal("18.90"));
        assertThat(response.currentCost()).isEqualTo(new BigDecimal("12.50"));
        assertThat(response.unit()).isEqualTo("KG");
        assertThat(response.barcode()).isEqualTo("7891234567890");
        assertThat(response.perishable()).isTrue();
        assertThat(response.frequent()).isTrue();
        assertThat(response.status()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(response.stockQuantity()).isEqualTo(50);
        assertThat(response.category()).isNotNull();
        assertThat(response.category().id()).isEqualTo(categoryId);
        assertThat(response.category().name()).isEqualTo("Frango Congelado");

        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Deve falhar ao cadastrar produto com código de barras duplicado")
    void shouldFailWhenBarcodeAlreadyExists() {
        CreateProductRequest request = new CreateProductRequest(
                "Frango a Passarinho",
                null,
                new BigDecimal("15.00"),
                new BigDecimal("10.00"),
                "UN",
                "7890000000000",
                false,
                false,
                ProductStatus.ACTIVE,
                10
        );

        when(productRepository.existsByBarcode("7890000000000")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Já existe um produto cadastrado com este código de barras.");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve falhar ao cadastrar produto quando categoria informada não existe")
    void shouldFailWhenCategoryDoesNotExist() {
        UUID nonExistentId = UUID.randomUUID();
        CreateProductRequest request = new CreateProductRequest(
                "Frango Caipira",
                nonExistentId,
                new BigDecimal("25.00"),
                new BigDecimal("17.00"),
                "UN",
                null,
                false,
                false,
                ProductStatus.ACTIVE,
                0
        );

        when(categoryRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Categoria");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve buscar produto por ID com sucesso")
    void shouldFindProductByIdSuccessfully() {
        UUID productId = UUID.randomUUID();
        Product product = new Product(
                productId,
                testCategory,
                "Filé de Peito",
                new BigDecimal("22.00"),
                new BigDecimal("14.00"),
                "KG",
                "7891112223334",
                true,
                true,
                ProductStatus.ACTIVE,
                null,
                null
        );

        when(productRepository.findWithCategoryAndStockById(productId)).thenReturn(Optional.of(product));

        ProductResponse response = productService.findById(productId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(productId);
        assertThat(response.name()).isEqualTo("Filé de Peito");
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar produto inexistente")
    void shouldThrowExceptionWhenProductNotFound() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findWithCategoryAndStockById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(productId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Produto");
    }
}
