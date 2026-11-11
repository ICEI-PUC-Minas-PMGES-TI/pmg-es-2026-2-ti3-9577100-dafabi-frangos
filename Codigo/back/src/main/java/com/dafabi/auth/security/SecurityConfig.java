package com.dafabi.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/refresh").permitAll()
                        .requestMatchers("/auth/logout", "/auth/me").authenticated()

                        // Produtos e categorias
                        .requestMatchers(HttpMethod.GET, "/categories", "/categories/**").hasAnyRole("ADMIN", "CASHIER")
                        .requestMatchers(HttpMethod.GET, "/products", "/products/*").hasAnyRole("ADMIN", "CASHIER")
                        .requestMatchers(HttpMethod.GET, "/products/by-barcode/*").hasAnyRole("ADMIN", "CASHIER")
                        .requestMatchers(HttpMethod.POST, "/products").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/products/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/products/*/status").hasRole("ADMIN")

                        // Estoque e perecíveis
                        .requestMatchers(HttpMethod.GET, "/inventory").hasAnyRole("ADMIN", "CASHIER")
                        .requestMatchers("/inventory/**").hasRole("ADMIN")

                        // Caixa
                        .requestMatchers("/cash-registers", "/cash-registers/**").hasAnyRole("ADMIN", "CASHIER")

                        // Estornos e vendas
                        .requestMatchers("/sales/*/refund", "/sales/*/refunds", "/sales/*/refund-eligibility").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/sales/by-number/*").hasRole("ADMIN")
                        .requestMatchers("/refunds", "/refunds/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/sales").hasAnyRole("ADMIN", "CASHIER")
                        .requestMatchers(HttpMethod.GET, "/sales", "/sales/*").hasAnyRole("ADMIN", "CASHIER")

                        // Áreas exclusivas da Administradora
                        .requestMatchers(
                                "/dashboard", "/dashboard/**",
                                "/suppliers", "/suppliers/**",
                                "/purchases", "/purchases/**",
                                "/expenses", "/expenses/**",
                                "/integrations", "/integrations/**",
                                "/settings", "/settings/**",
                                "/users", "/users/**",
                                "/roles", "/roles/**",
                                "/permissions", "/permissions/**"
                        ).hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            String code = attributeOrDefault(
                    request,
                    JwtAuthenticationFilter.AUTH_ERROR_CODE_ATTR,
                    "SESSION_EXPIRED"
            );
            String detail = attributeOrDefault(
                    request,
                    JwtAuthenticationFilter.AUTH_ERROR_DETAIL_ATTR,
                    "Sua sessão expirou ou você não está autenticado. Faça login novamente."
            );
            writeProblemDetail(response, HttpStatus.UNAUTHORIZED, "Não autenticado", code, detail);
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> writeProblemDetail(
                response,
                HttpStatus.FORBIDDEN,
                "Acesso negado",
                "ACCESS_DENIED",
                "Acesso negado. Seu perfil não possui permissão para realizar esta operação."
        );
    }

    private void writeProblemDetail(
            HttpServletResponse response,
            HttpStatus status,
            String title,
            String code,
            String detail) throws IOException {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setTitle(title);
        problemDetail.setProperty("code", code);
        problemDetail.setProperty("traceId", UUID.randomUUID().toString());

        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problemDetail);
    }

    private static String attributeOrDefault(HttpServletRequest request, String attribute, String fallback) {
        Object value = request.getAttribute(attribute);
        if (value instanceof String str && !str.isBlank()) {
            return str;
        }
        return fallback;
    }
}
