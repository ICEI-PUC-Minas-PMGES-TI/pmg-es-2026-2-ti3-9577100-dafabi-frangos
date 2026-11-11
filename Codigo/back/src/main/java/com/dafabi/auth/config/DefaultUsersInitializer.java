package com.dafabi.auth.config;

import com.dafabi.products.domain.Category;
import com.dafabi.products.domain.Product;
import com.dafabi.products.domain.ProductStatus;
import com.dafabi.products.domain.ProductStock;
import com.dafabi.products.repository.CategoryRepository;
import com.dafabi.products.repository.ProductRepository;
import com.dafabi.users.domain.Role;
import com.dafabi.users.domain.User;
import com.dafabi.users.repository.RoleRepository;
import com.dafabi.users.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@Profile("!test")
public class DefaultUsersInitializer implements ApplicationRunner {

    public static final UUID ADMIN_ROLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
    public static final UUID CASHIER_ROLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000202");

    public static final UUID ADMIN_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    public static final UUID CASHIER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000102");
    public static final UUID INACTIVE_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000103");

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    public DefaultUsersInitializer(
            RoleRepository roleRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Category frangosCategory = new Category(UUID.fromString("a0000000-0000-0000-0000-000000000003"), "Frangos", true);
        Category complementosCategory = new Category(UUID.fromString("a0000000-0000-0000-0000-000000000002"), "Complementos", true);
        Category bebidasCategory = new Category(UUID.fromString("a0000000-0000-0000-0000-000000000001"), "Bebidas", true);

        if (categoryRepository.count() == 0) {
            categoryRepository.saveAll(List.of(
                    bebidasCategory,
                    complementosCategory,
                    frangosCategory,
                    new Category(UUID.fromString("a0000000-0000-0000-0000-000000000004"), "Industrializados", true),
                    new Category(UUID.fromString("a0000000-0000-0000-0000-000000000005"), "Marmitas", true)
            ));
        }

        if (productRepository.count() == 0) {
            Category persistedFrangos = categoryRepository.findById(frangosCategory.getId()).orElse(frangosCategory);
            Category persistedComplementos = categoryRepository.findById(complementosCategory.getId()).orElse(complementosCategory);
            Category persistedBebidas = categoryRepository.findById(bebidasCategory.getId()).orElse(bebidasCategory);

            Product frango = new Product(
                    UUID.fromString("b0000000-0000-0000-0000-000000000001"),
                    persistedFrangos,
                    "Frango Assado Tradicional",
                    new BigDecimal("44.90"),
                    new BigDecimal("24.50"),
                    "UN",
                    "7891000000011",
                    true,
                    true,
                    ProductStatus.ACTIVE,
                    null,
                    null
            );
            frango.setStock(new ProductStock(frango, 25));

            Product farofa = new Product(
                    UUID.fromString("b0000000-0000-0000-0000-000000000002"),
                    persistedComplementos,
                    "Farofa Caseira Crocante 250g",
                    new BigDecimal("9.90"),
                    new BigDecimal("4.20"),
                    "UN",
                    "7891000000028",
                    false,
                    true,
                    ProductStatus.ACTIVE,
                    null,
                    null
            );
            farofa.setStock(new ProductStock(farofa, 40));

            Product refrigerante = new Product(
                    UUID.fromString("b0000000-0000-0000-0000-000000000003"),
                    persistedBebidas,
                    "Refrigerante Guaraná 2L",
                    new BigDecimal("12.00"),
                    new BigDecimal("7.50"),
                    "UN",
                    "7891000000035",
                    false,
                    true,
                    ProductStatus.ACTIVE,
                    null,
                    null
            );
            refrigerante.setStock(new ProductStock(refrigerante, 30));

            productRepository.saveAll(List.of(frango, farofa, refrigerante));
        }

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role(ADMIN_ROLE_ID, "ROLE_ADMIN")));

        Role cashierRole = roleRepository.findByName("ROLE_CASHIER")
                .orElseGet(() -> roleRepository.save(new Role(CASHIER_ROLE_ID, "ROLE_CASHIER")));

        if (!userRepository.existsByEmail("admin@dafabi.com.br")) {
            User admin = new User(
                    ADMIN_USER_ID,
                    "admin@dafabi.com.br",
                    passwordEncoder.encode("admin123"),
                    "Fabiana Nogueira",
                    Set.of(adminRole)
            );
            userRepository.save(admin);
        }

        if (!userRepository.existsByEmail("caixa@dafabi.com.br")) {
            User cashier = new User(
                    CASHIER_USER_ID,
                    "caixa@dafabi.com.br",
                    passwordEncoder.encode("caixa123"),
                    "Carlos Oliveira",
                    Set.of(cashierRole)
            );
            userRepository.save(cashier);
        }

        if (!userRepository.existsByEmail("marina@dafabi.com.br")) {
            User inactive = new User(
                    INACTIVE_USER_ID,
                    "marina@dafabi.com.br",
                    passwordEncoder.encode("marina123"),
                    "Marina Souza",
                    Set.of(cashierRole)
            );
            inactive.deactivate();
            userRepository.save(inactive);
        }
    }
}
