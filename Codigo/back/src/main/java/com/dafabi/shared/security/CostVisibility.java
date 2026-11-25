package com.dafabi.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Custo e margem são informações gerenciais: só a Administradora as recebe da API. */
public final class CostVisibility {

    private CostVisibility() {
    }

    public static boolean canSeeCost() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
