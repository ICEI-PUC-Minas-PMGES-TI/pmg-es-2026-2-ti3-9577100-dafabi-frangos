package com.dafabi.users.controller;

import com.dafabi.shared.exception.BusinessException;
import com.dafabi.shared.security.RequestUser;
import com.dafabi.shared.security.UserRole;
import com.dafabi.users.application.UserService;
import com.dafabi.users.domain.User;
import com.dafabi.users.dto.CreateUserRequest;
import com.dafabi.users.dto.SetUserStatusRequest;
import com.dafabi.users.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> findAll() {
        return userService.findAll().stream().map(this::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return toResponse(userService.create(request.email(), request.password(), request.name(), request.role()));
    }

    @PatchMapping("/{id}/status")
    public UserResponse setStatus(@PathVariable UUID id,
                                  @Valid @RequestBody SetUserStatusRequest request,
                                  Authentication authentication) {
        RequestUser current = RequestUser.from(authentication);
        if (!request.active() && current.id().equals(id)) {
            throw new BusinessException("CANNOT_DEACTIVATE_SELF", "Não é possível bloquear o próprio acesso.", HttpStatus.CONFLICT);
        }
        return toResponse(userService.setActive(id, request.active()));
    }

    private UserResponse toResponse(User user) {
        UserRole role = user.getRoles().stream()
                .map(roleEntity -> roleEntity.getName().replace("ROLE_", ""))
                .map(UserRole::valueOf)
                .findFirst()
                .orElse(UserRole.CASHIER);
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), role, user.isActive(), user.getCreatedAt());
    }
}
