package com.talan.creditplatform.controller.rest;

import com.talan.creditplatform.model.entity.User;
import com.talan.creditplatform.repository.UserRepository;
import com.talan.creditplatform.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.talan.creditplatform.service.EventService;

@RestController
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
@RequestMapping("/api/users")
public class AdminUserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final EventService eventService;

    public AdminUserController(UserRepository userRepository, UserService userService,
                               PasswordEncoder passwordEncoder, EventService eventService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.eventService = eventService;
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User saved = userRepository.save(user);
        eventService.emitUsersChanged();
        eventService.emitAnalystsChanged();
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @Valid @RequestBody User update) {
        Optional<User> updated = userService.updateUser(id, update, passwordEncoder);
        updated.ifPresent(u -> {
            eventService.emitUsersChanged();
            eventService.emitAnalystsChanged();
        });
        return updated.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = userOpt.get();
        if (!user.isFired()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cannot delete employee: not marked as fired by manager."));
        }
        userService.deleteUser(id);
        eventService.emitUsersChanged();
        eventService.emitAnalystsChanged();
        return ResponseEntity.ok(Map.of("message", "User deleted."));
    }

    @PutMapping("/{id}/fire")
    public ResponseEntity<Map<String, String>> fireEmployee(@PathVariable Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = userOpt.get();
        user.setFired(true);
        userRepository.save(user);
        eventService.emitUsersChanged();
        eventService.emitAnalystsChanged();
        return ResponseEntity.ok(Map.of("message", "Employee marked as fired."));
    }
}
