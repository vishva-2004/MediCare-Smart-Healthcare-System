package com.medicare.project.configuration;

import com.medicare.project.entity.Role;
import com.medicare.project.entity.User;
import com.medicare.project.repository.RoleRepository;
import com.medicare.project.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}") private String adminUsername;
    @Value("${app.admin.password}") private String adminPassword;

    @Override
    public void run(String... args) {
        // Seed roles
        String[] roles = {"ROLE_ADMIN", "ROLE_DOCTOR", "ROLE_PATIENT", "ROLE_STAFF"};
        for (String roleName : roles) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                Role role = new Role();
                role.setName(roleName);
                roleRepository.save(role);
            }
        }

        // Seed default admin
        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setFullName("System Admin");
            admin.setEmail("admin@medicare.com");
            admin.setStatus("APPROVED");
            admin.setRoles(Set.of(adminRole));
            userRepository.save(admin);
            System.out.println("✅ Default admin created: " + adminUsername);
        }
    }
}
