package com.talan.creditplatform.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.talan.creditplatform.model.validation.E164Phone;
import com.talan.creditplatform.model.validation.NationalId;
import com.talan.creditplatform.persistence.UserAdminGuardListener;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
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
    @Email(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", 
            message = "Email must be a valid RFC 5322 email address")
    @NotBlank(message = "Email is required")
    private String email;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username must contain only alphanumeric characters and underscores")
    private String username;

    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "Password is required")
    @Size(min = 12, message = "Password must be at least 12 characters")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{12,}$", 
             message = "Password must contain at least one digit, one uppercase letter, one lowercase letter, and one special character")
    private String password;

    @Column(name = "last_name", nullable = false)
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-ZÀ-ÿ\\s-]+$", message = "Last name must contain only letters, spaces, and hyphens")
    private String lastName;

    @Column(name = "first_name", nullable = false)
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-ZÀ-ÿ\\s-]+$", message = "First name must contain only letters, spaces, and hyphens")
    private String firstName;

    @Column(name = "national_id", unique = true, nullable = false)
    @NationalId(message = "National ID must contain only alphanumeric characters and hyphens, 5-20 characters")
    @NotBlank(message = "National ID is required")
    private String nationalId;

    private String gender;

    @Column(name = "phone_number")
    @E164Phone(message = "Phone number must be in E.164 format (e.g., +1234567890)")
    private String phoneNumber;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(nullable = false)
    private BigDecimal salary;

    private String role;

    @Column(nullable = false)
    private boolean fired = false;

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

    public boolean isFired() {
        return fired;
    }

    public void setFired(boolean fired) {
        this.fired = fired;
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
