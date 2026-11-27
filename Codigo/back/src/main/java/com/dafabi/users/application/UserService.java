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
import java.util.List;
import java.util.UUID;

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

    @Transactional
    public User setActive(UUID id, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Usuário não encontrado.", HttpStatus.NOT_FOUND));
        if (active) user.activate(); else user.deactivate();
        return userRepository.save(user);
    }

    /** Altera nome e perfil; mantém pelo menos uma Administradora ativa. */
    @Transactional
    public User update(UUID id, String name, UserRole role, UUID currentUserId) {
        User user = findUser(id);
        if (role == null) {
            throw new BusinessException("INVALID_ROLE", "O perfil do usuário é obrigatório.");
        }
        boolean demotingAdmin = isAdmin(user) && role != UserRole.ADMIN;
        if (demotingAdmin && id.equals(currentUserId)) {
            throw new BusinessException("CANNOT_DEMOTE_SELF", "Não é possível remover o seu próprio perfil de Administradora.",
                    HttpStatus.CONFLICT);
        }
        if (demotingAdmin && userRepository.findAll().stream().filter(other -> other.isActive() && isAdmin(other)).count() <= 1) {
            throw new BusinessException("LAST_ADMIN", "O sistema precisa de pelo menos uma Administradora ativa.",
                    HttpStatus.CONFLICT);
        }
        Role persistedRole = roleRepository.findByName(roleName(role))
                .orElseGet(() -> roleRepository.save(new Role(null, roleName(role))));
        user.rename(name.trim());
        user.replaceRoles(Set.of(persistedRole));
        return userRepository.save(user);
    }

    /** A Administradora define uma senha temporária para quem esqueceu a própria. */
    @Transactional
    public User resetPassword(UUID id, String newPassword) {
        User user = findUser(id);
        validatePassword(newPassword);
        user.changePassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    @Transactional
    public void changeOwnPassword(UUID id, String currentPassword, String newPassword) {
        User user = findUser(id);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException("INVALID_CURRENT_PASSWORD", "A senha atual não confere.");
        }
        validatePassword(newPassword);
        user.changePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private User findUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Usuário não encontrado.", HttpStatus.NOT_FOUND));
    }

    private static boolean isAdmin(User user) {
        return user.getRoles().stream().anyMatch(role -> "ROLE_ADMIN".equalsIgnoreCase(role.getName()));
    }

    @Transactional
    public List<User> findAll() {
        return userRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
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
