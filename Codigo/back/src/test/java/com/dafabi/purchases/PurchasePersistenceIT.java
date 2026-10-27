package com.dafabi.purchases;

import com.dafabi.products.domain.Category;
import com.dafabi.products.domain.Product;
import com.dafabi.products.domain.ProductStatus;
import com.dafabi.products.domain.ProductStock;
import com.dafabi.products.repository.CategoryRepository;
import com.dafabi.products.repository.ProductRepository;
import com.dafabi.purchases.domain.Purchase;
import com.dafabi.purchases.domain.PurchaseItem;
import com.dafabi.purchases.domain.PurchaseStatus;
import com.dafabi.purchases.repository.PurchaseItemRepository;
import com.dafabi.purchases.repository.PurchaseRepository;
import com.dafabi.suppliers.domain.Supplier;
import com.dafabi.suppliers.repository.SupplierRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PurchasePersistenceIT {

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private PurchaseItemRepository purchaseItemRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Validator validator;
    private Supplier supplier;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        purchaseItemRepository.deleteAll();
        purchaseRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        supplierRepository.deleteAll();

        supplier = supplierRepository.save(new Supplier(
                UUID.randomUUID(),
                Supplier.DEFAULT_STORE_ID,
                "Granja Santa Clara",
                "(31) 99812-4400",
                "contato@granjasantaclara.com.br",
                "Entregas às terças e sextas.",
                true,
                null,
                null
        ));

        Category category = categoryRepository.save(new Category(UUID.randomUUID(), "Frangos", true));

        product1 = new Product(
                UUID.randomUUID(),
                category,
                "Frango assado tradicional",
                new BigDecimal("42.90"),
                new BigDecimal("24.50"),
                "UN",
                null,
                true,
                true,
                ProductStatus.ACTIVE,
                null,
                null
        );
        product1.setStock(new ProductStock(product1, 10));
        product1 = productRepository.save(product1);

        product2 = new Product(
                UUID.randomUUID(),
                category,
                "Meio frango assado",
                new BigDecimal("23.90"),
                new BigDecimal("13.50"),
                "UN",
                null,
                true,
                true,
                ProductStatus.ACTIVE,
                null,
                null
        );
        product2.setStock(new ProductStock(product2, 5));
        product2 = productRepository.save(product2);
    }

    @Test
    @DisplayName("Deve persistir compra com itens, calcular valor total e preservar snapshot histórico do produto")
    void shouldPersistPurchaseWithItemsAndHistoricalProductSnapshot() {
        Purchase purchase = new Purchase(
                null,
                supplier,
                LocalDate.of(2026, 10, 26),
                null,
                null,
                "Fabiana Nogueira",
                null
        );

        purchase.addItem(new PurchaseItem(product1, 40, new BigDecimal("24.50")));
        purchase.addItem(new PurchaseItem(product2, 20, new BigDecimal("13.00")));

        Purchase saved = purchaseRepository.saveAndFlush(purchase);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(PurchaseStatus.REGISTERED);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getTotalAmount()).isEqualByComparingTo(new BigDecimal("1240.00"));

        Purchase persisted = purchaseRepository.findWithDetailsById(saved.getId()).orElseThrow();
        assertThat(persisted.getSupplier().getId()).isEqualTo(supplier.getId());
        assertThat(persisted.getSupplier().getName()).isEqualTo("Granja Santa Clara");
        assertThat(persisted.getPurchaseDate()).isEqualTo(LocalDate.of(2026, 10, 26));
        assertThat(persisted.getCreatedBy()).isEqualTo("Fabiana Nogueira");
        assertThat(persisted.getItems()).hasSize(2);
        assertThat(persisted.getItems())
                .extracting(PurchaseItem::getProductName)
                .containsExactlyInAnyOrder("Frango assado tradicional", "Meio frango assado");
        assertThat(persisted.getItems())
                .extracting(PurchaseItem::getTotalAmount)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactlyInAnyOrder(new BigDecimal("980.00"), new BigDecimal("260.00"));
    }

    @Test
    @DisplayName("Deve rejeitar compra sem campos obrigatórios ou com valores inválidos")
    void shouldRejectMissingRequiredFieldsAndNonPositiveAmounts() {
        Purchase invalidPurchase = new Purchase();
        invalidPurchase.setTotalAmount(BigDecimal.ZERO);

        Set<ConstraintViolation<Purchase>> purchaseViolations = validator.validate(invalidPurchase);
        assertThat(purchaseViolations)
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .contains("supplier", "purchaseDate", "totalAmount", "items");

        PurchaseItem invalidItem = new PurchaseItem();
        invalidItem.setQuantity(0);
        invalidItem.setUnitCost(BigDecimal.ZERO);
        invalidItem.setTotalAmount(BigDecimal.ZERO);

        Set<ConstraintViolation<PurchaseItem>> itemViolations = validator.validate(invalidItem);
        assertThat(itemViolations)
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .contains("product", "productName", "quantity", "unitCost", "totalAmount");
    }
}
