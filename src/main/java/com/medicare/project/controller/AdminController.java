package com.medicare.project.controller;

import com.medicare.project.entity.*;
import com.medicare.project.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private MedicalRecordRepository medicalRecordRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDoctors",      doctorRepository.count());
        stats.put("totalPatients",     patientRepository.count());
        stats.put("totalAppointments", appointmentRepository.count());
        stats.put("pendingAppointments", appointmentRepository.findByStatus("PENDING").size());
        stats.put("completedAppointments", appointmentRepository.findByStatus("COMPLETED").size());
        stats.put("totalRecords",      medicalRecordRepository.count());
        stats.put("totalUsers",        userRepository.count());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("fullName", u.getFullName());
            m.put("email", u.getEmail());
            m.put("roles", u.getRoles().stream().map(r -> r.getName().replace("ROLE_", "")).toList());
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reset-password/{id}")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.isBlank())
            return ResponseEntity.badRequest().body(Map.of("message", "New password required."));
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Password reset for " + user.getUsername()));
    }

    @DeleteMapping("/user/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "User deleted."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cannot delete: " + e.getMessage()));
        }
    }

    // Delete patient with cascade
    @DeleteMapping("/patient/{id}")
    public ResponseEntity<?> deletePatient(@PathVariable Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        Long userId = patient.getUser() != null ? patient.getUser().getId() : null;
        List<MedicalRecord> records = medicalRecordRepository.findByPatientId(id);
        medicalRecordRepository.deleteAll(records);
        List<Appointment> appts = appointmentRepository.findByPatientId(id);
        appointmentRepository.deleteAll(appts);
        patientRepository.delete(patient);
        if (userId != null) {
            try { userRepository.deleteById(userId); } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(Map.of("message", "Patient deleted successfully."));
    }

    // Delete doctor with cascade
    @DeleteMapping("/doctor/{id}")
    public ResponseEntity<?> deleteDoctor(@PathVariable Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        Long userId = doctor.getUser() != null ? doctor.getUser().getId() : null;
        List<MedicalRecord> records = medicalRecordRepository.findByDoctorId(id);
        medicalRecordRepository.deleteAll(records);
        List<Appointment> appts = appointmentRepository.findByDoctorId(id);
        appointmentRepository.deleteAll(appts);
        doctorRepository.delete(doctor);
        if (userId != null) {
            try { userRepository.deleteById(userId); } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(Map.of("message", "Doctor deleted successfully."));
    }
}
