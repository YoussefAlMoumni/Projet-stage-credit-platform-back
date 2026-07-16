package com.talan.creditplatform.service;

import com.talan.creditplatform.exception.SoleAdminException;
import com.talan.creditplatform.model.entity.User;
import com.talan.creditplatform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    void deleteUser_blocksWhenTargetIsSoleAdmin() {
        User admin = adminUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleIgnoreCase("admin")).thenReturn(1L);

        assertThrows(SoleAdminException.class, () -> userService.deleteUser(1L));
        verify(userRepository, never()).delete(any());
    }

    @Test
    void deleteUser_allowsWhenOtherAdminsExist() {
        User admin = adminUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleIgnoreCase("admin")).thenReturn(2L);

        assertDoesNotThrow(() -> userService.deleteUser(1L));
        verify(userRepository).delete(admin);
    }

    @Test
    void updateUser_blocksDemotingSoleAdmin() {
        User existing = adminUser(1L);
        User update = managerUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.countByRoleIgnoreCase("admin")).thenReturn(1L);

        assertThrows(SoleAdminException.class,
                () -> userService.updateUser(1L, update, passwordEncoder));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_allowsDemotingAdminWhenOthersExist() {
        User existing = adminUser(1L);
        User update = managerUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.countByRoleIgnoreCase("admin")).thenReturn(2L);
        when(userRepository.save(existing)).thenReturn(existing);

        User saved = userService.updateUser(1L, update, passwordEncoder).orElseThrow();
        assertEquals("manager", saved.getRole());
        verify(userRepository).save(existing);
    }

    private static User adminUser(Long id) {
        User user = new User("admin1", "secret", "admin");
        user.setId(id);
        return user;
    }

    private static User managerUser() {
        User user = new User("manager1", "secret", "manager");
        user.setUsername("manager1");
        user.setEmail("manager1@talan.com");
        return user;
    }
}
