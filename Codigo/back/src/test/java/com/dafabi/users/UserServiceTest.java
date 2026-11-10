package com.dafabi.users;

import com.dafabi.shared.exception.BusinessException;
import com.dafabi.shared.security.UserRole;
import com.dafabi.users.application.UserService;
import com.dafabi.users.domain.Role;
import com.dafabi.users.domain.User;
import com.dafabi.users.repository.RoleRepository;
import com.dafabi.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void shouldHashPasswordAndPersistAdminRole() {
        Role role = new Role(UUID.randomUUID(), "ROLE_ADMIN");
        when(userRepository.existsByEmail("admin@dafabi.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("plain-password")).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = service.create(" ADMIN@DAFABI.COM ", "plain-password", "Admin", UserRole.ADMIN);

        assertThat(user.getEmail()).isEqualTo("admin@dafabi.com");
        assertThat(user.getPassword()).isEqualTo("$2a$hashed");
        assertThat(user.getPassword()).isNotEqualTo("plain-password");
        assertThat(user.getRoles()).extracting(Role::getName).containsExactly("ROLE_ADMIN");
    }

    @Test
    void shouldRejectInvalidEmailAndDuplicateEmail() {
        assertThatThrownBy(() -> service.create("invalid", "password", "User", UserRole.CASHIER))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("e-mail válido");

        when(userRepository.existsByEmail("user@dafabi.com")).thenReturn(true);
        assertThatThrownBy(() -> service.create("user@dafabi.com", "password", "User", UserRole.CASHIER))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Já existe um usuário cadastrado com este e-mail.");
    }

    @Test
    void shouldDeactivateExistingUser() {
        User user = new User(null, "user@dafabi.com", "$2a$hashed", "User", java.util.Set.of());
        when(userRepository.findByEmail("user@dafabi.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User deactivated = service.deactivate("user@dafabi.com");

        assertThat(deactivated.isActive()).isFalse();
    }
}
