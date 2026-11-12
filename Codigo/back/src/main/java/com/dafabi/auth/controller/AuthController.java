package com.dafabi.auth.controller;

import com.dafabi.auth.application.AuthService;
import com.dafabi.auth.dto.AuthTokenResponse;
import com.dafabi.auth.dto.AuthenticatedUserResponse;
import com.dafabi.auth.dto.LoginRequest;
import com.dafabi.auth.dto.LogoutRequest;
import com.dafabi.auth.dto.RefreshTokenRequest;
import com.dafabi.shared.security.RequestUser;
import com.dafabi.shared.security.UserRole;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            Authentication authentication,
            @RequestBody(required = false) LogoutRequest request) {
        RequestUser currentUser = extractRequestUser(authentication);
        authService.logout(currentUser, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthenticatedUserResponse> me(Authentication authentication) {
        RequestUser currentUser = extractRequestUser(authentication);
        return ResponseEntity.ok(authService.me(currentUser));
    }

    private static RequestUser extractRequestUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof RequestUser requestUser) {
            return requestUser;
        }
        if (principal instanceof UserDetails userDetails) {
            boolean isAdmin = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(auth -> "ROLE_ADMIN".equals(auth) || "ADMIN".equals(auth));
            UUID id = UUID.nameUUIDFromBytes(userDetails.getUsername().getBytes(StandardCharsets.UTF_8));
            return new RequestUser(id, userDetails.getUsername(), isAdmin ? UserRole.ADMIN : UserRole.CASHIER);
        }
        return null;
    }
}
