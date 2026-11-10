package com.dafabi.users.application;

import com.dafabi.shared.exception.BusinessException;
import com.dafabi.shared.security.UserRole;
import com.dafabi.users.domain.Role;
import com.dafabi.users.domain.User;
import com.dafabi.users.repository.RoleRepository;
import com.dafabi.users.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User create(String email, String rawPassword, String name, UserRole role) {
        String normalizedEmail = normalizeEmail(email);
        validatePassword(rawPassword);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException("USER_EMAIL_ALREADY_EXISTS",
                    "Já existe um usuário cadastrado com este e-mail.", HttpStatus.CONFLICT);
        }
        if (role == null) {
            throw new BusinessException("INVALID_ROLE", "O perfil do usuário é obrigatório.");
        }

        Role persistedRole = roleRepository.findByName(roleName(role))
                .orElseGet(() -> roleRepository.save(new Role(null, roleName(role))));
        User user = new User(null, normalizedEmail, passwordEncoder.encode(rawPassword), name,
                Set.of(persistedRole));
        return userRepository.save(user);
    }

    @Transactional
    public User deactivate(String email) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND",
                        "Usuário não encontrado.", HttpStatus.NOT_FOUND));
        user.deactivate();
        return userRepository.save(user);
    }

    private static String normalizeEmail(String email) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        if (normalizedEmail.isBlank() || !normalizedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new BusinessException("INVALID_EMAIL", "Informe um e-mail válido.");
        }
        return normalizedEmail;
    }

    private static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new BusinessException("INVALID_PASSWORD", "A senha é obrigatória.");
        }
    }

    private static String roleName(UserRole role) {
        return "ROLE_" + role.name();
    }

}
