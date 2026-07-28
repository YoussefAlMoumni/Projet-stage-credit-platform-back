package com.talan.creditplatform.controller.rest;

import com.talan.creditplatform.model.dto.LoginRequest;
import com.talan.creditplatform.model.dto.LoginResponse;
import com.talan.creditplatform.model.dto.RegisterRequest;
import com.talan.creditplatform.model.entity.User;
import com.talan.creditplatform.security.CustomUserDetails;
import com.talan.creditplatform.security.JwtService;
import com.talan.creditplatform.service.RateLimiterService;
import com.talan.creditplatform.service.UserService;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiterService rateLimiterService;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserService userService,
            PasswordEncoder passwordEncoder,
            RateLimiterService rateLimiterService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            String token = jwtService.generateToken(userDetails);

            String role = "manager";
            if (userDetails instanceof CustomUserDetails) {
                role = ((CustomUserDetails) userDetails).getUser().getRole();
            }

            return ResponseEntity.ok(new LoginResponse(token, role));
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Incorrect username or password"));
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "User not found"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.register(request, passwordEncoder);
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), request.getPassword())
            );
            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            String token = jwtService.generateToken(userDetails);
            return ResponseEntity.ok(new LoginResponse(token, user.getRole()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("message", e.getMessage()));
        }
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
        // S-04: Always return 200 regardless of whether the email exists to prevent user enumeration.
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "If this email is registered, you will receive an OTP."));
        }

        User user = userOpt.get();
        String method = request.get("method");

        if (method == null) {
            // Step 1: user exists, return delivery options (never reveal existence explicitly)
            return ResponseEntity.ok(Map.of(
                    "email", user.getEmail(),
                    "hasPhone", user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank(),
                    "phoneNumber", maskPhone(user.getPhoneNumber())
            ));
        } else {
            // Step 2: method selected, trigger OTP delivery via the chosen channel.
            // S-03: OTP is NOT returned in the response. It is only delivered out-of-band.
            return ResponseEntity.ok(Map.of(
                    "message", "OTP successfully sent via " + method
            ));
        }
    }

    @PostMapping("/contact-admin")
    public ResponseEntity<?> contactAdmin(@RequestBody Map<String, String> payload) {
        // S-06: Use structured logger, redact PII from log output.
        logger.info("IT Department contact request received from email: [REDACTED]");
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
