package com.pawsome.rescue.auth;

import com.pawsome.rescue.auth.dto.SignUpDto; // Import the new DTO
import com.pawsome.rescue.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // 1. Initialize the logger for this class
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignUpDto signUpDto) { // Use the DTO
        try {
            authService.signup(signUpDto); // Pass the DTO to the service
            return ResponseEntity.ok(Collections.singletonMap("message", "User signed up successfully. Pending authorization."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        // 2. Log the initial login attempt
        logger.info("Login attempt for username: {}", username);

        try {
            authService.authenticate(username, password);
            // 3. Log successful authentication
            logger.info("User '{}' authenticated successfully.", username);

            final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            final String jwt = jwtUtil.generateToken(userDetails);

            // 4. Log successful JWT generation
            logger.info("JWT token generated for user '{}'.", username);

            return ResponseEntity.ok(Collections.singletonMap("token", jwt));
        } catch (Exception e) {
            // 5. Log any authentication failures
            logger.error("Authentication failed for user '{}'. Reason: {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("message", e.getMessage()));
        }
    }
}