package com.talan.creditplatform.service;

import com.talan.creditplatform.exception.ConflictException;
import com.talan.creditplatform.model.entity.User;
import com.talan.creditplatform.repository.DossierRepository;
import com.talan.creditplatform.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.time.LocalDate;
import java.math.BigDecimal;

@Service
public class UserService {

    private static final String ADMIN_ROLE = "admin";
    private static final String SOLE_ADMIN_MESSAGE =
            "Cannot delete or demote the last active admin.";

    private final UserRepository userRepository;
    private final DossierRepository dossierRepository;

    public UserService(UserRepository userRepository, DossierRepository dossierRepository) {
        this.userRepository = userRepository;
        this.dossierRepository = dossierRepository;
    }

    public void ensureNotSoleAdmin(User target) {
        if (!isAdmin(target)) {
            return;
        }
        if (countActiveAdmins() <= 1) {
            throw new ConflictException(SOLE_ADMIN_MESSAGE);
        }
    }

    public void ensureRoleChangeAllowed(User existing, String newRole) {
        if (!isAdmin(existing)) {
            return;
        }
        User roleProbe = new User();
        roleProbe.setRole(newRole);
        if (!isAdmin(roleProbe)) {
            ensureNotSoleAdmin(existing);
        }
    }

    private void checkUniqueness(String username, String email, String nationalId, Long excludeId) {
        if (username != null && userRepository.existsByUsername(username)) {
            userRepository.findByUsername(username).ifPresent(u -> {
                if (!u.getId().equals(excludeId)) {
                    throw new ConflictException("Username '" + username + "' is already taken.");
                }
            });
        }
        if (email != null && userRepository.existsByEmail(email)) {
            userRepository.findByEmail(email).ifPresent(u -> {
                if (!u.getId().equals(excludeId)) {
                    throw new ConflictException("Email '" + email + "' is already in use.");
                }
            });
        }
        if (nationalId != null && userRepository.existsByNationalId(nationalId)) {
            userRepository.findByNationalId(nationalId).ifPresent(u -> {
                if (!u.getId().equals(excludeId)) {
                    throw new ConflictException("National ID '" + nationalId + "' is already registered.");
                }
            });
        }
    }

    @Transactional
    public User createUser(User user, PasswordEncoder passwordEncoder) {
        checkUniqueness(user.getUsername(), user.getEmail(), user.getNationalId(), null);

        // Apply explicit defaults previously handled by @PrePersist
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            user.setEmail(user.getUsername() + "@talan.com");
        }
        if (user.getLastName() == null || user.getLastName().isBlank()) {
            user.setLastName(user.getUsername());
        }
        if (user.getFirstName() == null || user.getFirstName().isBlank()) {
            user.setFirstName(user.getUsername());
        }
        if (user.getNationalId() == null || user.getNationalId().isBlank()) {
            user.setNationalId("NID-" + user.getUsername());
        }
        if (user.getHireDate() == null) {
            user.setHireDate(LocalDate.now());
        }
        if (user.getSalary() == null) {
            user.setSalary(BigDecimal.ZERO);
        }
        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("manager");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Transactional
    public Optional<User> updateUser(Long id, User update, PasswordEncoder passwordEncoder) {
        return userRepository.findById(id)
                .map(existing -> {
                    checkUniqueness(update.getUsername(), update.getEmail(), update.getNationalId(), id);
                    ensureRoleChangeAllowed(existing, update.getRole());

                    existing.setUsername(update.getUsername());
                    existing.setEmail(update.getEmail());
                    existing.setFirstName(update.getFirstName());
                    existing.setLastName(update.getLastName());
                    existing.setNationalId(update.getNationalId());
                    existing.setGender(update.getGender());
                    existing.setPhoneNumber(update.getPhoneNumber());
                    existing.setHireDate(update.getHireDate());
                    existing.setSalary(update.getSalary());
                    existing.setRole(update.getRole());
                    existing.setFired(update.isFired());
                    if (update.getPassword() != null && !update.getPassword().isBlank()) {
                        existing.setPassword(passwordEncoder.encode(update.getPassword()));
                    }
                    return userRepository.save(existing);
                });
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        ensureNotSoleAdmin(user);
        // Null out FK references in dossier table before deleting the user.
        // Without this, the DB throws a FK constraint violation (→ HTTP 500)
        // because assigned_analyst_id / approved_by_id point to this user row.
        dossierRepository.clearAssignedAnalystById(id);
        dossierRepository.clearApprovedByById(id);
        userRepository.delete(user);
    }

    public long countActiveAdmins() {
        return userRepository.countByRoleIgnoreCase(ADMIN_ROLE);
    }

    public static boolean isAdmin(User user) {
        return user != null && ADMIN_ROLE.equalsIgnoreCase(user.getRole());
    }
}
