package com.talan.creditplatform.persistence;

import com.talan.creditplatform.model.entity.User;
import com.talan.creditplatform.repository.UserRepository;
import com.talan.creditplatform.service.UserService;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

public class UserAdminGuardListener {

    private static UserRepository userRepository;
    private static UserService userService;

    public static void setDependencies(UserRepository repository, UserService service) {
        userRepository = repository;
        userService = service;
    }

    @PreRemove
    public void beforeDelete(User user) {
        if (userService != null) {
            userService.ensureNotSoleAdmin(user);
        }
    }

    @PreUpdate
    public void beforeRoleUpdate(User user) {
        if (userRepository == null || userService == null || user.getId() == null) {
            return;
        }
        userRepository.findById(user.getId()).ifPresent(existing -> {
            if (UserService.isAdmin(existing) && !UserService.isAdmin(user)) {
                userService.ensureNotSoleAdmin(existing);
            }
        });
    }
}
