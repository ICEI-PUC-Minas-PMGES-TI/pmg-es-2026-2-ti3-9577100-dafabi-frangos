package com.dafabi.shared.security;

import com.dafabi.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.util.UUID;

/**
 * Usuário responsável pela requisição, extraído do token JWT autenticado
 * no contexto de segurança.
 */
public record RequestUser(UUID id, String name, UserRole role) {

    /**
     * Obtém o usuário autenticado pelo token JWT. Os dados vêm sempre do token assinado,
     * nunca de valores enviados pelo cliente, garantindo a autoria das operações.
     */
    public static RequestUser from(Authentication authentication) {
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof RequestUser requestUser) {
            return requestUser;
        }
        throw new BusinessException(
                "SESSION_EXPIRED",
                "Sua sessão expirou ou você não está autenticado. Faça login novamente.",
                HttpStatus.UNAUTHORIZED);
    }

    public void requireRole(UserRole requiredRole, String message) {
        if (role != requiredRole) {
            throw new BusinessException("ACCESS_DENIED", message, HttpStatus.FORBIDDEN);
        }
    }
}
