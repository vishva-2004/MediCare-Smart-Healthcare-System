package com.medicare.project.controller;

import com.medicare.project.entity.*;
import com.medicare.project.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/patient")
public class PatientController {

    @Autowired private PatientRepository patientRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private MedicalRecordRepository medicalRecordRepository;

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Patient patient = patientRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Patient profile not found"));
        return ResponseEntity.ok(toDto(patient));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMyProfile(@RequestBody Map<String, String> body, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Patient patient = patientRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Patient profile not found"));

        if (body.get("phone")      != null) patient.setPhone(body.get("phone"));
        if (body.get("address")    != null) patient.setAddress(body.get("address"));
        if (body.get("bloodGroup") != null) patient.setBloodGroup(body.get("bloodGroup"));
        if (body.get("age")        != null) patient.setAge(Integer.parseInt(body.get("age")));
        if (body.get("gender")     != null) patient.setGender(body.get("gender"));
        patientRepository.save(patient);

        return ResponseEntity.ok(Map.of("message", "Profile updated successfully."));
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        List<Patient> patients = patientRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Patient p : patients) result.add(toDto(p));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Patient p = patientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        return ResponseEntity.ok(toDto(p));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePatient(@PathVariable Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        Long userId = patient.getUser() != null ? patient.getUser().getId() : null;

        // Delete appointments and records first
        List<Appointment> appts = appointmentRepository.findByPatientId(id);
        List<MedicalRecord> records = medicalRecordRepository.findByPatientId(id);
        medicalRecordRepository.deleteAll(records);
        appointmentRepository.deleteAll(appts);
        patientRepository.delete(patient);
        if (userId != null) {
            try { userRepository.deleteById(userId); } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(Map.of("message", "Patient deleted successfully."));
    }

    private Map<String, Object> toDto(Patient p) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("phone", p.getPhone());
        m.put("email", p.getEmail());
        m.put("address", p.getAddress());
        m.put("bloodGroup", p.getBloodGroup());
        m.put("age", p.getAge());
        m.put("gender", p.getGender());
        m.put("registeredDate", p.getRegisteredDate());
        return m;
    }
}
