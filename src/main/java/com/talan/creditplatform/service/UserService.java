package com.talan.creditplatform.service;

import com.talan.creditplatform.exception.SoleAdminException;
import com.talan.creditplatform.model.entity.User;
import com.talan.creditplatform.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private static final String ADMIN_ROLE = "admin";
    private static final String SOLE_ADMIN_MESSAGE =
            "Cannot delete or demote the last active admin.";

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void ensureNotSoleAdmin(User target) {
        if (!isAdmin(target)) {
            return;
        }
        if (countActiveAdmins() <= 1) {
            throw new SoleAdminException(SOLE_ADMIN_MESSAGE);
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

    @Transactional
    public Optional<User> updateUser(Long id, User update, PasswordEncoder passwordEncoder) {
        return userRepository.findById(id)
                .map(existing -> {
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
        userRepository.delete(user);
    }

    public long countActiveAdmins() {
        return userRepository.countByRoleIgnoreCase(ADMIN_ROLE);
    }

    public static boolean isAdmin(User user) {
        return user != null && ADMIN_ROLE.equalsIgnoreCase(user.getRole());
    }
}
