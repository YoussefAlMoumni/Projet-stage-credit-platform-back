package com.talan.creditplatform.persistence;

import com.talan.creditplatform.repository.UserRepository;
import com.talan.creditplatform.service.UserService;
import org.springframework.stereotype.Component;

@Component
public class UserAdminGuardListenerConfig {

    public UserAdminGuardListenerConfig(UserRepository userRepository, UserService userService) {
        UserAdminGuardListener.setDependencies(userRepository, userService);
    }
}
