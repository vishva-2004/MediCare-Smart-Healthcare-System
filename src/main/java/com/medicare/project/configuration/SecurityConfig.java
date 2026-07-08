package com.medicare.project.configuration;

import com.medicare.project.security.JwtFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Autowired private JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public API endpoints
                .requestMatchers("/auth/login", "/auth/register", "/auth/forgot-password").permitAll()
                .requestMatchers("/public/**").permitAll()

                // Public page routes (JS handles auth checks)
                .requestMatchers("/login", "/register", "/home", "/", "/forgot-password", "/change-password").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()

                // Protected API routes
                .requestMatchers("/auth/me", "/auth/change-password").authenticated()
                .requestMatchers("/ai/**").authenticated()
                .requestMatchers("/patient/**").authenticated()
                .requestMatchers("/doctor/**").authenticated()
                .requestMatchers("/appointment/**").authenticated()
                .requestMatchers("/medical-record/**").authenticated()
                .requestMatchers("/admin/**").authenticated()
                .requestMatchers("/notification/**").authenticated()

                // Role-based restrictions
                .requestMatchers("/appointment/confirm/**").hasAnyRole("DOCTOR", "ADMIN")
                .requestMatchers("/appointment/complete/**").hasAnyRole("DOCTOR", "ADMIN")

                // Page routes — JWT handled in JavaScript
                .requestMatchers("/dashboard", "/patient-dashboard", "/doctor-dashboard").permitAll()
                .requestMatchers("/doctors", "/appointments", "/patients", "/medical-records").permitAll()
                .requestMatchers("/symptom-checker").permitAll()

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
