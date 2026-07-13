package com.talan.creditplatform.controller.rest;

import com.talan.creditplatform.model.entity.User;
import com.talan.creditplatform.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User saved = userRepository.save(user);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User update) {
        return userRepository.findById(id)
                .map(existing -> {
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
                    if (update.getPassword() != null && !update.getPassword().isBlank()) {
                        existing.setPassword(passwordEncoder.encode(update.getPassword()));
                    }
                    return ResponseEntity.ok(userRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "User deleted."));
    }
}
