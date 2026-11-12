package com.dafabi.auth.security;

import com.dafabi.shared.security.RequestUser;
import com.dafabi.users.domain.User;
import com.dafabi.users.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR_CODE_ATTR = "dafabi.auth.error.code";
    public static final String AUTH_ERROR_DETAIL_ATTR = "dafabi.auth.error.detail";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            request.setAttribute(AUTH_ERROR_CODE_ATTR, "SESSION_EXPIRED");
            request.setAttribute(AUTH_ERROR_DETAIL_ATTR, "Token de autenticação não informado.");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            JwtClaims claims = jwtService.parseToken(token);

            Optional<User> persistedUser = userRepository.findById(claims.userId());
            if (persistedUser.isPresent() && !persistedUser.get().isActive()) {
                request.setAttribute(AUTH_ERROR_CODE_ATTR, "SESSION_EXPIRED");
                request.setAttribute(AUTH_ERROR_DETAIL_ATTR, "Usuário com acesso bloqueado ou inativo.");
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            RequestUser principal = new RequestUser(claims.userId(), claims.name(), claims.role());
            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + claims.role().name())
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, token, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (ExpiredJwtException ex) {
            request.setAttribute(AUTH_ERROR_CODE_ATTR, "SESSION_EXPIRED");
            request.setAttribute(AUTH_ERROR_DETAIL_ATTR, "Sua sessão expirou. Faça login novamente.");
            SecurityContextHolder.clearContext();
        } catch (JwtException | IllegalArgumentException ex) {
            request.setAttribute(AUTH_ERROR_CODE_ATTR, "SESSION_EXPIRED");
            request.setAttribute(AUTH_ERROR_DETAIL_ATTR, "Token de autenticação inválido.");
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
