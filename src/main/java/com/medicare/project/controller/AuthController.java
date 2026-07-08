package com.medicare.project.controller;

import com.medicare.project.entity.Role;
import com.medicare.project.entity.User;
import com.medicare.project.entity.Patient;
import com.medicare.project.repository.PatientRepository;
import com.medicare.project.repository.RoleRepository;
import com.medicare.project.repository.UserRepository;
import com.medicare.project.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired private AuthenticationManager authManager;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        try {
            Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );
            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            String token = jwtUtil.generateToken(userDetails.getUsername());

            User user = userRepository.findByUsername(username).orElseThrow();
            String role = user.getRoles().stream()
                    .map(r -> r.getName().replace("ROLE_", ""))
                    .findFirst().orElse("PATIENT");

            return ResponseEntity.ok(Map.of(
                "token", token,
                "username", username,
                "role", role,
                "fullName", user.getFullName()
            ));
        } catch (BadCredentialsException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid username or password."));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String username  = body.get("username");
        String password  = body.get("password");
        String fullName  = body.get("fullName");
        String email     = body.get("email");
        String phone     = body.get("phone");

        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username already taken."));
        }
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email already registered."));
        }

        Role patientRole = roleRepository.findByName("ROLE_PATIENT")
                .orElseThrow(() -> new RuntimeException("Role not found"));

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setStatus("APPROVED");
        user.setRoles(Set.of(patientRole));
        userRepository.save(user);

        Patient patient = new Patient();
        patient.setName(fullName);
        patient.setEmail(email);
        patient.setPhone(phone);
        patient.setUser(user);
        patientRepository.save(patient);

        return ResponseEntity.ok(Map.of("message", "Registration successful! You can now login."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username is required."));
        }
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "No account found with that username."));
        }
        // Generate a temporary password
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        String tempPassword = "Mc@" + sb.toString();

        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(tempPassword));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
            "message", "Temporary password generated. Please login with this password and change it immediately.",
            "tempPassword", tempPassword,
            "username", username
        ));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated."));
        }
        String currentPassword = body.get("currentPassword");
        String newPassword     = body.get("newPassword");
        String confirmPassword = body.get("confirmPassword");

        if (currentPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "All fields are required."));
        }
        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of("message", "New passwords do not match."));
        }
        if (newPassword.length() < 4) {
            return ResponseEntity.badRequest().body(Map.of("message", "Password must be at least 4 characters."));
        }

        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Current password is incorrect."));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        String role = user.getRoles().stream()
                .map(r -> r.getName().replace("ROLE_", ""))
                .findFirst().orElse("PATIENT");
        return ResponseEntity.ok(Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "fullName", user.getFullName(),
            "role", role
        ));
    }
}
