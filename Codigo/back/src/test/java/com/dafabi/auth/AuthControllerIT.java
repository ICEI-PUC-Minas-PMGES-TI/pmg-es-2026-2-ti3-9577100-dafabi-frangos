package com.dafabi.auth;

import com.dafabi.auth.repository.RefreshTokenRepository;
import com.dafabi.auth.security.JwtService;
import com.dafabi.inventory.repository.StockMovementRepository;
import com.dafabi.products.domain.Category;
import com.dafabi.products.repository.CategoryRepository;
import com.dafabi.products.repository.ProductRepository;
import com.dafabi.shared.security.UserRole;
import com.dafabi.users.domain.Role;
import com.dafabi.users.domain.User;
import com.dafabi.users.repository.RoleRepository;
import com.dafabi.users.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIT {

    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID CASHIER_ID = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID INACTIVE_ID = UUID.fromString("00000000-0000-0000-0000-000000000103");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private StockMovementRepository stockMovementRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private Category category;

    @BeforeEach
    void setUp() {
        cleanUp();

        Role adminRole = roleRepository.save(new Role(null, "ROLE_ADMIN"));
        Role cashierRole = roleRepository.save(new Role(null, "ROLE_CASHIER"));

        userRepository.save(new User(
                ADMIN_ID,
                "admin@dafabi.com.br",
                passwordEncoder.encode("admin123"),
                "Fabiana Nogueira",
                Set.of(adminRole)
        ));

        userRepository.save(new User(
                CASHIER_ID,
                "caixa@dafabi.com.br",
                passwordEncoder.encode("caixa123"),
                "Carlos Oliveira",
                Set.of(cashierRole)
        ));

        User inactiveUser = new User(
                INACTIVE_ID,
                "marina@dafabi.com.br",
                passwordEncoder.encode("marina123"),
                "Marina Souza",
                Set.of(cashierRole)
        );
        inactiveUser.deactivate();
        userRepository.save(inactiveUser);

        category = categoryRepository.save(new Category(UUID.randomUUID(), "Frangos", true));
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    @DisplayName("POST /auth/login deve autenticar ADMIN e CASHIER retornando credencial de sessão JWT")
    void shouldLoginAndReturnJwtSession() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "identifier", "admin",
                                "password", "admin123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.user.id", is(ADMIN_ID.toString())))
                .andExpect(jsonPath("$.user.name", is("Fabiana Nogueira")))
                .andExpect(jsonPath("$.user.email", is("admin@dafabi.com.br")))
                .andExpect(jsonPath("$.user.role", is("ADMIN")))
                .andExpect(jsonPath("$.user.active", is(true)))
                .andExpect(jsonPath("$.user.permissions", hasItems("DASHBOARD_READ", "REFUND_CREATE")));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "identifier", "caixa@dafabi.com.br",
                                "password", "caixa123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.user.id", is(CASHIER_ID.toString())))
                .andExpect(jsonPath("$.user.role", is("CASHIER")));
    }

    @Test
    @DisplayName("POST /auth/login deve retornar erro genérico 401 INVALID_CREDENTIALS para senha inválida ou usuário inativo")
    void shouldRejectInvalidCredentialsAndInactiveUser() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "identifier", "admin@dafabi.com.br",
                                "password", "senha-incorreta"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("INVALID_CREDENTIALS")))
                .andExpect(jsonPath("$.detail", is("Usuário ou senha inválidos.")));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "identifier", "marina@dafabi.com.br",
                                "password", "marina123"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("INVALID_CREDENTIALS")))
                .andExpect(jsonPath("$.detail", is("Usuário ou senha inválidos.")));
    }

    @Test
    @DisplayName("Token expirado ou ausente deve ser bloqueado com 401 SESSION_EXPIRED")
    void shouldRejectMissingOrExpiredToken() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("SESSION_EXPIRED")));

        String expiredToken = jwtService.generateAccessToken(
                ADMIN_ID,
                "admin@dafabi.com.br",
                "Fabiana Nogueira",
                UserRole.ADMIN,
                Duration.ofSeconds(-60)
        );

        mockMvc.perform(get("/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("SESSION_EXPIRED")))
                .andExpect(jsonPath("$.detail", is("Sua sessão expirou. Faça login novamente.")));
    }

    @Test
    @DisplayName("Autorização por perfil: CASHIER deve acessar rotas de atendimento e receber 403 ACCESS_DENIED nas administrativas")
    void shouldEnforceRoleBasedAuthorization() throws Exception {
        String cashierToken = obtainAccessToken("caixa", "caixa123");
        String adminToken = obtainAccessToken("admin", "admin123");

        // Rotas permitidas para CASHIER e ADMIN
        mockMvc.perform(get("/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cashierToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cashierToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/inventory")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cashierToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/cash-registers/current")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cashierToken))
                .andExpect(status().isNoContent());

        // Rotas administrativas devem rejeitar CASHIER com 403 ACCESS_DENIED
        mockMvc.perform(get("/dashboard/financial-summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cashierToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));

        mockMvc.perform(get("/suppliers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cashierToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));

        mockMvc.perform(get("/purchases")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cashierToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));

        mockMvc.perform(get("/expenses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cashierToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));

        mockMvc.perform(get("/inventory/lots")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cashierToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));

        String createProductBody = objectMapper.writeValueAsString(Map.of(
                "name", "Frango Assado",
                "categoryId", category.getId().toString(),
                "salePrice", 45.00,
                "currentCost", 25.00,
                "unit", "UN",
                "perishable", true,
                "frequent", true,
                "status", "ACTIVE",
                "initialStock", 10
        ));

        mockMvc.perform(post("/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cashierToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createProductBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));

        // ADMIN deve conseguir acessar as rotas administrativas normalmente
        mockMvc.perform(get("/dashboard/financial-summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createProductBody))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("GET /auth/me, POST /auth/refresh e POST /auth/logout devem gerenciar o ciclo de vida da sessão")
    void shouldHandleMeRefreshAndLogout() throws Exception {
        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "identifier", "admin",
                                "password", "admin123"
                        ))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(loginResponse);
        String accessToken = json.get("accessToken").asText();
        String refreshToken = json.get("refreshToken").asText();

        mockMvc.perform(get("/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(ADMIN_ID.toString())))
                .andExpect(jsonPath("$.role", is("ADMIN")));

        String refreshResponse = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        String nextRefreshToken = objectMapper.readTree(refreshResponse).get("refreshToken").asText();

        // Refresh token anterior foi revogado na rotação
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("SESSION_EXPIRED")));

        // Logout revoga os refresh tokens ativos
        mockMvc.perform(post("/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", nextRefreshToken))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", nextRefreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("SESSION_EXPIRED")));
    }

    private String obtainAccessToken(String identifier, String password) throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "identifier", identifier,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    private void cleanUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        stockMovementRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }
}
