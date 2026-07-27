package com.talan.creditplatform.repository;

import com.talan.creditplatform.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findByRoleOrderByLastNameAscFirstNameAsc(String role);
    long countByRoleIgnoreCase(String role);
    Optional<User> findByNationalId(String nationalId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByNationalId(String nationalId);
}
