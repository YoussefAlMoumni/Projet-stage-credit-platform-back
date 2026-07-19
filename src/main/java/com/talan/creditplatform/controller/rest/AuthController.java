package com.talan.creditplatform.controller.rest;

import com.talan.creditplatform.model.dto.LoginRequest;
import com.talan.creditplatform.model.dto.LoginResponse;
import com.talan.creditplatform.model.dto.RegisterRequest;
import com.talan.creditplatform.model.entity.User;
import com.talan.creditplatform.repository.UserRepository;
import com.talan.creditplatform.security.JwtService;
import com.talan.creditplatform.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiterService rateLimiterService;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RateLimiterService rateLimiterService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String token = jwtService.generateToken(userDetails);
        
        String role = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_USER");

        return ResponseEntity.ok(new LoginResponse(token, role));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        String username = request.getUsername().trim();
        String password = request.getPassword();
        String role = normalizeRole(request.getRole());

        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("message", "That username is already registered."));
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setNationalId(request.getNationalId());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setGender(request.getGender());
        user.setRole(role);

        userRepository.save(user);

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String token = jwtService.generateToken(userDetails);
        String authority = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_MANAGER");

        return ResponseEntity.ok(new LoginResponse(token, authority));
    }

    private String normalizeRole(String requestedRole) {
        if ("ROLE_ADMIN".equals(requestedRole)) {
            return "admin";
        }
        if ("ROLE_ANALYST".equals(requestedRole)) {
            return "analyst";
        }
        return "manager";
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String email = request.get("email");
        String ip = httpRequest.getRemoteAddr();

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
        }

        if (rateLimiterService.isBlocked(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Too many attempts from this IP. Please wait 15 minutes."));
        }
        if (rateLimiterService.isBlocked(email)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Too many attempts for this email. Please wait 15 minutes."));
        }

        rateLimiterService.recordAttempt(ip);
        rateLimiterService.recordAttempt(email);

        Optional<User> userOpt = userRepository.findByEmail(email.trim());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Email address not found."));
        }

        User user = userOpt.get();
        String method = request.get("method");

        if (method == null) {
            // Step 1: user exists, return options
            return ResponseEntity.ok(Map.of(
                    "email", user.getEmail(),
                    "hasPhone", user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank(),
                    "phoneNumber", maskPhone(user.getPhoneNumber())
            ));
        } else {
            // Step 2: method selected, trigger simulated OTP delivery
            return ResponseEntity.ok(Map.of(
                    "message", "OTP successfully sent via " + method,
                    "otp", "123456"
            ));
        }
    }

    @PostMapping("/contact-admin")
    public ResponseEntity<?> contactAdmin(@RequestBody Map<String, String> payload) {
        System.out.println("IT Department contacted by: " + payload);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "IT Department contacted successfully."
        ));
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) return "";
        String p = phone.trim();
        if (p.length() < 5) return p;
        return p.substring(0, 3) + "******" + p.substring(p.length() - 2);
    }
}
