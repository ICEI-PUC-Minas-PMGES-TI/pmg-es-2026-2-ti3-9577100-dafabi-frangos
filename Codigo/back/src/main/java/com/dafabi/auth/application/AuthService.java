package com.dafabi.auth.application;

import com.dafabi.auth.domain.RefreshToken;
import com.dafabi.auth.dto.AuthTokenResponse;
import com.dafabi.auth.dto.AuthenticatedUserResponse;
import com.dafabi.auth.dto.LoginRequest;
import com.dafabi.auth.dto.LogoutRequest;
import com.dafabi.auth.dto.RefreshTokenRequest;
import com.dafabi.auth.repository.RefreshTokenRepository;
import com.dafabi.auth.security.JwtService;
import com.dafabi.shared.exception.BusinessException;
import com.dafabi.shared.security.RequestUser;
import com.dafabi.shared.security.UserRole;
import com.dafabi.users.domain.Role;
import com.dafabi.users.domain.User;
import com.dafabi.users.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final List<String> ADMIN_PERMISSIONS = List.of(
            "DASHBOARD_READ",
            "SALE_CREATE",
            "SALE_READ",
            "PRODUCT_READ",
            "PRODUCT_WRITE",
            "STOCK_ADJUST",
            "CASH_OPERATE",
            "PURCHASE_MANAGE",
            "EXPENSE_MANAGE",
            "INTEGRATION_MANAGE",
            "REFUND_CREATE",
            "SETTINGS_MANAGE"
    );

    private static final List<String> CASHIER_PERMISSIONS = List.of(
            "SALE_CREATE",
            "SALE_READ",
            "PRODUCT_READ",
            "CASH_OPERATE"
    );

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthTokenResponse login(LoginRequest request) {
        String identifier = request.resolvedIdentifier();
        if (identifier.isBlank()) {
            throw new BusinessException(
                    "INVALID_DATA",
                    "Informe seu usuário ou e-mail.",
                    HttpStatus.BAD_REQUEST
            );
        }

        User user = findUserByIdentifier(identifier)
                .orElseThrow(AuthService::invalidCredentials);

        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw invalidCredentials();
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthTokenResponse refresh(RefreshTokenRequest request) {
        String rawRefreshToken = request.refreshToken() == null ? "" : request.refreshToken().trim();
        if (rawRefreshToken.isEmpty()) {
            throw sessionExpired();
        }

        String tokenHash = sha256(rawRefreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(AuthService::sessionExpired);

        if (storedToken.isRevoked() || storedToken.isExpired()) {
            throw sessionExpired();
        }

        User user = storedToken.getUser();
        if (user == null || !user.isActive()) {
            storedToken.revoke();
            refreshTokenRepository.save(storedToken);
            throw sessionExpired();
        }

        storedToken.revoke();
        refreshTokenRepository.save(storedToken);

        return issueTokens(user);
    }

    @Transactional
    public void logout(RequestUser currentUser, LogoutRequest request) {
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            String tokenHash = sha256(request.refreshToken().trim());
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                token.revoke();
                refreshTokenRepository.save(token);
            });
        }

        if (currentUser != null && currentUser.id() != null) {
            List<RefreshToken> activeTokens = refreshTokenRepository
                    .findAllByUser_IdAndRevokedAtIsNull(currentUser.id());
            for (RefreshToken token : activeTokens) {
                token.revoke();
            }
            refreshTokenRepository.saveAll(activeTokens);
        }
    }

    @Transactional(readOnly = true)
    public AuthenticatedUserResponse me(RequestUser currentUser) {
        if (currentUser == null || currentUser.id() == null) {
            throw sessionExpired();
        }

        Optional<User> persisted = userRepository.findById(currentUser.id());
        if (persisted.isPresent()) {
            User user = persisted.get();
            if (!user.isActive()) {
                throw sessionExpired();
            }
            UserRole role = resolveRole(user);
            return toUserResponse(user, role);
        }

        return new AuthenticatedUserResponse(
                currentUser.id(),
                currentUser.name(),
                currentUser.role() == UserRole.ADMIN ? "admin@dafabi.com.br" : "caixa@dafabi.com.br",
                currentUser.role(),
                true,
                permissionsFor(currentUser.role())
        );
    }

    private AuthTokenResponse issueTokens(User user) {
        UserRole role = resolveRole(user);
        String accessToken = jwtService.generateAccessToken(user, role);
        long expiresIn = jwtService.getAccessTokenExpirationSeconds();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusSeconds(expiresIn);

        String rawRefreshToken = UUID.randomUUID() + "." + UUID.randomUUID();
        OffsetDateTime refreshExpiresAt = OffsetDateTime.now()
                .plusSeconds(jwtService.getRefreshTokenExpirationSeconds());

        RefreshToken refreshToken = new RefreshToken(
                null,
                user,
                sha256(rawRefreshToken),
                refreshExpiresAt
        );
        refreshTokenRepository.save(refreshToken);

        return new AuthTokenResponse(
                accessToken,
                accessToken,
                rawRefreshToken,
                "Bearer",
                expiresIn,
                expiresAt,
                toUserResponse(user, role)
        );
    }

    private Optional<User> findUserByIdentifier(String rawIdentifier) {
        String normalized = rawIdentifier.trim().toLowerCase();
        Optional<User> exact = userRepository.findByEmailIgnoreCase(normalized);
        if (exact.isPresent()) {
            return exact;
        }
        if (!normalized.contains("@")) {
            Optional<User> comBr = userRepository.findByEmailIgnoreCase(normalized + "@dafabi.com.br");
            if (comBr.isPresent()) {
                return comBr;
            }
            return userRepository.findByEmailIgnoreCase(normalized + "@dafabi.com");
        }
        return Optional.empty();
    }

    public static UserRole resolveRole(User user) {
        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                String name = role.getName() == null ? "" : role.getName().trim().toUpperCase();
                if ("ROLE_ADMIN".equals(name) || "ADMIN".equals(name)) {
                    return UserRole.ADMIN;
                }
            }
        }
        return UserRole.CASHIER;
    }

    public static List<String> permissionsFor(UserRole role) {
        return role == UserRole.ADMIN ? ADMIN_PERMISSIONS : CASHIER_PERMISSIONS;
    }

    private static AuthenticatedUserResponse toUserResponse(User user, UserRole role) {
        return new AuthenticatedUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                role,
                user.isActive(),
                permissionsFor(role)
        );
    }

    private static BusinessException invalidCredentials() {
        return new BusinessException(
                "INVALID_CREDENTIALS",
                "Usuário ou senha inválidos.",
                HttpStatus.UNAUTHORIZED
        );
    }

    private static BusinessException sessionExpired() {
        return new BusinessException(
                "SESSION_EXPIRED",
                "Sua sessão expirou. Faça login novamente.",
                HttpStatus.UNAUTHORIZED
        );
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível.", ex);
        }
    }
}
