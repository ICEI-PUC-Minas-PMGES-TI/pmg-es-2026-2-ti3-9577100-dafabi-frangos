package com.dafabi.shared.security;

import com.dafabi.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Usuário responsável pela requisição, identificado pelo token JWT autenticado
 * no contexto de segurança ou pelos cabeçalhos enviados pelo cliente.
 */
public record RequestUser(UUID id, String name, UserRole role) {

    public static final String ID_HEADER = "X-User-Id";
    public static final String NAME_HEADER = "X-User-Name";
    public static final String ROLE_HEADER = "X-User-Role";

    private static final String DEFAULT_NAME = "Usuário local";
    private static final int MAX_NAME_LENGTH = 150;

    public static RequestUser resolve(String headerId, String headerName, String headerRole) {
        if (!isBlank(headerId) || !isBlank(headerRole)) {
            return fromHeaders(headerId, headerName, headerRole);
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof RequestUser requestUser) {
            return requestUser;
        }
        throw unauthenticated();
    }

    public static RequestUser fromHeaders(String id, String name, String role) {
        if (isBlank(id) || isBlank(role)) {
            throw unauthenticated();
        }

        UUID userId;
        UserRole userRole;
        try {
            userId = UUID.fromString(id.trim());
            userRole = UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw unauthenticated();
        }

        String userName = isBlank(name) ? DEFAULT_NAME : name.trim();
        if (userName.length() > MAX_NAME_LENGTH) {
            userName = userName.substring(0, MAX_NAME_LENGTH);
        }
        return new RequestUser(userId, userName, userRole);
    }

    public void requireRole(UserRole requiredRole, String message) {
        if (role != requiredRole) {
            throw new BusinessException("FORBIDDEN", message, HttpStatus.FORBIDDEN);
        }
    }

    private static BusinessException unauthenticated() {
        return new BusinessException(
                "UNAUTHENTICATED",
                "Usuário responsável não identificado. Faça login novamente.",
                HttpStatus.UNAUTHORIZED);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
