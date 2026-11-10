package com.dafabi.users;

import com.dafabi.users.domain.Role;
import com.dafabi.users.domain.User;
import com.dafabi.users.repository.RoleRepository;
import com.dafabi.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UserRolePersistenceIT {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
    }

    @Test
    void shouldPersistUserWithRolesAndFindByEmail() {
        Role role = roleRepository.saveAndFlush(new Role(null, "ROLE_ADMIN"));
        User user = userRepository.saveAndFlush(new User(
                null,
                "admin@dafabi.com",
                "hashed-password",
                "Administrador",
                Set.of(role)));

        User persisted = userRepository.findByEmail("admin@dafabi.com").orElseThrow();

        assertThat(persisted.getId()).isEqualTo(user.getId());
        assertThat(persisted.getRoles()).extracting(Role::getName)
                .containsExactly("ROLE_ADMIN");
        assertThat(userRepository.existsByEmail("admin@dafabi.com")).isTrue();
        assertThat(roleRepository.findByName("ROLE_ADMIN")).isPresent();
    }
}
