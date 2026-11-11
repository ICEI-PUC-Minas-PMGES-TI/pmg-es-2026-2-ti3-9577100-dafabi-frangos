package com.dafabi.auth;

import com.dafabi.auth.application.AuthService;
import com.dafabi.auth.domain.RefreshToken;
import com.dafabi.auth.dto.AuthTokenResponse;
import com.dafabi.auth.dto.LoginRequest;
import com.dafabi.auth.dto.RefreshTokenRequest;
import com.dafabi.auth.repository.RefreshTokenRepository;
import com.dafabi.auth.security.JwtClaims;
import com.dafabi.auth.security.JwtService;
import com.dafabi.shared.exception.BusinessException;
import com.dafabi.shared.security.UserRole;
import com.dafabi.users.domain.Role;
import com.dafabi.users.domain.User;
import com.dafabi.users.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        jwtService = new JwtService(
                "test-jwt-secret-key-with-more-than-32-characters-for-hmac-sha256",
                3600,
                86400
        );
        authService = new AuthService(userRepository, refreshTokenRepository, passwordEncoder, jwtService);
    }

    @Test
    @DisplayName("Deve autenticar usuário ADMIN válido e retornar JWT com perfil e permissões")
    void shouldAuthenticateValidAdmin() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000101");
        Role adminRole = new Role(UUID.randomUUID(), "ROLE_ADMIN");
        User admin = new User(
                userId,
                "admin@dafabi.com.br",
                passwordEncoder.encode("admin123"),
                "Fabiana Nogueira",
                Set.of(adminRole)
        );

        when(userRepository.findByEmailIgnoreCase("admin")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("admin@dafabi.com.br")).thenReturn(Optional.of(admin));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthTokenResponse response = authService.login(new LoginRequest("admin", null, null, "admin123"));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.token()).isEqualTo(response.accessToken());
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().id()).isEqualTo(userId);
        assertThat(response.user().name()).isEqualTo("Fabiana Nogueira");
        assertThat(response.user().email()).isEqualTo("admin@dafabi.com.br");
        assertThat(response.user().role()).isEqualTo(UserRole.ADMIN);
        assertThat(response.user().permissions()).contains("DASHBOARD_READ", "REFUND_CREATE", "SETTINGS_MANAGE");

        JwtClaims claims = jwtService.parseToken(response.accessToken());
        assertThat(claims.userId()).isEqualTo(userId);
        assertThat(claims.role()).isEqualTo(UserRole.ADMIN);
        assertThat(claims.email()).isEqualTo("admin@dafabi.com.br");
    }

    @Test
    @DisplayName("Deve retornar erro genérico INVALID_CREDENTIALS quando senha for inválida")
    void shouldRejectInvalidPasswordWithGenericError() {
        Role cashierRole = new Role(UUID.randomUUID(), "ROLE_CASHIER");
        User cashier = new User(
                UUID.randomUUID(),
                "caixa@dafabi.com.br",
                passwordEncoder.encode("caixa123"),
                "Carlos Oliveira",
                Set.of(cashierRole)
        );

        when(userRepository.findByEmailIgnoreCase("caixa@dafabi.com.br")).thenReturn(Optional.of(cashier));

        assertThatThrownBy(() -> authService.login(new LoginRequest("caixa@dafabi.com.br", null, null, "senha-errada")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("INVALID_CREDENTIALS");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(be.getMessage()).isEqualTo("Usuário ou senha inválidos.");
                });
    }

    @Test
    @DisplayName("Deve rejeitar usuário inativo com erro genérico INVALID_CREDENTIALS")
    void shouldRejectInactiveUser() {
        Role cashierRole = new Role(UUID.randomUUID(), "ROLE_CASHIER");
        User inactive = new User(
                UUID.randomUUID(),
                "marina@dafabi.com.br",
                passwordEncoder.encode("marina123"),
                "Marina Souza",
                Set.of(cashierRole)
        );
        inactive.deactivate();

        when(userRepository.findByEmailIgnoreCase("marina@dafabi.com.br")).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> authService.login(new LoginRequest("marina@dafabi.com.br", null, null, "marina123")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("INVALID_CREDENTIALS");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
    }

    @Test
    @DisplayName("Deve rejeitar token JWT expirado ao validar assinatura e validade")
    void shouldRejectExpiredJwtToken() {
        String expiredToken = jwtService.generateAccessToken(
                UUID.randomUUID(),
                "admin@dafabi.com.br",
                "Fabiana Nogueira",
                UserRole.ADMIN,
                Duration.ofSeconds(-10)
        );

        assertThatThrownBy(() -> jwtService.parseToken(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Deve renovar sessão com refresh token válido e revogar o anterior")
    void shouldRefreshSessionAndRevokeOldToken() {
        Role cashierRole = new Role(UUID.randomUUID(), "ROLE_CASHIER");
        User cashier = new User(
                UUID.randomUUID(),
                "caixa@dafabi.com.br",
                passwordEncoder.encode("caixa123"),
                "Carlos Oliveira",
                Set.of(cashierRole)
        );
        RefreshToken stored = new RefreshToken(
                UUID.randomUUID(),
                cashier,
                "any-hash",
                OffsetDateTime.now().plusHours(2)
        );

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthTokenResponse refreshed = authService.refresh(new RefreshTokenRequest("raw-refresh-token"));

        assertThat(stored.isRevoked()).isTrue();
        assertThat(refreshed.accessToken()).isNotBlank();
        assertThat(refreshed.user().role()).isEqualTo(UserRole.CASHIER);
        verify(refreshTokenRepository, org.mockito.Mockito.atLeast(2)).save(any(RefreshToken.class));
    }
}
