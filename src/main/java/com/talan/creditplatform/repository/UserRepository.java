package com.talan.creditplatform.repository;

import com.talan.creditplatform.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    List<User> findByRoleOrderByLastNameAscFirstNameAsc(String role);
    long countByRoleIgnoreCase(String role);
}
