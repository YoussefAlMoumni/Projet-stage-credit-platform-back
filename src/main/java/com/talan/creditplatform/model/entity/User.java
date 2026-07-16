package com.talan.creditplatform.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.talan.creditplatform.persistence.UserAdminGuardListener;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "employee")
@EntityListeners(UserAdminGuardListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "national_id", unique = true, nullable = false)
    private String nationalId;

    private String gender;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(nullable = false)
    private BigDecimal salary;

    private String role;

    public User() {}

    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        setRole(role);
        applyDefaults();
    }

    @PrePersist
    @PreUpdate
    protected void applyDefaults() {
        if (email == null || email.isBlank()) {
            email = username + "@talan.com";
        }
        if (lastName == null || lastName.isBlank()) {
            lastName = username;
        }
        if (firstName == null || firstName.isBlank()) {
            firstName = username;
        }
        if (nationalId == null || nationalId.isBlank()) {
            nationalId = "NID-" + username;
        }
        if (hireDate == null) {
            hireDate = LocalDate.now();
        }
        if (salary == null) {
            salary = BigDecimal.ZERO;
        }
        if (role == null || role.isBlank()) {
            role = "manager";
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        if ("ROLE_ADMIN".equals(role)) {
            this.role = "admin";
        } else if ("ROLE_ANALYST".equals(role)) {
            this.role = "analyst";
        } else if ("ROLE_BANQUIER".equals(role) || "ROLE_MANAGER".equals(role)) {
            this.role = "manager";
        } else {
            this.role = role;
        }
    }

    public String getAuthority() {
        if ("admin".equalsIgnoreCase(role)) {
            return "ROLE_ADMIN";
        }
        if ("analyst".equalsIgnoreCase(role)) {
            return "ROLE_ANALYST";
        }
        return "ROLE_MANAGER";
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
